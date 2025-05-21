package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.api.ArchiveService
import com.example.paradigmaapp.android.loadDownloadedPodcastIdentifiers
import com.example.paradigmaapp.android.podcast.Podcast
import com.example.paradigmaapp.android.saveDownloadedPodcastIdentifiers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadedPodcastViewModel(
    private val context: Context,
    private val archiveService: ArchiveService
) : ViewModel() {

    // Identificadores de los podcasts que están "descargados" (sus URLs están guardadas)
    private val _downloadedPodcastIdentifiers = MutableStateFlow<List<String>>(emptyList())
    val downloadedPodcastIdentifiers: StateFlow<List<String>> = _downloadedPodcastIdentifiers.asStateFlow()

    // Lista de objetos Podcast descargados para el UI (obtenidos de _allPodcasts)
    private val _downloadedPodcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val downloadedPodcasts: StateFlow<List<Podcast>> = _downloadedPodcasts.asStateFlow()

    private val _allPodcasts = mutableListOf<Podcast>() // Copia local de todos los podcasts disponibles
    private val MAX_DOWNLOADS = 10 // Máximo de podcasts permitidos para descargar

    init {
        loadDownloadedState()
    }

    fun setAllPodcasts(podcasts: List<Podcast>) {
        _allPodcasts.clear()
        _allPodcasts.addAll(podcasts)
        updateDownloadedPodcastsList() // Actualizar la lista visible de descargados
    }

    private fun loadDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            _downloadedPodcastIdentifiers.value = loadDownloadedPodcastIdentifiers(context)
            updateDownloadedPodcastsList()
            Timber.d("Loaded downloaded podcast identifiers: ${_downloadedPodcastIdentifiers.value}")
        }
    }

    private fun saveDownloadedState() {
        viewModelScope.launch(Dispatchers.IO) {
            saveDownloadedPodcastIdentifiers(context, _downloadedPodcastIdentifiers.value)
            Timber.d("Saved downloaded podcast identifiers: ${_downloadedPodcastIdentifiers.value}")
        }
    }

    private fun updateDownloadedPodcastsList() {
        val currentDownloaded = _allPodcasts.filter { podcast ->
            _downloadedPodcastIdentifiers.value.contains(podcast.identifier)
        }
        _downloadedPodcasts.value = currentDownloaded
        Timber.d("Updated downloaded podcasts list: ${_downloadedPodcasts.value.size}")
    }

    // Funcionalidad de "descarga" (simulada)
    fun downloadPodcast(podcast: Podcast, onMessage: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_downloadedPodcastIdentifiers.value.size >= MAX_DOWNLOADS) {
                onMessage("Máximo de podcast descargados (Max: $MAX_DOWNLOADS)")
                return@launch
            }
            if (_downloadedPodcastIdentifiers.value.contains(podcast.identifier)) {
                onMessage("El podcast '${podcast.title}' ya está descargado.")
                return@launch
            }

            // Simular la descarga: agregar el identificador a la lista de "descargados"
            _downloadedPodcastIdentifiers.value = _downloadedPodcastIdentifiers.value + podcast.identifier
            saveDownloadedState()
            updateDownloadedPodcastsList()
            onMessage("Descargando '${podcast.title}'...") // Mensaje de "descargando"
            // Lógica de descarga usando DownloadManager o Ktor para descargar el archivo de audio
             val file = File(context.filesDir, "${podcast.identifier}.mp3")
             try {
                 URL(podcast.url).openStream().use { input ->
                     FileOutputStream(file).use { output ->
                         input.copyTo(output)
                     }
                 }
                 Timber.d("Downloaded actual file to ${file.absolutePath}")
                 onMessage("Descarga de '${podcast.title}' completada.")
             } catch (e: Exception) {
                 Timber.e(e, "Error downloading actual file: ${podcast.title}")
                 onMessage("Error al descargar '${podcast.title}'.")
                 // Revertir el estado de "descargado" si la descarga real falla
                 _downloadedPodcastIdentifiers.value = _downloadedPodcastIdentifiers.value - podcast.identifier
                 saveDownloadedState()
                 updateDownloadedPodcastsList()
             }
            onMessage("Descarga de '${podcast.title}' completada (simulada).")
        }
    }

    fun deleteDownloadedPodcast(podcast: Podcast) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_downloadedPodcastIdentifiers.value.contains(podcast.identifier)) {
                _downloadedPodcastIdentifiers.value = _downloadedPodcastIdentifiers.value - podcast.identifier
                saveDownloadedState()
                updateDownloadedPodcastsList()
                // lógica para ELIMINAR el archivo físico
                 val file = File(context.filesDir, "${podcast.identifier}.mp3")
                 if (file.exists()) {
                     file.delete()
                     Timber.d("Deleted actual file: ${file.absolutePath}")
                 }
                Timber.d("Podcast '${podcast.title}' eliminado de descargas.")
            }
        }
    }

    // Función para obtener la ruta del archivo descargado
     fun getDownloadedFilePath(podcast: Podcast): String? {
         val file = File(context.filesDir, "${podcast.identifier}.mp3")
         return if (file.exists()) file.absolutePath else null
     }
}