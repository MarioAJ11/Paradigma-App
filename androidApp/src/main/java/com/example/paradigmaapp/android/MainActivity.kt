package com.example.paradigmaapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory
import com.example.paradigmaapp.repository.WordpressService

/**
 * Actividad principal y punto de entrada de la aplicación Paradigma Media para Android.
 *
 * Responsabilidades:
 * - Habilitar el modo Edge-to-Edge para la UI.
 * - Inicializar dependencias clave como [AppPreferences], [WordpressService] (fuente de datos),
 * y la [ViewModelFactory] personalizada.
 * - Configurar el contenido de la UI usando Jetpack Compose, inflando [ParadigmaApp]
 * como el Composable raíz.
 * - Proveer los ViewModels necesarios ([MainViewModel], [SearchViewModel], [SettingsViewModel])
 * a la jerarquía de Composables, gestionando su ciclo de vida a través de [ViewModelProvider].
 * - Aplicar el tema de la aplicación ([Theme]) y observar las preferencias de tema del usuario
 * para actualizaciones en tiempo real.
 *
 * @author Mario Alguacil Juárez
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Habilita la UI de borde a borde

        // Inicialización de dependencias
        val appPreferencesInstance = AppPreferences(applicationContext)
        val wordpressServiceInstance = WordpressService() // Implementación del servicio de datos

        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferencesInstance,
            wordpressDataSource = wordpressServiceInstance,
            applicationContext = applicationContext
        )

        setContent {
            // Obtener el SettingsViewModel con el scope de esta Activity.
            // Este ViewModel gestiona las preferencias de tema y es observado para aplicar el tema dinámicamente.
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]
            val manualDarkThemeSetting by settingsViewModel.isManuallySetToDarkTheme.collectAsState()

            // Aplicar el tema de la aplicación, pasando la preferencia manual del usuario.
            Theme(manualDarkThemeSetting = manualDarkThemeSetting) {
                // Obtener los ViewModels principales para la aplicación.
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

                // Composable raíz de la aplicación que contiene el NavGraph y la UI principal.
                ParadigmaApp(
                    viewModelFactory = viewModelFactory,
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel
                )
            }
        }
    }
}