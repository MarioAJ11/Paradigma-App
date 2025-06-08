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
import com.example.paradigmaapp.cache.Database
import com.example.paradigmaapp.cache.DatabaseDriverFactory
import com.example.paradigmaapp.repository.ParadigmaRepository

/**
 * Actividad principal y punto de entrada de la aplicación Paradigma Media para Android.
 *
 * Responsabilidades:
 * - Inicializar dependencias clave de la aplicación como la base de datos [Database],
 * el repositorio [ParadigmaRepository] y las preferencias [AppPreferences].
 * - Configurar la [ViewModelFactory] para inyectar estas dependencias en los ViewModels.
 * - Establecer el contenido de la UI usando Jetpack Compose, inflando [ParadigmaApp]
 * y aplicando el tema de la aplicación.
 *
 * @author Mario Alguacil Juárez
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- INICIALIZACIÓN DE DEPENDENCIAS ---
        // Este es el lugar central donde se crean los objetos que la app necesita para funcionar.

        // 1. Preferencias locales (para ajustes, cola de reproducción, etc.)
        val appPreferencesInstance = AppPreferences(applicationContext)

        // 2. Base de datos (SQLDelight)
        // Creamos la factoría de drivers específica para Android y luego la instancia de la base de datos.
        val databaseDriverFactory = DatabaseDriverFactory(applicationContext)
        val database = Database(databaseDriverFactory)

        // 3. Repositorio principal
        // Creamos el repositorio y le pasamos la base de datos para que pueda usarla como caché.
        val paradigmaRepository = ParadigmaRepository(database)

        // 4. Factoría de ViewModels
        // Creamos la factoría que se encargará de crear todos los ViewModels,
        // pasándoles las dependencias que necesitan.
        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferencesInstance,
            wordpressDataSource = paradigmaRepository, // Le pasamos el nuevo repositorio con caché
            applicationContext = applicationContext
        )

        setContent {
            // Obtenemos el ViewModel de ajustes para gestionar el tema de la app.
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]
            val manualDarkThemeSetting by settingsViewModel.isManuallySetToDarkTheme.collectAsState()

            // Aplicamos el tema de la aplicación.
            Theme(manualDarkThemeSetting = manualDarkThemeSetting) {
                // Obtenemos las instancias de los ViewModels principales que usará la app.
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

                // Composable raíz que contiene toda la navegación y la UI.
                ParadigmaApp(
                    viewModelFactory = viewModelFactory,
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel
                )
            }
        }
    }
}