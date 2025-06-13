package com.example.paradigmaapp.android.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.inicio.OnboardingPage1
import com.example.paradigmaapp.android.inicio.OnboardingPage2
import com.example.paradigmaapp.android.inicio.OnboardingPage3
import kotlinx.coroutines.launch

/**
 * Muestra la pantalla de bienvenida (Onboarding) con un sistema de diapositivas (pager).
 * Guía al usuario a través de varias pantallas informativas antes de entrar a la app.
 *
 * @param onContinueClicked Lambda que se invoca cuando el usuario pulsa el botón final de 'ACEPTAR'.
 *
 * @author Mario Alguacil Juárez (con implementación de Pager)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onContinueClicked: () -> Unit
) {
    // Definimos el número total de diapositivas
    val pageCount = 3
    // Estado para controlar y recordar la página actual del Pager
    val pagerState = rememberPagerState { pageCount }
    // Scope de corutina para poder controlar el Pager de forma programática (con animación)
    val coroutineScope = rememberCoroutineScope()

    // Contenedor principal con el color de fondo de la marca
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        // HorizontalPager es el componente que permite deslizar entre diapositivas
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // El contenido de cada página se decide con un 'when'
            when (page) {
                0 -> OnboardingPage1 {
                    // Al hacer clic, se anima el scroll a la siguiente página
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                }
                1 -> OnboardingPage2 {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                }
                2 -> OnboardingPage3(onContinueClicked)
            }
        }
    }
}