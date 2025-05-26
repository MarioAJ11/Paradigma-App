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
    private val applicationContext: Context
) : ViewModel() {

    private val _downloadedEpisodeIds = MutableStateFlow<List<Int>>(emptyList())
    val downloadedEpisodeIds: StateFlow<List<Int>> = _downloadedEpisodeIds.asStateFlow()

    private val _downloadedEpisodios = MutableStateFlow<List<Episodio>>(emptyList())
    val downloadedEpisodios: StateFlow<List<Episodio>> = _downloadedEpisodios.asStateFlow()

    private var allAvailableEpisodesCache: List<Episodio> = emptyList()
    private val MAX_DOWNLOADS = 10 //

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
            Timber.d("Loaded downloaded episode IDs: ${_downloadedEpisodeIds.value}")
            updateDownloadedEpisodiosListFromIds()
        }
    }

    private fun saveDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            appPreferences.saveDownloadedEpisodeIds(_downloadedEpisodeIds.value)
            Timber.d("Saved downloaded episode IDs: ${_downloadedEpisodeIds.value}")
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
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to fetch details for downloaded episode ID $id")
                }
            }
        }
        _downloadedEpisodios.value = episodeDetailsList
        Timber.d("Updated downloaded episodios list with ${episodeDetailsList.size} items.")
    }

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
            val fileName = "${episodio.id}_${episodio.slug}.mp3" // Nombre de archivo más descriptivo
            val file = File(applicationContext.filesDir, fileName)

            try {
                val url = URL(episodio.archiveUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                }

                FileOutputStream(file).use { output ->
                    connection.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
                Timber.d("Descarga completada: ${episodio.title} a ${file.absolutePath}")

                // Añadir a la lista de descargados después de una descarga exitosa
                _downloadedEpisodeIds.value = _downloadedEpisodeIds.value + episodio.id
                saveDownloadedState()
                // Actualizar la lista de objetos Episodio descargados
                _downloadedEpisodios.value = _downloadedEpisodios.value + episodio


                withContext(Dispatchers.Main) {
                    onMessage("Descarga de '${episodio.title}' completada.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error descargando episodio: ${episodio.title}")
                file.delete() // Eliminar archivo parcial si la descarga falló
                withContext(Dispatchers.Main) {
                    onMessage("Error al descargar '${episodio.title}'.")
                }
            }
        }
    }

    fun deleteDownloadedEpisodio(episodio: Episodio) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = "${episodio.id}_${episodio.slug}.mp3"
            val file = File(applicationContext.filesDir, fileName)
            var fileDeleted = false
            if (file.exists()) {
                fileDeleted = file.delete()
                if (fileDeleted) Timber.d("Archivo físico eliminado: ${file.absolutePath}")
                else Timber.w("No se pudo eliminar el archivo físico: ${file.absolutePath}")
            } else {
                Timber.w("El archivo a eliminar no existía: ${file.absolutePath}")
                // Aún así, si el ID está en la lista, lo quitamos.
            }

            // Siempre intentar quitar de la lista de IDs, incluso si el archivo no existía (consistencia)
            if (_downloadedEpisodeIds.value.contains(episodio.id)) {
                _downloadedEpisodeIds.value = _downloadedEpisodeIds.value - episodio.id
                saveDownloadedState()
                _downloadedEpisodios.value = _downloadedEpisodios.value.filterNot { it.id == episodio.id }
                Timber.d("Episodio '${episodio.title}' eliminado de la lista de descargas.")
            }
        }
    }

    fun getDownloadedFilePath(episodio: Episodio): String? {
        val fileName = "${episodio.id}_${episodio.slug}.mp3"
        val file = File(applicationContext.filesDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }
    fun getDownloadedFilePathByEpisodeId(episodeId: Int): String? {
        // Necesitamos encontrar el slug para construir el nombre de archivo.
        // Esto es una limitación si no tenemos todos los episodios cacheados.
        // Idealmente, el nombre del archivo no dependería del slug o el slug se guardaría.
        // Por ahora, intentamos encontrarlo en la caché:
        val episodio = _downloadedEpisodios.value.find { it.id == episodeId }
            ?: allAvailableEpisodesCache.find {it.id == episodeId}

        return if (episodio != null) {
            val fileName = "${episodio.id}_${episodio.slug}.mp3"
            val file = File(applicationContext.filesDir, fileName)
            if (file.exists()) file.absolutePath else null
        } else {
            // Si no encontramos el episodio, no podemos determinar el nombre del archivo.
            // Podrías intentar listar archivos y buscar por ID si el slug no es crucial.
            Timber.w("No se pudo encontrar el episodio con ID $episodeId para obtener path de descarga.")
            null
        }
    }

    fun isEpisodeDownloaded(episodeId: Int): Boolean {
        return _downloadedEpisodeIds.value.contains(episodeId)
    }
}