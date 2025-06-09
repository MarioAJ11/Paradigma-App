package com.example.paradigmaapp.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Composable para un Divider con padding vertical. */
@Composable
fun ListDivider(modifier: Modifier = Modifier) {
    Divider(
        modifier = modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}