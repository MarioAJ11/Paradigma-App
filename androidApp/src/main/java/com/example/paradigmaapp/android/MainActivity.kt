package com.example.paradigmaapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.BuildConfig
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.viewmodel.*
import com.example.paradigmaapp.repository.WordpressService
import timber.log.Timber

/**
 * Actividad principal de la aplicación.
 * Configura el tema, los ViewModels y el Composable raíz de la interfaz de usuario (UI).
 *
 * @author Mario Alguacil Juárez
 */
class MainActivity : ComponentActivity() {

    // Factory para crear instancias de nuestros ViewModels con sus dependencias.
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita la visualización de borde a borde para una UI más inmersiva.
        // Esto permite que la UI se dibuje debajo de las barras de sistema (estado y navegación).
        // Necesitarás manejar los insets (márgenes) en tus Composables (ej. con .statusBarsPadding()).
        enableEdgeToEdge()

        // Inicialización de Timber para logging (registros).
        // Solo se planta el DebugTree si la aplicación está en modo DEBUG.
        // BuildConfig.DEBUG es generado automáticamente por Gradle para tu módulo de aplicación.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Inicializa las dependencias básicas.
        // En una aplicación más grande o de producción, considera usar un framework de
        // Inyección de Dependencias como Hilt o Koin para gestionar esto.
        val appPreferences = AppPreferences(applicationContext)
        val wordpressService = WordpressService() // Idealmente, esto también sería inyectado.

        // Crea la factoría de ViewModels con las dependencias necesarias.
        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferences,
            wordpressService = wordpressService,
            applicationContext = applicationContext // Se pasa el contexto de la aplicación.
        )

        // Establece el contenido de la actividad usando Jetpack Compose.
        setContent {
            // Aplica el tema personalizado (definido en Theme.kt) a toda la jerarquía de Composables.
            Theme {
                // Obtiene las instancias de los ViewModels usando la ViewModelProvider y la factory.
                // El ciclo de vida de estos ViewModels estará ligado al de esta Activity.
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]
                val queueViewModel: QueueViewModel = ViewModelProvider(this, viewModelFactory)[QueueViewModel::class.java]
                val downloadedViewModel: DownloadedEpisodioViewModel = ViewModelProvider(this, viewModelFactory)[DownloadedEpisodioViewModel::class.java]
                val onGoingViewModel: OnGoingEpisodioViewModel = ViewModelProvider(this, viewModelFactory)[OnGoingEpisodioViewModel::class.java]

                // Llama al Composable raíz de la aplicación, pasando los ViewModels y otras dependencias.
                ParadigmaApp(
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel,
                    queueViewModel = queueViewModel,
                    downloadedViewModel = downloadedViewModel,
                    onGoingViewModel = onGoingViewModel,
                    appPreferences = appPreferences // Se pasa appPreferences por si algún Composable de UI lo necesita directamente, aunque es mejor a través de ViewModel.
                )
            }
        }
    }
}