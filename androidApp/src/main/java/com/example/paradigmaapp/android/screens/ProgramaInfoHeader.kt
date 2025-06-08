package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.paradigmaapp.android.R
import com.example.paradigmaapp.android.utils.extractMeaningfulDescription
import com.example.paradigmaapp.android.utils.unescapeHtmlEntities
import com.example.paradigmaapp.model.Programa


/**
 * Composable auxiliar para la cabecera del programa.
 *
 * @param programa El objeto [Programa] que se mostrará en la cabecera.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun ProgramaInfoHeader(programa: Programa?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (programa == null) {
            Box(Modifier.height(280.dp), Alignment.Center) { CircularProgressIndicator() }
        } else {
            AsyncImage(
                model = programa.imageUrl ?: programa.imagenDelPrograma?.guid,
                contentDescription = "Portada de ${programa.name.unescapeHtmlEntities()}",
                modifier = Modifier.size(180.dp).shadow(6.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                error = painterResource(R.mipmap.logo_foreground),
                placeholder = painterResource(R.mipmap.logo_foreground)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = programa.name.unescapeHtmlEntities(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            programa.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc.extractMeaningfulDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}