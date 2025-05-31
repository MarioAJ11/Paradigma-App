package com.example.paradigmaapp.android.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.repository.WordpressService
/**
 * Esta es mi Factory personalizada para crear instancias de ViewModels.
 * Me permite inyectar las dependencias necesarias (como repositorios, SharedPreferences, contexto)
 * a cada ViewModel en el momento de su creación.
 * Utilizo 'CreationExtras' para obtener el 'SavedStateHandle' cuando un ViewModel lo requiere,
 * permitiendo así que los ViewModels puedan guardar y restaurar su estado.
 *
 * @param appPreferences Mi gestor de preferencias compartidas.
 * @param wordpressDataSource La implementación concreta de mis fuentes de datos (WordPress).
 * Aunque los ViewModels dependerán de las interfaces del repositorio,
 * la Factory necesita saber qué implementación concreta instanciar y pasar.
 * @param applicationContext El contexto de la aplicación.
 *
 * @author Mario Alguacil Juárez
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appPreferences: AppPreferences,
    private val wordpressDataSource: WordpressService,
    private val applicationContext: Context
) : ViewModelProvider.Factory {

    // Para ViewModels que gestionan la cola, descargas y episodios en curso,
    // los creo como instancias lazy si no necesitan SavedStateHandle directamente
    // o si su estado se gestiona de otra forma.
    // Sin embargo, para asegurar que reciben la instancia correcta de wordpressDataSource como
    // EpisodioRepository, es más robusto crearlos en el 'when' block si son solicitados,
    // especialmente si hubiera diferentes configuraciones de datasource en el futuro.
    // Por ahora, los mantendré como lazy, asumiendo que wordpressDataSource es estable.
    // Si necesitaran SavedStateHandle, deberían crearse en el `when` con `extras.createSavedStateHandle()`.

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        // En cada caso, paso la instancia 'wordpressDataSource' (que es un WordpressService)
        // a los constructores de los ViewModels que esperan las interfaces
        // ProgramaRepository y/o EpisodioRepository. Kotlin permite esto porque
        // WordpressService implementa dichas interfaces.
        val savedStateHandle = extras.createSavedStateHandle() // Obtener siempre por si se necesita

        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                // MainViewModel necesita ambos repositorios y otras dependencias.
                // También le paso instancias de otros ViewModels que él coordina.
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
                // ProgramaViewModel necesita ambos repositorios y SavedStateHandle.
                ProgramaViewModel(
                    programaRepository = wordpressDataSource,
                    episodioRepository = wordpressDataSource,
                    savedStateHandle = savedStateHandle
                ) as T
            }
            modelClass.isAssignableFrom(EpisodeDetailViewModel::class.java) -> {
                // EpisodeDetailViewModel necesita el EpisodioRepository y SavedStateHandle.
                EpisodeDetailViewModel(
                    episodioRepository = wordpressDataSource,
                    savedStateHandle = savedStateHandle
                ) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                // SearchViewModel necesita el EpisodioRepository.
                // Si necesitara SavedStateHandle, se lo pasaríamos: SearchViewModel(episodioRepository = wordpressDataSource, savedStateHandle = savedStateHandle)
                SearchViewModel(episodioRepository = wordpressDataSource) as T
            }
            modelClass.isAssignableFrom(QueueViewModel::class.java) -> {
                // QueueViewModel necesita AppPreferences y EpisodioRepository.
                QueueViewModel(appPreferences, wordpressDataSource) as T
            }
            modelClass.isAssignableFrom(DownloadedEpisodioViewModel::class.java) -> {
                // DownloadedEpisodioViewModel necesita AppPreferences, EpisodioRepository y el contexto.
                DownloadedEpisodioViewModel(appPreferences, wordpressDataSource, applicationContext) as T
            }
            modelClass.isAssignableFrom(OnGoingEpisodioViewModel::class.java) -> {
                // OnGoingEpisodioViewModel necesita AppPreferences y EpisodioRepository.
                OnGoingEpisodioViewModel(appPreferences, wordpressDataSource) as T
            }
            else -> throw IllegalArgumentException("Clase ViewModel desconocida en ViewModelFactory: ${modelClass.name}")
        }
    }
}