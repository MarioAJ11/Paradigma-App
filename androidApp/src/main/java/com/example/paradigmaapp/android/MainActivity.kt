package com.example.paradigmaapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Actividad principal de la aplicación.
 * Utiliza Jetpack Compose para construir la interfaz de usuario.
 *
 * @author Mario Alguacil Juárez
 */
class MainActivity : ComponentActivity() {
    /**
     * Método llamado al crear la actividad.
     * Aquí configuramos el contenido de la pantalla.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Aplica el tema personalizado a toda la jerarquía de Composables debajo.
            // Esto asegura que los colores, tipografía, etc., definidos en MyApplicationTheme sean utilizados.
            Theme {
                // Llama al Composable principal que define la estructura de la pantalla de Podcasts.
                AppScreen()
            }
        }
    }
}