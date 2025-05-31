package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.repository.WordpressService

/**
 * Factory para crear instancias de ViewModels, proveyendo las dependencias necesarias.
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appPreferences: AppPreferences,
    private val wordpressService: WordpressService,
    private val applicationContext: Context
) : ViewModelProvider.Factory {

    private val queueViewModelInstance: QueueViewModel by lazy {
        QueueViewModel(appPreferences, wordpressService)
    }
    private val onGoingEpisodioViewModelInstance: OnGoingEpisodioViewModel by lazy {
        OnGoingEpisodioViewModel(appPreferences, wordpressService)
    }
    private val downloadedEpisodioViewModelInstance: DownloadedEpisodioViewModel by lazy {
        DownloadedEpisodioViewModel(appPreferences, wordpressService, applicationContext)
    }
    private val searchViewModelInstance: SearchViewModel by lazy {
        SearchViewModel(wordpressService)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(
                    wordpressService = wordpressService,
                    appPreferences = appPreferences,
                    context = applicationContext,
                    queueViewModel = queueViewModelInstance,
                    onGoingViewModel = onGoingEpisodioViewModelInstance,
                    downloadedViewModel = downloadedEpisodioViewModelInstance
                ) as T
            }
            modelClass.isAssignableFrom(ProgramaViewModel::class.java) -> {
                val savedStateHandle = extras.createSavedStateHandle()
                ProgramaViewModel(wordpressService, savedStateHandle) as T
            }
            // Asegúrate de que esta entrada para EpisodeDetailViewModel está presente y es correcta:
            modelClass.isAssignableFrom(EpisodeDetailViewModel::class.java) -> {
                val savedStateHandle = extras.createSavedStateHandle()
                EpisodeDetailViewModel(wordpressService, savedStateHandle) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> searchViewModelInstance as T
            modelClass.isAssignableFrom(QueueViewModel::class.java) -> queueViewModelInstance as T
            modelClass.isAssignableFrom(DownloadedEpisodioViewModel::class.java) -> downloadedEpisodioViewModelInstance as T
            modelClass.isAssignableFrom(OnGoingEpisodioViewModel::class.java) -> onGoingEpisodioViewModelInstance as T

            else -> throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}") // Esta es la línea 60 que causa el error
        }
    }
}