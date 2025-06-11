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

        // 1. Dependencias básicas de la app.
        val appPreferencesInstance = AppPreferences(applicationContext)
        val databaseDriverFactory = DatabaseDriverFactory(applicationContext)
        val database = Database(databaseDriverFactory)

        // 2. Se crea e inicia el servicio de configuración remota.
        remoteConfigService = RemoteConfigService(database)
        lifecycleScope.launch(Dispatchers.IO) {
            remoteConfigService.fetchAndCacheConfig()
        }

        // 3. Se obtiene la configuración (remota, caché o por defecto).
        val config = remoteConfigService.getConfig()

        // 4. Se crea el Repositorio y el servicio de Andaina con las URLs dinámicas.
        val paradigmaRepository = ParadigmaRepository(database, config.wordpressApiBaseUrl)
        val andainaStream = AndainaStream(applicationContext, config.liveStreamUrl, config.liveStreamApiUrl)

        // 5. Se crea la factoría de ViewModels pasándole TODAS las dependencias.
        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferencesInstance,
            wordpressDataSource = paradigmaRepository,
            applicationContext = applicationContext,
            remoteConfigService = remoteConfigService,
            andainaStream = andainaStream // Se la pasamos a la factoría.
        )

        // --- CONSTRUCCIÓN DE LA UI ---
        setContent {
            val settingsViewModel: SettingsViewModel = ViewModelProvider(this, viewModelFactory)[SettingsViewModel::class.java]
            val manualDarkThemeSetting by settingsViewModel.isManuallySetToDarkTheme.collectAsState()

            Theme(manualDarkThemeSetting = manualDarkThemeSetting) {
                // La creación de los ViewModels ahora es limpia, usando la misma factoría para todos.
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