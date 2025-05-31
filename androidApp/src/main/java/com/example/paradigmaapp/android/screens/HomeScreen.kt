package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.ErrorView // Importar ErrorView
import com.example.paradigmaapp.android.ui.ErrorType // Importar ErrorType
import com.example.paradigmaapp.android.ui.ProgramaListItem
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.model.Programa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onProgramaSelected: (programaId: Int, programaNombre: String) -> Unit
) {
    val programas: List<Programa> by mainViewModel.programas.collectAsState()
    val isLoadingProgramas: Boolean by mainViewModel.isLoadingProgramas.collectAsState()
    val programasError: String? by mainViewModel.programasError.collectAsState() // Observar el nuevo estado de error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding() // Mueve esto aquí para que aplique a toda la columna
    ) {
        // No es necesario TopAppBar si el título está implícito o manejado por NavGraph

        when {
            isLoadingProgramas && programas.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            programasError != null -> { // Mostrar ErrorView si hay un error
                val errorType = if (programasError!!.contains("internet", ignoreCase = true) || programasError!!.contains("conectar", ignoreCase = true)) {
                    ErrorType.NO_INTERNET
                } else {
                    ErrorType.GENERAL_SERVER_ERROR
                }
                ErrorView(
                    message = programasError!!,
                    errorType = errorType,
                    onRetry = { mainViewModel.loadInitialProgramas() }
                )
            }
            programas.isEmpty() && !isLoadingProgramas -> { // Solo si no hay error y está vacío
                ErrorView( // Puedes usar ErrorView también para "no hay contenido"
                    message = "No hay programas disponibles en este momento.",
                    errorType = ErrorType.NO_RESULTS, // Un tipo específico para "no hay resultados"
                    onRetry = { mainViewModel.loadInitialProgramas() } // Opcional: reintentar incluso si no hay contenido
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = programas, key = { programa -> programa.id }) { programa ->
                        ProgramaListItem(
                            programa = programa,
                            onClicked = {
                                onProgramaSelected(programa.id, programa.name)
                            },
                        )
                    }
                }
            }
        }
    }
}