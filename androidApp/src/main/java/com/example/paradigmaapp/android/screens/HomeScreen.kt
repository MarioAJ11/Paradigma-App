package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Asegúrate de importar items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.* // Importa getValue y collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.ProgramaListItem // Importa tu Composable para items de programa
import com.example.paradigmaapp.android.viewmodel.MainViewModel
import com.example.paradigmaapp.model.Programa // Importa el modelo Programa del módulo shared

/**
 * Pantalla principal que muestra la lista de programas de radio.
 * Al seleccionar un programa, se navega a la pantalla de sus episodios.
 *
 * @param mainViewModel ViewModel principal para obtener la lista de programas.
 * @param onProgramaSelected Lambda que se invoca cuando un programa es seleccionado.
 * Recibe el ID y el nombre del programa para la siguiente pantalla.
 * @param onNavigateToSearch Lambda para navegar a la pantalla de búsqueda dedicada.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onProgramaSelected: (programaId: Int, programaNombre: String) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    // Asegúrate de que mainViewModel.programas es StateFlow<List<Programa>>
    // y mainViewModel.isLoadingProgramas es StateFlow<Boolean>
    val programas: List<Programa> by mainViewModel.programas.collectAsState()
    val isLoadingProgramas: Boolean by mainViewModel.isLoadingProgramas.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoadingProgramas) {
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
                    text = "No hay programas disponibles en este momento. Intenta refrescar.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Aquí, 'programa' debería ser de tipo Programa
                items(items = programas, key = { programa -> programa.id }) { programa ->
                    ProgramaListItem(
                        programa = programa, // Si aquí hay un type mismatch, 'programas' no es List<Programa>
                        onClicked = {
                            onProgramaSelected(programa.id, programa.name)
                        }
                    )
                }
            }
        }
    }
}