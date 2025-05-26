package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.repository.WordpressService

/**
 * Factory para crear instancias de ViewModels, proveyendo las dependencias necesarias.
 * Esta factory ahora puede manejar ViewModels que requieren SavedStateHandle.
 *
 * @property appPreferences Instancia de AppPreferences.
 * @property wordpressService Instancia de WordpressService.
 * @property applicationContext Contexto de la aplicación.
 * @property owner El SavedStateRegistryOwner (usualmente la Activity o Fragment/NavBackStackEntry)
 * necesario para ViewModels que usan SavedStateHandle.
 * @property defaultArgs Argumentos por defecto para el SavedStateHandle.
 * @author Mario Alguacil Juárez
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appPreferences: AppPreferences,
    private val wordpressService: WordpressService,
    private val applicationContext: Context,
    private val owner: SavedStateRegistryOwner, // Para SavedStateHandle
    private val defaultArgs: android.os.Bundle? = null // Argumentos por defecto
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    // Instancias de ViewModels que no usan SavedStateHandle directamente en su constructor primario
    // o que queremos que sean "singleton" dentro del scope de esta factory (si se llama múltiples veces).
    // Sin embargo, con AbstractSavedStateViewModelFactory, es más limpio crear cada uno bajo demanda.

    // Lazy ainstantiation para ViewModels que no toman SavedStateHandle en el constructor primario
    // que esta factory maneja directamente.
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

    override fun <T : ViewModel> create(
        key: String, // Clave para identificar el ViewModel
        modelClass: Class<T>,
        handle: SavedStateHandle // El SavedStateHandle es provisto por la superclase
    ): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                // MainViewModel necesita acceso a los otros ViewModels para coordinación
                MainViewModel(
                    wordpressService = wordpressService,
                    appPreferences = appPreferences,
                    context = applicationContext,
                    queueViewModel = queueViewModelInstance, // Usar instancias ya creadas si es necesario
                    onGoingViewModel = onGoingEpisodioViewModelInstance,
                    downloadedViewModel = downloadedEpisodioViewModelInstance
                    // SavedStateHandle no se pasa directamente aquí, MainViewModel no lo necesita en constructor
                ) as T
            }
            modelClass.isAssignableFrom(ProgramaViewModel::class.java) -> {
                // ProgramaViewModel SÍ usa SavedStateHandle
                ProgramaViewModel(wordpressService, handle) as T
            }
            // Para ViewModels que no usan SavedStateHandle en su constructor principal
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> searchViewModelInstance as T
            modelClass.isAssignableFrom(QueueViewModel::class.java) -> queueViewModelInstance as T
            modelClass.isAssignableFrom(DownloadedEpisodioViewModel::class.java) -> downloadedEpisodioViewModelInstance as T
            modelClass.isAssignableFrom(OnGoingEpisodioViewModel::class.java) -> onGoingEpisodioViewModelInstance as T

            else -> throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
        }
    }
}