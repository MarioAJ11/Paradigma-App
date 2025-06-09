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

        // Este es el lugar central donde se crean los objetos que la app necesita para funcionar.
        val appPreferencesInstance = AppPreferences(applicationContext)

        // Creamos la factoría de drivers específica para Android y luego la instancia de la base de datos.
        val databaseDriverFactory = DatabaseDriverFactory(applicationContext)
        val database = Database(databaseDriverFactory)

        // Creamos el repositorio y le pasamos la base de datos para que pueda usarla como caché.
        val paradigmaRepository = ParadigmaRepository(database)

        // Creamos la factoría que se encargará de crear todos los ViewModels,
        // pasándoles las dependencias que necesitan.
        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferencesInstance,
            wordpressDataSource = paradigmaRepository, // Le pasamos el nuevo repositorio con caché
            applicationContext = applicationContext
        )

        setContent {
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]
            val manualDarkThemeSetting by settingsViewModel.isManuallySetToDarkTheme.collectAsState()

            Theme(manualDarkThemeSetting = manualDarkThemeSetting) {
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

                // La llamada ahora es más sencilla
                ParadigmaApp(
                    viewModelFactory = viewModelFactory,
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel
                )
            }
        }
    }
}