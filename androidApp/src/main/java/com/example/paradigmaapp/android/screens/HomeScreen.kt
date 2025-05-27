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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.ProgramaListItem
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.model.Programa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onProgramaSelected: (programaId: Int, programaNombre: String) -> Unit,
    onNavigateToSearch: () -> Unit // Mantienes esto si la search icon está en otro lado
) {
    val programas: List<Programa> by mainViewModel.programas.collectAsState()
    val isLoadingProgramas: Boolean by mainViewModel.isLoadingProgramas.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {

        if (isLoadingProgramas && programas.isEmpty()) { // Mostrar carga solo si la lista está vacía
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (programas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay programas disponibles en este momento.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), // Espacio vertical entre items
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Espacio horizontal entre items
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