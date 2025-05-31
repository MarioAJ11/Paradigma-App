package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.model.Episodio
import com.example.paradigmaapp.repository.contracts.EpisodioRepository // Usar la interfaz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Timber y logs eliminados
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * ViewModel para gestionar mis episodios descargados.
 * Me encargo de persistir la lista de IDs de episodios descargados usando [AppPreferences],
 * obtener los detalles de estos episodios (usando [EpisodioRepository] si no están en caché),
 * y manejar las operaciones de descarga y eliminación de archivos físicos usando el [applicationContext].
 *
 * @author Mario Alguacil Juárez
 */
class DownloadedEpisodioViewModel(
    private val appPreferences: AppPreferences,
    // Ahora dependo de la abstracción del repositorio de episodios.
    private val episodioRepository: EpisodioRepository,
    private val applicationContext: Context // Uso el contexto de la aplicación para operaciones de archivo y evitar fugas.
) : ViewModel() {

    // Lista de IDs de los episodios que han sido descargados.
    private val _downloadedEpisodeIds = MutableStateFlow<List<Int>>(emptyList())
    val downloadedEpisodeIds: StateFlow<List<Int>> = _downloadedEpisodeIds.asStateFlow()

    // Lista de objetos Episodio completos que están descargados, para la UI.
    private val _downloadedEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val downloadedEpisodios: StateFlow<List<Episodio>> = _downloadedEpisodios.asStateFlow()

    // Caché de todos los episodios disponibles, me la pasa MainViewModel.
    private var allAvailableEpisodesCache: List<Episodio> = emptyList()
    // Límite máximo de episodios que permito descargar.
    private val MAX_DOWNLOADS = 10

    init {
        loadDownloadedState() // Cargo el estado de las descargas al iniciar.
    }

    /**
     * Establece la lista de todos los episodios disponibles.
     * Esto me permite construir la lista de `_downloadedEpisodios` de forma más eficiente.
     * @param episodes Lista completa de episodios disponibles.
     */
    fun setAllAvailableEpisodes(episodes: List<Episodio>) {
        allAvailableEpisodesCache = episodes
        // Si la caché de episodios se actualiza, reconstruyo la lista de objetos descargados.
        viewModelScope.launch(Dispatchers.IO) {
            updateDownloadedEpisodiosListFromIds()
        }
    }

    // Carga los IDs de los episodios descargados desde SharedPreferences.
    private fun loadDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadedEpisodeIds.value = appPreferences.loadDownloadedEpisodeIds()
            // Log eliminado: Timber.d("IDs de episodios descargados cargados: ${_downloadedEpisodeIds.value}")
            updateDownloadedEpisodiosListFromIds()
        }
    }

    // Guarda la lista actual de IDs de episodios descargados en SharedPreferences.
    private fun saveDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.saveDownloadedEpisodeIds(_downloadedEpisodeIds.value)
            // Log eliminado: Timber.d("IDs de episodios descargados guardados: ${_downloadedEpisodeIds.value}")
        }
    }

    // Actualiza `_downloadedEpisodios` basándose en `_downloadedEpisodeIds`.
    // Intenta obtener los detalles desde `allAvailableEpisodesCache` primero,
    // y si no, usa `episodioRepository`.
    private suspend fun updateDownloadedEpisodiosListFromIds() {
        val episodeDetailsList = mutableListOf<Episodio>()
        for (id in _downloadedEpisodeIds.value) {
            val cachedEpisodio = allAvailableEpisodesCache.find { it.id == id }
            if (cachedEpisodio != null) {
                episodeDetailsList.add(cachedEpisodio)
            } else {
                try {
                    // Uso la interfaz del repositorio.
                    episodioRepository.getEpisodio(id)?.let { fetchedEpisodio ->
                        episodeDetailsList.add(fetchedEpisodio)
                    } // ?: Timber.w("No se encontró el episodio descargado con ID $id.") // Log eliminado
                } catch (e: Exception) {
                    // Timber.e(e, "Error al obtener detalles del episodio descargado con ID $id.") // Log eliminado
                    // Considerar manejo de error.
                }
            }
        }
        _downloadedEpisodios.value = episodeDetailsList
        // Log eliminado: Timber.d("Lista de episodios descargados actualizada con ${episodeDetailsList.size} items.")
    }

    /**
     * Inicia la descarga de un episodio.
     * Verifica si ya está descargado o si se alcanzó el límite de descargas.
     * Guarda el archivo en el almacenamiento interno de la aplicación.
     * @param episodio El episodio a descargar.
     * @param onMessage Callback para enviar mensajes de estado (éxito/error) a la UI.
     */
    fun downloadEpisodio(episodio: Episodio, onMessage: (String) -> Unit) {
        if (episodio.archiveUrl == null) {
            onMessage("El episodio '${episodio.title}' no tiene URL de descarga.")
            // Log eliminado: Timber.w("Episodio ${episodio.id} ('${episodio.title}') no tiene archiveUrl.")
            return
        }
        if (_downloadedEpisodeIds.value.contains(episodio.id)) {
            onMessage("El episodio '${episodio.title}' ya está descargado.")
            return
        }
        if (_downloadedEpisodeIds.value.size >= MAX_DOWNLOADS) {
            onMessage("Máximo de episodios descargados alcanzado (Límite: $MAX_DOWNLOADS).")
            return
        }

        onMessage("Descargando '${episodio.title}'...")
        viewModelScope.launch(Dispatchers.IO) { // Operación de red y archivo en hilo de IO.
            // Uso un nombre de archivo que incluye el ID para evitar colisiones y facilitar la búsqueda.
            // Sanitizo el slug para evitar caracteres no válidos en nombres de archivo.
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3" // Asumo que son mp3.
            val file = File(applicationContext.filesDir, fileName)

            try {
                val url = URL(episodio.archiveUrl) // archiveUrl debe ser la URL directa al mp3.
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000 // 15 segundos de timeout para conexión.
                connection.readTimeout = 60000    // 60 segundos de timeout para lectura (descargas pueden tardar).
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Error del servidor al descargar: ${connection.responseCode} ${connection.responseMessage}")
                }

                // Guardo el archivo.
                FileOutputStream(file).use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                // Log eliminado: Timber.d("Descarga completada: ${episodio.title} a ${file.absolutePath}")

                // Actualizo la lista de IDs descargados y la persisto.
                _downloadedEpisodeIds.value = _downloadedEpisodeIds.value + episodio.id
                saveDownloadedState()
                // Añado el episodio a la lista de objetos descargados inmediatamente para la UI.
                _downloadedEpisodios.value = (_downloadedEpisodios.value + episodio)
                    .distinctBy { it.id } // Aseguro unicidad.

                withContext(Dispatchers.Main) { // Envío mensaje a la UI en el hilo principal.
                    onMessage("Descarga de '${episodio.title}' completada.")
                }
            } catch (e: Exception) {
                // Timber.e(e, "Error descargando episodio: ${episodio.title}") // Log eliminado
                file.delete() // Elimino el archivo parcial si la descarga falló.
                withContext(Dispatchers.Main) {
                    onMessage("Error al descargar '${episodio.title}'. Revisa la conexión e inténtalo de nuevo.")
                }
            }
        }
    }

    /**
     * Elimina un episodio descargado.
     * Esto incluye borrar el archivo físico del almacenamiento interno y
     * quitar el episodio de la lista de seguimiento de descargas.
     * @param episodio El episodio a eliminar.
     */
    fun deleteDownloadedEpisodio(episodio: Episodio) {
        viewModelScope.launch(Dispatchers.IO) { // Operación de archivo en hilo de IO.
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3"
            val file = File(applicationContext.filesDir, fileName)

            if (file.exists()) {
                if (!file.delete()) {
                    // Log eliminado: Timber.w("No se pudo eliminar el archivo físico: ${file.absolutePath}")
                    // Considerar informar al usuario si la eliminación del archivo falla.
                }
            } else {
                // Log eliminado: Timber.w("El archivo a eliminar no existía (puede ser normal si ya se borró o la ruta cambió): ${file.absolutePath}")
            }

            // Aunque el archivo no exista o falle su borrado, lo quito de la lista de seguimiento.
            if (_downloadedEpisodeIds.value.contains(episodio.id)) {
                _downloadedEpisodeIds.value = _downloadedEpisodeIds.value - episodio.id
                saveDownloadedState()
                _downloadedEpisodios.value = _downloadedEpisodios.value.filterNot { it.id == episodio.id }
                // Log eliminado: Timber.d("Episodio '${episodio.title}' eliminado de la lista de descargas.")
            }
        }
    }

    /**
     * Obtiene la ruta del archivo local para un episodio descargado.
     * Es necesario para que el reproductor pueda acceder al archivo local.
     * @param episodeId El ID del episodio.
     * @return La ruta absoluta al archivo si está descargado y existe, o null en caso contrario.
     */
    fun getDownloadedFilePathByEpisodeId(episodeId: Int): String? {
        // Para construir el nombre del archivo, necesito el slug del episodio.
        // Busco el episodio en la caché de descargados o en la caché global.
        val episodio = _downloadedEpisodios.value.find { it.id == episodeId }
            ?: allAvailableEpisodesCache.find { it.id == episodeId }

        return if (episodio != null) {
            val sanitizedSlug = episodio.slug.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${episodio.id}_$sanitizedSlug.mp3"
            val file = File(applicationContext.filesDir, fileName)
            if (file.exists()) file.absolutePath else null
        } else {
            // Log eliminado: Timber.w("No se pudo encontrar el slug para el episodio ID $episodeId al buscar ruta de archivo.")
            null
        }
    }

    /**
     * Verifica si un episodio específico está actualmente descargado.
     * @param episodeId El ID del episodio a verificar.
     * @return `true` si el episodio está en la lista de descargados, `false` en caso contrario.
     */
    fun isEpisodeDownloaded(episodeId: Int): Boolean {
        return _downloadedEpisodeIds.value.contains(episodeId)
    }
}