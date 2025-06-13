package com.example.paradigmaapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.paradigmaapp.android.api.AndainaStream
import com.example.paradigmaapp.android.data.AppPreferences
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.android.viewmodel.SearchViewModel
import com.example.paradigmaapp.android.viewmodel.SettingsViewModel
import com.example.paradigmaapp.android.viewmodel.ViewModelFactory
import com.example.paradigmaapp.cache.Database
import com.example.paradigmaapp.cache.DatabaseDriverFactory
import com.example.paradigmaapp.config.RemoteConfigService
import com.example.paradigmaapp.repository.ParadigmaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Actividad principal y punto de entrada de la aplicación.
 * Responsable de inicializar todas las dependencias clave y la UI.
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModelFactory: ViewModelFactory
    private lateinit var remoteConfigService: RemoteConfigService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- INICIALIZACIÓN DE DEPENDENCIAS ---
        val appPreferencesInstance = AppPreferences(applicationContext)
        val databaseDriverFactory = DatabaseDriverFactory(applicationContext)
        val database = Database(databaseDriverFactory)

        remoteConfigService = RemoteConfigService(database)
        lifecycleScope.launch(Dispatchers.IO) {
            remoteConfigService.fetchAndCacheConfig()
        }

        val config = remoteConfigService.getConfig()
        val paradigmaRepository = ParadigmaRepository(database, config.wordpressApiBaseUrl)
        val andainaStream = AndainaStream(applicationContext, config.liveStreamUrl, config.liveStreamApiUrl)

        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferencesInstance,
            wordpressDataSource = paradigmaRepository,
            applicationContext = applicationContext,
            remoteConfigService = remoteConfigService,
            andainaStream = andainaStream
        )

        // --- CONSTRUCCIÓN DE LA UI ---
        setContent {
            // Se crea una ÚNICA instancia del SettingsViewModel para toda la Activity.
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]
            val manualDarkThemeSetting by settingsViewModel.isManuallySetToDarkTheme.collectAsState()

            // Se aplica el tema observando el estado de ese ViewModel.
            Theme(manualDarkThemeSetting = manualDarkThemeSetting) {
                // Se crean los otros ViewModels.
                val mainViewModel: MainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
                val searchViewModel: SearchViewModel = ViewModelProvider(this, viewModelFactory)[SearchViewModel::class.java]

                // Se pasan los ViewModels necesarios al Composable raíz de la app.
                ParadigmaApp(
                    viewModelFactory = viewModelFactory,
                    mainViewModel = mainViewModel,
                    searchViewModel = searchViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}