package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.config.RemoteConfigService
import com.example.paradigmaapp.repository.ParadigmaRepository

/**
 * Factoría personalizada para la creación de ViewModels.
 * Inyecta todas las dependencias necesarias a cada ViewModel.
 *
 * @param appPreferences Instancia para la gestión de preferencias.
 * @param wordpressDataSource Instancia del repositorio principal.
 * @param applicationContext El Context de la aplicación.
 * @param remoteConfigService El servicio para obtener URLs de forma remota.
 * @param andainaStream El servicio para gestionar la radio en directo.
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appPreferences: AppPreferences,
    private val wordpressDataSource: ParadigmaRepository,
    private val applicationContext: Context,
    private val remoteConfigService: RemoteConfigService,
    private val andainaStream: AndainaStream
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()

        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                // Creamos los ViewModels de los que depende MainViewModel.
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
                    downloadedViewModel = downloadedVM,
                    andainaStreamPlayer = andainaStream
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(appPreferences, remoteConfigService) as T
            }
            // El resto de ViewModels se mantienen igual.
            modelClass.isAssignableFrom(ProgramaViewModel::class.java) -> {
                ProgramaViewModel(wordpressDataSource, wordpressDataSource, savedStateHandle) as T
            }
            modelClass.isAssignableFrom(EpisodeDetailViewModel::class.java) -> {
                EpisodeDetailViewModel(wordpressDataSource, savedStateHandle) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(wordpressDataSource) as T
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
            else -> throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
        }
    }
}