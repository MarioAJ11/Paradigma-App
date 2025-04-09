package com.example.paradigmaapp.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PodcastList(podcasts: List<Podcast>, onPodcastSelected: (Podcast) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(podcasts) { podcast ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF747272)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onPodcastSelected(podcast) }
            ) {
                Text(
                    text = podcast.title,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
