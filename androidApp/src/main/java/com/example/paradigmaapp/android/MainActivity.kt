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
 * Configura el tema, los ViewModels y el Composable raíz de la UI.
 *
 * @author Mario Alguacil Juárez
 */
class MainActivity : ComponentActivity() {

    private lateinit var viewModelFactory: ViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Inicializa dependencias básicas.
        val appPreferences = AppPreferences(applicationContext)
        val wordpressService = WordpressService()
        viewModelFactory = ViewModelFactory(
            appPreferences = appPreferences,
            wordpressService = wordpressService,
            applicationContext = applicationContext
        )

        setContent {
            Theme {
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