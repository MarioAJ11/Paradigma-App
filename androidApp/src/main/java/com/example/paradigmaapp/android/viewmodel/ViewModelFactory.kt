package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.repository.ParadigmaRepository

/**
 * Factoría personalizada para la creación de instancias de [ViewModel].
 * Permite inyectar dependencias necesarias (como repositorios, [AppPreferences], [Context])
 * a cada ViewModel en el momento de su creación.
 *
 * @property appPreferences Instancia de [AppPreferences] para la gestión de preferencias.
 * @property wordpressDataSource Instancia de [ParadigmaRepository], que implementa los repositorios.
 * @property applicationContext El [Context] de la aplicación.
 *
 * @author Mario Alguacil Juárez
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appPreferences: AppPreferences,
    private val wordpressDataSource: ParadigmaRepository,
    private val applicationContext: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()

        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                val queueVM = QueueViewModel(appPreferences, wordpressDataSource)
                val onGoingVM = OnGoingEpisodioViewModel(appPreferences)
                val downloadedVM = DownloadedEpisodioViewModel(appPreferences, applicationContext)
                MainViewModel(
                    programaRepository = wordpressDataSource,
                    episodioRepository = wordpressDataSource,
                    appPreferences = appPreferences,
                    context = applicationContext,
                    queueViewModel = queueVM,
                    onGoingViewModel = onGoingVM,
                    downloadedViewModel = downloadedVM
                ) as T
            }
            modelClass.isAssignableFrom(ProgramaViewModel::class.java) -> {
                ProgramaViewModel(
                    programaRepository = wordpressDataSource,
                    episodioRepository = wordpressDataSource,
                    savedStateHandle = savedStateHandle
                ) as T
            }
            modelClass.isAssignableFrom(EpisodeDetailViewModel::class.java) -> {
                EpisodeDetailViewModel(
                    episodioRepository = wordpressDataSource,
                    savedStateHandle = savedStateHandle
                ) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(episodioRepository = wordpressDataSource) as T
            }
            modelClass.isAssignableFrom(QueueViewModel::class.java) -> {
                QueueViewModel(appPreferences, wordpressDataSource) as T
            }
            modelClass.isAssignableFrom(DownloadedEpisodioViewModel::class.java) -> {
                DownloadedEpisodioViewModel(appPreferences, applicationContext) as T
            }
            modelClass.isAssignableFrom(OnGoingEpisodioViewModel::class.java) -> {
                OnGoingEpisodioViewModel(appPreferences) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(appPreferences) as T
            }
            else -> throw IllegalArgumentException("Clase ViewModel desconocida en ViewModelFactory: ${modelClass.name}")
        }
    }
}