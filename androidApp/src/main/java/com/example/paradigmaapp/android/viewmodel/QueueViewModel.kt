package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paradigmaapp.android.loadPodcastQueue
import com.example.paradigmaapp.android.podcast.Podcast
import com.example.paradigmaapp.android.savePodcastQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class QueueViewModel(private val context: Context) : ViewModel() {

    // La cola se almacena como una lista de URLs de podcast
    private val _queuePodcastUrls = MutableStateFlow<List<String>>(emptyList())
    val queuePodcastUrls: StateFlow<List<String>> = _queuePodcastUrls.asStateFlow()

    // Lista de todos los podcasts disponibles (para mapear URLs a objetos Podcast)
    private val _allPodcasts = mutableListOf<Podcast>()

    init {
        loadQueueState()
    }

    fun setAllPodcasts(podcasts: List<Podcast>) {
        _allPodcasts.clear()
        _allPodcasts.addAll(podcasts)
        // Después de cargar todos los podcasts, actualiza la lista de objetos de la cola
        updateQueuePodcastsList()
    }

    private fun loadQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            _queuePodcastUrls.value = loadPodcastQueue(context)
            updateQueuePodcastsList()
            Timber.Forest.d("Loaded podcast queue URLs: ${_queuePodcastUrls.value}")
        }
    }

    private fun saveQueueState() {
        viewModelScope.launch(Dispatchers.IO) {
            savePodcastQueue(context, _queuePodcastUrls.value)
            Timber.Forest.d("Saved podcast queue URLs: ${_queuePodcastUrls.value}")
        }
    }

    // Mapea las URLs de la cola a objetos Podcast
    private val _queuePodcasts = MutableStateFlow<List<Podcast>>(emptyList())
    val queuePodcasts: StateFlow<List<Podcast>> = _queuePodcasts.asStateFlow()

    private fun updateQueuePodcastsList() {
        val currentQueue = _queuePodcastUrls.value.mapNotNull { url ->
            _allPodcasts.find { it.url == url }
        }
        _queuePodcasts.value = currentQueue
        Timber.Forest.d("Updated queue podcasts list: ${_queuePodcasts.value.size}")
    }

    fun addPodcastToQueue(podcast: Podcast) {
        if (!_queuePodcastUrls.value.contains(podcast.url)) {
            _queuePodcastUrls.value = _queuePodcastUrls.value + podcast.url
            saveQueueState()
            updateQueuePodcastsList()
            Timber.Forest.d("Added '${podcast.title}' to queue. Current queue size: ${_queuePodcastUrls.value.size}")
        } else {
            Timber.Forest.d("Podcast '${podcast.title}' is already in queue.")
        }
    }

    fun removePodcastFromQueue(podcast: Podcast) {
        if (_queuePodcastUrls.value.contains(podcast.url)) {
            _queuePodcastUrls.value = _queuePodcastUrls.value - podcast.url
            saveQueueState()
            updateQueuePodcastsList()
            Timber.Forest.d("Removed '${podcast.title}' from queue. Current queue size: ${_queuePodcastUrls.value.size}")
        }
    }

    /**
     * Elimina el podcast actual de la cabeza de la cola y devuelve el siguiente podcast.
     * @param playedPodcastUrl La URL del podcast que acaba de terminar.
     * @return El siguiente podcast en la cola, o null si la cola está vacía.
     */
    fun dequeuePodcast(playedPodcastUrl: String): Podcast? {
        val currentQueue = _queuePodcastUrls.value.toMutableList()
        if (currentQueue.isNotEmpty() && currentQueue.first() == playedPodcastUrl) {
            currentQueue.removeAt(0) // Elimina el podcast que acaba de terminar
        } else {
            // Si el podcast que terminó no era el primero en la cola (ej. reproducción manual o salto),
            // lo eliminamos si está en la cola, pero no avanzamos la cola automáticamente a menos que sea el primero.
            currentQueue.remove(playedPodcastUrl)
        }
        _queuePodcastUrls.value = currentQueue
        saveQueueState()
        updateQueuePodcastsList()
        Timber.Forest.d("Dequeued podcast. Current queue size: ${_queuePodcastUrls.value.size}")

        return if (_queuePodcastUrls.value.isNotEmpty()) {
            _allPodcasts.find { it.url == _queuePodcastUrls.value.first() }
        } else {
            null
        }
    }
}