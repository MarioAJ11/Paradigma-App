package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.ErrorType
import com.example.paradigmaapp.android.ui.ErrorView
import com.example.paradigmaapp.android.ui.ProgramaListItem
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.model.Programa

/**
 * Pantalla principal de la aplicación que muestra una cuadrícula de [Programa]s disponibles.
 * Gestiona los estados de carga, error y contenido vacío.
 *
 * @param mainViewModel El [MainViewModel] que proporciona la lista de programas y su estado de carga/error.
 * @param onProgramaSelected Lambda que se invoca cuando el usuario selecciona un programa de la lista.
 * Recibe el ID y el nombre del programa seleccionado.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onProgramaSelected: (programaId: Int, programaNombre: String) -> Unit
) {
    // Observa los estados del MainViewModel
    val programas: List<Programa> by mainViewModel.programas.collectAsState()
    val isLoadingProgramas: Boolean by mainViewModel.isLoadingProgramas.collectAsState()
    val programasError: String? by mainViewModel.programasError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding() // Aplica padding para la barra de estado al contenedor principal
    ) {
        when {
            // Estado de carga: Muestra un indicador de progreso si se están cargando y no hay programas aún.
            isLoadingProgramas && programas.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // Estado de error: Muestra el componente ErrorView.
            programasError != null -> {
                val errorType = if (programasError!!.contains("internet", ignoreCase = true) ||
                    programasError!!.contains("conectar", ignoreCase = true)) {
                    ErrorType.NO_INTERNET
                } else {
                    ErrorType.GENERAL_SERVER_ERROR
                }
                ErrorView(
                    message = programasError!!,
                    errorType = errorType,
                    onRetry = { mainViewModel.loadInitialProgramas() } // Acción para reintentar la carga
                )
            }
            // Estado de contenido vacío (sin error): Muestra un mensaje indicando que no hay programas.
            programas.isEmpty() && !isLoadingProgramas -> {
                ErrorView(
                    message = "No hay programas disponibles en este momento.",
                    errorType = ErrorType.NO_RESULTS, // Tipo específico para "sin resultados"
                    onRetry = { mainViewModel.loadInitialProgramas() } // Opcional: permitir reintentar
                )
            }
            // Estado con contenido: Muestra la cuadrícula de programas.
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), // Cuadrícula con dos columnas
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), // Padding alrededor de la cuadrícula
                    verticalArrangement = Arrangement.spacedBy(12.dp), // Espacio vertical entre ítems
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Espacio horizontal entre ítems
                ) {
                    items(items = programas, key = { programa -> programa.id }) { programa ->
                        ProgramaListItem(
                            programa = programa,
                            onClicked = {
                                onProgramaSelected(programa.id, programa.name)
                            }
                        )
                    }
                }
            }
        }
    }
}