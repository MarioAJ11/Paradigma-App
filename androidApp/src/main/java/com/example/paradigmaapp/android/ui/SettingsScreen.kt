package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * Composable que representa la pantalla de ajustes de la aplicación.
 * Permite al usuario configurar diversas opciones.
 *
 * @param navController El NavHostController para manejar la navegación (ej. volver atrás).
 */
@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar en Material3
@Composable
fun SettingsScreen(navController: NavHostController) {
    // Scaffold proporciona una estructura básica para la pantalla
    Scaffold(
        topBar = {
            // TopAppBar para la barra superior con título y botón de retroceso
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    // Botón de retroceso que usa popBackStack para volver
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // Contenido principal de la pantalla de ajustes
        Column(
            modifier = Modifier
                .padding(paddingValues) // Aplica el padding proporcionado por Scaffold
                .fillMaxSize() // Llena el espacio disponible
                .padding(16.dp), // Padding interno para el contenido
            horizontalAlignment = Alignment.CenterHorizontally, // Alinea el contenido horizontalmente
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre elementos
        ) {
            // Título de la sección de ajustes
            Text(
                "Configuración del Usuario",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Divider() // Separador visual

            // TODO: Añadie los elementos de UI para tus ajustes
            // Ejemplo provisional
            Text(
                "Opción de ajuste 1",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(checked = false, onCheckedChange = {}) // Un ejemplo de un switch

            Text(
                "Opción de ajuste 2",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettingsScreen() {
    // Para el Preview, necesitamos un NavHostController "dummy".
    // rememberNavController() funciona en @Preview Composable.
    SettingsScreen(navController = rememberNavController())
}