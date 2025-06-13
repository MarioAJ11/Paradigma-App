package com.example.paradigmaapp.android.inicio

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.R
import androidx.compose.runtime.getValue

/**
 * Diapositiva 1: Bienvenida con animación e imagen.
 * @param onScreenClick Lambda que se ejecuta al hacer clic en cualquier parte de la pantalla.
 */
@Composable
fun OnboardingPage1(onScreenClick: () -> Unit) {
    // Animación infinita para un efecto sutil de "pulso" en la imagen.
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
        ), label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onScreenClick) // La pantalla entera es clickable
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.paradigma_inicio),
            contentDescription = "Logo de Paradigma Media",
            modifier = Modifier
                .fillMaxWidth(0.8f) // Ocupa el 80% del ancho
                .scale(scale) // Aplica la animación de escala
        )
        Spacer(Modifier.height(48.dp))
        Text(
            text = "Bienvenid@ a Paradigma Media",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}