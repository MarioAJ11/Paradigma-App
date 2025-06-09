package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.ui.InfoRow

/**
 * Muestra la pantalla de bienvenida (Onboarding) que el usuario ve la primera vez que abre la aplicación.
 * Presenta información introductoria y un botón para continuar a la pantalla principal.
 *
 * @author Mario Alguacil Juárez
 * @param onContinueClicked Lambda que se invoca cuando el usuario pulsa el botón de 'ACEPTAR'.
 * Esta acción se encarga de marcar el onboarding como completado y navegar
 * a la pantalla de inicio.
 *
 * @author Mario Alguacil Juárez
 */
@Composable
fun OnboardingScreen(
    onContinueClicked: () -> Unit
) {
    // Variables con el texto de bienvenida que se mostrará en la pantalla.
    val textoTitulo = "Bienvenid@ a Paradigma Media"
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


    // Contenedor principal que ocupa toda la pantalla y permite el desplazamiento vertical.
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Establece el color de fondo al color primario del tema (amarillo).
            .background(MaterialTheme.colorScheme.primary)
            // Añade padding alrededor del contenido.
            .padding(16.dp)
            // Habilita el scroll si el contenido excede la altura de la pantalla.
            .verticalScroll(rememberScrollState()),
        // Centra todo el contenido horizontalmente.
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Espaciador para separar el contenido del borde superior de la pantalla.
        Spacer(Modifier.height(32.dp))

        // Muestra el título principal de la pantalla.
        Text(
            text = textoTitulo,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            // Establece el color del texto para un buen contraste con el fondo.
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(Modifier.height(24.dp))

        // Muestra el cuerpo del texto principal con la descripción.
        Text(
            text = textoLargoPrincipal,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Justify,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(Modifier.height(32.dp))

        // Fila de información con un icono y texto de ayuda.
        InfoRow()

        Spacer(Modifier.weight(1f))

        // Botón principal de la pantalla para continuar.
        Button(
            // La acción a ejecutar al hacer clic es la que se recibe por parámetro.
            onClick = onContinueClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            // Define colores personalizados para el botón, creando un efecto invertido.
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary, // Fondo del botón
                contentColor = MaterialTheme.colorScheme.primary      // Color del texto del botón
            )
        ) {
            Text("ACEPTAR")
        }
    }
}