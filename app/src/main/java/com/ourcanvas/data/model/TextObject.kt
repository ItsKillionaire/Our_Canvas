package com.ourcanvas.data.model

import androidx.compose.ui.graphics.Color

// Represents a piece of text on the canvas.
data class TextObject(
    val id: String = "",
    val text: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val color: Long = Color.Black.value.toLong(),
    val fontSize: Float = 24f
)
