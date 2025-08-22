package com.ourcanvas.data.model

import androidx.compose.ui.graphics.Color

// Represents a single continuous path drawn on the canvas.
// A new path is created every time the user lifts their finger.
data class DrawPath(
    val points: List<PointF> = emptyList(), // List of x,y coordinates
    val color: Long = Color.Black.value.toLong(), // Color packed into a Long
    val strokeWidth: Float = 8f,
    val id: String = "", // Unique ID for the path
    val userId: String = "" // ID of the user who drew the path
)

// Represents a single point in a path.
data class PointF(
    val x: Float = 0f,
    val y: Float = 0f
)
