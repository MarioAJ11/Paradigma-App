package com.example.paradigmaapp.android.inicio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Diapositiva 2: Texto informativo largo.
 * @param onScreenClick Lambda que se ejecuta al hacer clic en cualquier parte de la pantalla.
 */
@Composable
fun OnboardingPage2(onScreenClick: () -> Unit) {
    val textoLargoPrincipal = "Paradigma Media Andalucía (en adelante Paradigma) es una iniciativa ciudadana que " +
            "surge de la necesidad de cubrir las carencias incuestionables que tiene la sociedad en " +
            "general, y la cordobesa en particular, sobre la información que le afecta de " +
            "primera mano.\n\nParadigma tiene entidad sin ánimo de lucro. Todo lo recaudado " +
            "por la Asociación será dirigido a conseguir los medios técnicos y humanos necesarios " +
            "para mantener la mínima calidad exigible a un medio de comunicación en manos de la " +
            "ciudadanía.\n\nLos contenidos de nuestros medios de comunicación serán de eminente " +
            "carácter social. De hecho, servirán para dar voz a todos los colectivos sociales que q" +
            "uieran usarlos para dar a conocer sus problemáticas, sus luchas, sus denuncias, sus " +
            "obstáculos, sus relaciones con las instituciones. Asimismo, se elaborarán contenidos " +
            "en los que se explique de forma exhaustiva los procesos sociales, legales, laborales y, " +
            "en general, políticos, que afectan de primera mano a la sociedad. También habrá programas " +
            "de diversión, infantiles, de participación directa de la audiencia.\n\nParadigma " +
            "comienza en Córdoba, aunque nuestro proyecto ampara la colaboración y extensión por " +
            "toda Andalucía. Paradigma constará de tres medios de comunicación:\n   • Paradigma Radio," +
            " que emitirá tanto en FM como en streaming a través de internet, en directo.\n   " +
            "• Paradigma TV, que emitirá a través del canal de YouTube “Paradigma Tv Andalucía”.\n   " +
            "• Paradigma Prensa. Se trata de un periódico diario digital.\n\nTodas nuestras" +
            " producciones se harán y distribuirán bajo licencia de Creative Commons."

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onScreenClick)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Permite scroll si el texto no cabe
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(38.dp))
        Text(
            text = textoLargoPrincipal,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Justify,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.height(80.dp))
    }
}