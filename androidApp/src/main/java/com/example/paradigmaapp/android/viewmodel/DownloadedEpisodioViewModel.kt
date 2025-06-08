package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episodio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.remove

/**
 * ViewModel responsable de gestionar la lógica y el estado de los episodios descargados.
 * Es autosuficiente para el modo offline, ya que lee y escribe la lista completa de
 * objetos [Episodio] descargados desde y hacia [AppPreferences].
 *
 * @property appPreferences Instancia de [AppPreferences] para acceder a las preferencias guardadas.
 * @property applicationContext El [Context] de la aplicación, necesario para operaciones de sistema de archivos.
 *
 * @author Mario Alguacil Juárez
 */
class DownloadedEpisodioViewModel(
    private val appPreferences: AppPreferences,
    private val applicationContext: Context
) : ViewModel() {

    // Este StateFlow contiene la lista de objetos Episodio completos, leída desde la persistencia local.
    private val _downloadedEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val downloadedEpisodios: StateFlow<List<Episodio>> = _downloadedEpisodios.asStateFlow()
    private val downloadsInProgress = mutableSetOf<Int>()

    private val MAX_DOWNLOADS = 10

    init {
        loadDownloadedState()
    }

    private fun loadDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            val episodiosGuardados = appPreferences.loadDownloadedEpisodios()
            withContext(Dispatchers.Main) {
                _downloadedEpisodios.value = episodiosGuardados
            }
        }
    }

    /**
     * Inicia la descarga de un episodio, evitando duplicados y descargas concurrentes del mismo item.
     *
     * @param episodio El [Episodio] a descargar.
     * @param onMessage Callback para notificar a la UI sobre el resultado.
     */
    fun downloadEpisodio(episodio: Episodio, onMessage: (String) -> Unit) {
        if (episodio.archiveUrl == null) {
            onMessage("El episodio '${episodio.title}' no tiene URL de descarga.")
            return
        }
        if (isEpisodeDownloaded(episodio.id)) {
            onMessage("El episodio '${episodio.title}' ya está descargado.")
            return
        }
        // Comprobamos si el episodio ya está en la cola de descargas en progreso.
        if (downloadsInProgress.contains(episodio.id)) {
            onMessage("La descarga de '${episodio.title}' ya está en curso.")
            return
        }
        if (_downloadedEpisodios.value.size >= MAX_DOWNLOADS) {
            onMessage("Máximo de $MAX_DOWNLOADS episodios descargados alcanzado.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val file = File(applicationContext.filesDir, createFileName(episodio))
            try {
                // Añadimos el ID al set de descargas en progreso.
                downloadsInProgress.add(episodio.id)

                val url = URL(episodio.archiveUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Error del servidor: ${connection.responseCode}")
                }

                FileOutputStream(file).use { output -> connection.inputStream.use { it.copyTo(output) } }

                val listaActualizada = _downloadedEpisodios.value + episodio
                appPreferences.saveDownloadedEpisodios(listaActualizada)

                withContext(Dispatchers.Main) {
                    _downloadedEpisodios.value = listaActualizada
                    onMessage("Descarga de '${episodio.title}' completada.")
                }
            } catch (e: Exception) {
                file.delete()
                withContext(Dispatchers.Main) {
                    onMessage("Error al descargar '${episodio.title}'.")
                }
            } finally {
                // Es crucial quitar el ID del set cuando la tarea termina (sea con éxito o error).
                downloadsInProgress.remove(episodio.id)
            }
        }
    }

    /**
     * Elimina un episodio descargado, borrando el archivo físico y
     * actualizando la lista persistida en AppPreferences.
     *
     * @param episodio El [Episodio] a eliminar.
     */
    fun deleteDownloadedEpisodio(episodio: Episodio) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(applicationContext.filesDir, createFileName(episodio))
            if (file.exists()) {
                file.delete()
            }

            val listaActualizada = _downloadedEpisodios.value.filterNot { it.id == episodio.id }
            appPreferences.saveDownloadedEpisodios(listaActualizada)
            withContext(Dispatchers.Main) {
                _downloadedEpisodios.value = listaActualizada
            }
        }
    }

    /** Verifica si un episodio está descargado.
     *
     * @param episodeId El ID del episodio a verificar.
     */
    fun isEpisodeDownloaded(episodeId: Int): Boolean {
        return _downloadedEpisodios.value.any { it.id == episodeId }
    }

    /**
     * Obtiene la ruta del archivo local para un episodio descargado.
     * Busca el episodio en la lista interna de episodios descargados.
     *
     * @param episodeId El ID del episodio a buscar.
     */
    fun getDownloadedFilePathByEpisodeId(episodeId: Int): String? {
        val episodio = _downloadedEpisodios.value.find { it.id == episodeId }
        return episodio?.let {
            val file = File(applicationContext.filesDir, createFileName(it))
            if (file.exists()) file.absolutePath else null
        }
    }

    /** Crea un nombre de archivo único y sanitizado para un episodio.
     *
     * @param episodio El [Episodio] para el cual se creará el nombre de archivo.
     */
    private fun createFileName(episodio: Episodio): String {
        val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        return "${episodio.id}_$sanitizedSlug.mp3"
    }
}