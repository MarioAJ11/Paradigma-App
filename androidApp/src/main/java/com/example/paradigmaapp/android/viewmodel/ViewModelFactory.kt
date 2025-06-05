package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.repository.WordpressService // Implementación concreta de los repositorios
import com.example.paradigmaapp.repository.contracts.EpisodioRepository
import com.example.paradigmaapp.repository.contracts.ProgramaRepository

/**
 * Factoría personalizada para la creación de instancias de [ViewModel].
 * Permite inyectar dependencias necesarias (como repositorios, [AppPreferences], [Context])
 * a cada ViewModel en el momento de su creación.
 *
 * Utiliza [CreationExtras] para obtener el [androidx.lifecycle.SavedStateHandle] si un ViewModel lo requiere,
 * permitiendo que los ViewModels puedan guardar y restaurar su estado a través de los
 * argumentos de navegación y el estado del proceso.
 *
 * @property appPreferences Instancia de [AppPreferences] para la gestión de preferencias.
 * @property wordpressDataSource Instancia de [WordpressService], que implementa
 * [ProgramaRepository] y [EpisodioRepository]. Se pasa a los ViewModels que dependen de estas interfaces.
 * @property applicationContext El [Context] de la aplicación.
 *
 * @author Mario Alguacil Juárez
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appPreferences: AppPreferences,
    private val wordpressDataSource: WordpressService, // Es la implementación de los repositorios
    private val applicationContext: Context
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle() // Obtener SavedStateHandle para ViewModels que lo necesiten

        // El `wordpressDataSource` (que es un `WordpressService`) se pasa donde se esperan
        // las interfaces `ProgramaRepository` y/o `EpisodioRepository`.
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                val queueVM = QueueViewModel(appPreferences, wordpressDataSource)
                val onGoingVM = OnGoingEpisodioViewModel(appPreferences, wordpressDataSource)
                val downloadedVM = DownloadedEpisodioViewModel(appPreferences, wordpressDataSource, applicationContext)
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
                    savedStateHandle = savedStateHandle // Pasar SavedStateHandle
                ) as T
            }
            modelClass.isAssignableFrom(EpisodeDetailViewModel::class.java) -> {
                EpisodeDetailViewModel(
                    episodioRepository = wordpressDataSource,
                    savedStateHandle = savedStateHandle // Pasar SavedStateHandle
                ) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(episodioRepository = wordpressDataSource) as T
            }
            modelClass.isAssignableFrom(QueueViewModel::class.java) -> {
                QueueViewModel(appPreferences, wordpressDataSource) as T
            }
            modelClass.isAssignableFrom(DownloadedEpisodioViewModel::class.java) -> {
                DownloadedEpisodioViewModel(appPreferences, wordpressDataSource, applicationContext) as T
            }
            modelClass.isAssignableFrom(OnGoingEpisodioViewModel::class.java) -> {
                OnGoingEpisodioViewModel(appPreferences, wordpressDataSource) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> { // Caso para SettingsViewModel
                SettingsViewModel(appPreferences) as T
            }
            else -> throw IllegalArgumentException("Clase ViewModel desconocida en ViewModelFactory: ${modelClass.name}")
        }
    }
}