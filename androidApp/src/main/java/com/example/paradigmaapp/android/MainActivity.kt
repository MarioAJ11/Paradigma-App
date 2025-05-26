package com.example.paradigmaapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.BuildConfig
// Importa el BuildConfig de tu propio módulo de aplicación
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.viewmodel.* // Asegúrate que todos los ViewModels están aquí
import com.example.paradigmaapp.repository.WordpressService
import timber.log.Timber

/**
 * Actividad principal de la aplicación.
 * Configura el tema, los ViewModels y el Composable raíz de la UI.
 *
 * @author Mario Alguacil Juárez
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicialización de Timber para logging (solo en Debug)
        if (BuildConfig.DEBUG) { // Usa el BuildConfig generado para tu app
            Timber.plant(Timber.DebugTree())
        }

        // Inicializa dependencias básicas.
        val appPreferences = AppPreferences(applicationContext)
        val wordpressService = WordpressService() // Idealmente, esto sería inyectado.

        // Crea la factoría de ViewModels.
        // 'this' (la Activity) actúa como SavedStateRegistryOwner.
        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferences,
            wordpressService = wordpressService,
            applicationContext = applicationContext,
            owner = this // Pasar la Activity como SavedStateRegistryOwner
            // defaultArgs = intent?.extras // Opcional, si tienes argumentos de intent para el SavedStateHandle inicial
        )

        setContent {
            Theme { // Tu Composable Theme existente
                // Obtiene las instancias de los ViewModels usando la ViewModelProvider y la factory.
                // 'this' (la Activity) es el ViewModelStoreOwner.
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]
                // Los siguientes ViewModels son instanciados por MainViewModel o por la factory directamente
                // y pasados a MainViewModel. Aquí nos aseguramos que MainViewModel tiene las referencias correctas.
                // Si se necesitan en otros lugares, se obtendrían con el factory de manera similar.
                // QueueViewModel, DownloadedEpisodioViewModel, OnGoingEpisodioViewModel son accedidos via mainViewModel.

                ParadigmaApp(
                    viewModelFactory = viewModelFactory, // Pasar la factory para ViewModels anidados en NavGraph si es necesario
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel
                    // Los otros ViewModels principales (Queue, Downloaded, OnGoing)
                    // ahora son accedidos a través de MainViewModel si es necesario,
                    // o instanciados directamente en NavGraph con la factory si se prefiere
                    // y el NavBackStackEntry como owner.
                )
            }
        }
    }
}