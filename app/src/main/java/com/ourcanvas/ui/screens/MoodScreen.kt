package com.ourcanvas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MoodScreen(
    onMoodSelected: (String) -> Unit
) {
    val moods = listOf("😊", "😢", "😠", "😍", "😂", "🤔", "😴", "🥳", "🤯", "😇", "😎", "🥺")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "How are you feeling?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularMoodSelector(moods = moods, onMoodSelected = onMoodSelected)
        }
    }
}

@Composable
fun CircularMoodSelector(
    moods: List<String>,
    onMoodSelected: (String) -> Unit
) {
    Box(modifier = Modifier.size(300.dp)) {
        val angleStep = 2 * Math.PI / moods.size
        moods.forEachIndexed { index, mood ->
            val angle = index * angleStep
            val x = (120 * cos(angle)).toFloat()
            val y = (120 * sin(angle)).toFloat()

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onMoodSelected(mood) }
                    .align(Alignment.Center)
                    .offset(x.dp, y.dp)
            ) {
                Text(
                    text = mood,
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}