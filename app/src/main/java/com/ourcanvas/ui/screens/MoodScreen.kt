package com.ourcanvas.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MoodScreen(
    onMoodSelected: (String) -> Unit
) {
    val moods = listOf("😊", "😢", "😠", "😍", "😂", "🤔", "😴", "🥳", "🤯", "😇", "😎", "🥺")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("How are you feeling?", fontSize = 24.sp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.padding(16.dp)
        ) {
            items(moods) {
                Text(
                    text = it,
                    fontSize = 48.sp,
                    modifier = Modifier.clickable { onMoodSelected(it) }
                )
            }
        }
    }
}
