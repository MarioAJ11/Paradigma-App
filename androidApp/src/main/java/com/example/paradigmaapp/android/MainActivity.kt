package com.example.paradigmaapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
// import androidx.media3.common.BuildConfig // BuildConfig de Media3 no suele usarse así. Usar el de la app.
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory
import com.example.paradigmaapp.repository.WordpressService
// import timber.log.Timber // Timber eliminado según solicitud

/**
 * Esta es la actividad principal y punto de entrada de mi aplicación Android.
 * Aquí configuro el tema de la aplicación, inicializo las dependencias clave como
 * [AppPreferences], [WordpressService] (mi fuente de datos), y la [ViewModelFactory].
 * Luego, establezco el contenido de la UI usando Jetpack Compose, inflando mi
 * Composable raíz [ParadigmaApp] y proveyéndole los ViewModels necesarios.
 * También habilito el modo Edge-to-Edge para una mejor experiencia visual.
 *
 * @author Mario Alguacil Juárez
 */
class MainActivity : ComponentActivity() {

    // Mi factory personalizada para crear ViewModels con sus dependencias.
    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilito que la UI se dibuje de borde a borde (debajo de las barras de sistema).
        enableEdgeToEdge()

        // Inicializo las dependencias básicas que necesitará mi ViewModelFactory.
        val appPreferences = AppPreferences(applicationContext)
        // Esta es la instancia de mi implementación concreta del servicio de datos.
        val wordpressServiceInstance = WordpressService()

        // Creo la instancia de mi ViewModelFactory, pasándole las dependencias.
        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferences,
            wordpressDataSource = wordpressServiceInstance,
            applicationContext = applicationContext
        )

        // Establezco el contenido de la actividad usando Jetpack Compose.
        setContent {
            // Aplico mi tema personalizado.
            Theme {
                // Obtengo las instancias de mis ViewModels principales usando la factory.
                // El ViewModelProvider se encarga del ciclo de vida de estos ViewModels.
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

                // Llamo a mi Composable raíz, pasándole la factory y los ViewModels.
                ParadigmaApp(
                    viewModelFactory = viewModelFactory,
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel
                )
            }
        }
    }
}