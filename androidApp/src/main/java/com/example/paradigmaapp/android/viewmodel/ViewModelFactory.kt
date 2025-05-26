package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.repository.WordpressService // Asegúrate que el import es correcto

/**
 * Factory para crear instancias de ViewModels, proveyendo las dependencias necesarias.
 *
 * @property appPreferences Instancia de AppPreferences para el acceso a SharedPreferences.
 * @property wordpressService Instancia de WordpressService para el acceso a la API.
 * @property applicationContext Contexto de la aplicación.
 * @author Mario Alguacil Juárez
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appPreferences: AppPreferences,
    private val wordpressService: WordpressService,
    private val applicationContext: Context
) : ViewModelProvider.Factory {

    // Instancias de ViewModels que pueden ser compartidas o pasadas a MainViewModel
    // Se crean aquí para asegurar que MainViewModel reciba las mismas instancias si es necesario.
    // Si no hay interdependencia directa estricta, MainViewModel podría no necesitarlas en su constructor.
    private val queueViewModelInstance by lazy {
        QueueViewModel(appPreferences, wordpressService)
    }
    private val onGoingEpisodioViewModelInstance by lazy {
        OnGoingEpisodioViewModel(appPreferences, wordpressService)
    }
    private val downloadedEpisodioViewModelInstance by lazy {
        DownloadedEpisodioViewModel(appPreferences, wordpressService, applicationContext)
    }


    override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(wordpressService) as T
            }
            modelClass.isAssignableFrom(QueueViewModel::class.java) -> {
                // Devolver la instancia ya creada si es un singleton dentro del Factory
                queueViewModelInstance as T
            }
            modelClass.isAssignableFrom(DownloadedEpisodioViewModel::class.java) -> {
                downloadedEpisodioViewModelInstance as T
            }
            modelClass.isAssignableFrom(OnGoingEpisodioViewModel::class.java) -> {
                onGoingEpisodioViewModelInstance as T
            }
            else -> throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
        }
    }
}