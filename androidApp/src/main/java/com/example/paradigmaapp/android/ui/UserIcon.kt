package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * Composable para mostrar un icono de usuario circular.
 * Podría ser interactivo en el futuro (ej. para abrir un perfil o ajustes).
 *
 * @param modifier Modificador opcional para aplicar a este Composable.
 * @param onClick Lambda opcional que se invoca al hacer clic en el icono.
 */
@Composable
fun UserIcon(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null // Permite que el icono sea clickeable
) {
    Box(
        modifier = modifier
            .size(48.dp) // Tamaño del círculo
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(4.dp) // Espacio interno
        // TODO:Añadir .clickable { onClick?.invoke() }
        ,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = "Icono de usuario",
            tint = MaterialTheme.colorScheme.onPrimary // Color del icono
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUserIcon() {
    UserIcon()
}