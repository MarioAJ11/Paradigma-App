package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.WordpressService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * ViewModel para gestionar los episodios descargados.
 *
 * @property appPreferences Para persistir la lista de IDs de episodios descargados.
 * @property wordpressService Para obtener detalles de los episodios si es necesario.
 * @property applicationContext Contexto de la aplicación para operaciones de archivo.
 * @author Mario Alguacil Juárez
 */
class DownloadedEpisodioViewModel(
    private val appPreferences: AppPreferences,
    private val wordpressService: WordpressService,
    private val applicationContext: Context // Usar applicationContext para evitar fugas
) : ViewModel() {

    private val _downloadedEpisodeIds = MutableStateFlow<List<Int>>(emptyList())
    val downloadedEpisodeIds: StateFlow<List<Int>> = _downloadedEpisodeIds.asStateFlow()

    private val _downloadedEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val downloadedEpisodios: StateFlow<List<Episodio>> = _downloadedEpisodios.asStateFlow()

    private var allAvailableEpisodesCache: List<Episodio> = emptyList()
    private val MAX_DOWNLOADS = 10 // Límite de descargas

    init {
        loadDownloadedState()
    }

    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        viewModelScope.launch(Dispatchers.IO) {
            updateDownloadedEpisodiosListFromIds()
        }
    }

    private fun loadDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadedEpisodeIds.value = appPreferences.loadDownloadedEpisodeIds()
            Timber.d("IDs de episodios descargados cargados: ${_downloadedEpisodeIds.value}")
            updateDownloadedEpisodiosListFromIds()
        }
    }

    private fun saveDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.saveDownloadedEpisodeIds(_downloadedEpisodeIds.value)
            Timber.d("IDs de episodios descargados guardados: ${_downloadedEpisodeIds.value}")
        }
    }

    private suspend fun updateDownloadedEpisodiosListFromIds() {
        val episodeDetailsList = mutableListOf<Episodio>()
        for (id in _downloadedEpisodeIds.value) {
            val cachedEpisodio = allAvailableEpisodesCache.find { it.id == id }
            if (cachedEpisodio != null) {
                episodeDetailsList.add(cachedEpisodio)
            } else {
                try {
                    wordpressService.getEpisodio(id)?.let { fetchedEpisodio ->
                        episodeDetailsList.add(fetchedEpisodio)
                    } ?: Timber.w("No se encontró el episodio descargado con ID $id.")
                } catch (e: Exception) {
                    Timber.e(e, "Error al obtener detalles del episodio descargado con ID $id.")
                }
            }
        }
        _downloadedEpisodios.value = episodeDetailsList
        Timber.d("Lista de episodios descargados actualizada con ${episodeDetailsList.size} items.")
    }

    /**
     * Inicia la descarga de un episodio.
     * @param episodio El episodio a descargar.
     * @param onMessage Callback para enviar mensajes de estado a la UI.
     */
    fun downloadEpisodio(episodio: Episodio, onMessage: (String) -> Unit) {
        if (episodio.archiveUrl == null) {
            onMessage("El episodio '${episodio.title}' no tiene URL de descarga.")
            Timber.w("Episodio ${episodio.id} ('${episodio.title}') no tiene archiveUrl.")
            return
        }
        if (_downloadedEpisodeIds.value.contains(episodio.id)) {
            onMessage("El episodio '${episodio.title}' ya está descargado.")
            return
        }
        if (_downloadedEpisodeIds.value.size >= MAX_DOWNLOADS) {
            onMessage("Máximo de episodios descargados alcanzado (Max: $MAX_DOWNLOADS).")
            return
        }

        onMessage("Descargando '${episodio.title}'...")
        viewModelScope.launch(Dispatchers.IO) {
            // Usar un nombre de archivo que incluya el ID para evitar colisiones y facilitar la búsqueda.
            // El slug puede tener caracteres no válidos para nombres de archivo, así que lo sanitizamos.
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3"
            val file = File(applicationContext.filesDir, fileName)

            try {
                val url = URL(episodio.archiveUrl) // archiveUrl debe ser la URL directa al mp3
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000 // 15 segundos timeout conexión
                connection.readTimeout = 30000    // 30 segundos timeout lectura
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Error del servidor: ${connection.responseCode} ${connection.responseMessage}")
                }

                FileOutputStream(file).use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                Timber.d("Descarga completada: ${episodio.title} a ${file.absolutePath}")

                _downloadedEpisodeIds.value = _downloadedEpisodeIds.value + episodio.id
                saveDownloadedState()
                // Añadir el episodio a la lista de objetos descargados inmediatamente
                _downloadedEpisodios.value = (_downloadedEpisodios.value + episodio)
                    .distinctBy { it.id } // Asegurar unicidad

                withContext(Dispatchers.Main) {
                    onMessage("Descarga de '${episodio.title}' completada.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error descargando episodio: ${episodio.title}")
                file.delete() // Eliminar archivo parcial si la descarga falló
                withContext(Dispatchers.Main) {
                    onMessage("Error al descargar '${episodio.title}'. Revisa la conexión.")
                }
            }
        }
    }

    /**
     * Elimina un episodio descargado (el archivo físico y de la lista de seguimiento).
     * @param episodio El episodio a eliminar.
     */
    fun deleteDownloadedEpisodio(episodio: Episodio) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3"
            val file = File(applicationContext.filesDir, fileName)
            var fileWasDeleted = false
            if (file.exists()) {
                if (file.delete()) {
                    Timber.d("Archivo físico eliminado: ${file.absolutePath}")
                    fileWasDeleted = true
                } else {
                    Timber.w("No se pudo eliminar el archivo físico: ${file.absolutePath}")
                }
            } else {
                Timber.w("El archivo a eliminar no existía: ${file.absolutePath}")
                fileWasDeleted = true
            }

            if (_downloadedEpisodeIds.value.contains(episodio.id)) {
                _downloadedEpisodeIds.value = _downloadedEpisodeIds.value - episodio.id
                saveDownloadedState()
                _downloadedEpisodios.value = _downloadedEpisodios.value.filterNot { it.id == episodio.id }
                Timber.d("Episodio '${episodio.title}' eliminado de la lista de descargas.")
            }
        }
    }

    /**
     * Obtiene la ruta del archivo local para un episodio descargado.
     * @param episodeId El ID del episodio.
     * @return La ruta absoluta al archivo si está descargado y existe, o null en caso contrario.
     */
    fun getDownloadedFilePathByEpisodeId(episodeId: Int): String? {
        // Para construir el nombre del archivo, necesitamos el slug.
        // Buscamos el episodio en la caché o en la lista de descargados.
        val episodio = _downloadedEpisodios.value.find { it.id == episodeId }
            ?: allAvailableEpisodesCache.find { it.id == episodeId }

        return if (episodio != null) {
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3"
            val file = File(applicationContext.filesDir, fileName)
            if (file.exists()) file.absolutePath else null
        } else {
            Timber.w("No se pudo encontrar el slug para el episodio ID $episodeId al buscar ruta de archivo.")
            null
        }
    }

    fun isEpisodeDownloaded(episodeId: Int): Boolean {
        return _downloadedEpisodeIds.value.contains(episodeId)
    }
}