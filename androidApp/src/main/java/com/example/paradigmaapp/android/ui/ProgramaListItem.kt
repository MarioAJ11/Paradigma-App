package com.example.paradigmaapp.android.ui // O tu paquete preferido para componentes UI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.utils.extractTextFromHtml // Importa la función de utilidad
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities // Importa la función de utilidad
import com.example.paradigmaapp.model.Programa

/**
 * Composable para mostrar un item individual en la lista de programas.
 *
 * @param programa El objeto [Programa] a mostrar.
 * @param onClicked Lambda que se invoca al hacer clic en el item.
 * @author Mario Alguacil Juárez
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramaListItem(
    programa: Programa,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier // Añadido para flexibilidad
) {
    Card(
        onClick = onClicked,
        modifier = modifier // Usar el modifier pasado
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = programa.name.unescapeHtmlEntities(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            programa.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc.extractTextFromHtml().unescapeHtmlEntities(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            programa.count?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Episodios: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}