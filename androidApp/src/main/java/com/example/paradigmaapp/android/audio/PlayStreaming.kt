package com.example.paradigmaapp.android.audio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.paradigmaapp.android.R

@Composable
fun PlayStreaming(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.mipmap.play),
        contentDescription = "Play",
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() }
            .padding(8.dp)
    )
}