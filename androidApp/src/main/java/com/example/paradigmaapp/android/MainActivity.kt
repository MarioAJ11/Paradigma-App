package com.example.paradigmaapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory
import com.example.paradigmaapp.repository.WordpressService
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel

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

    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appPreferencesInstance = AppPreferences(applicationContext)
        val wordpressServiceInstance = WordpressService()

        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferencesInstance,
            wordpressDataSource = wordpressServiceInstance,
            applicationContext = applicationContext
        )

        setContent {
            // Obtener el SettingsViewModel con el scope de la Activity
            // para que sea la misma instancia o al menos su StateFlow se actualice correctamente.
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]
            val manualDarkThemeSetting by settingsViewModel.isManuallySetToDarkTheme.collectAsState()

            // Pasar la preferencia manual al Composable Theme
            Theme(manualDarkThemeSetting = manualDarkThemeSetting) {
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

                ParadigmaApp(
                    viewModelFactory = viewModelFactory,
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel
                )
            }
        }
    }
}