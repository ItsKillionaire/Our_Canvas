package com.ourcanvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ourcanvas.ui.viewmodels.CanvasViewModel.CanvasState
import androidx.compose.ui.graphics.nativeCanvas
import com.ourcanvas.data.model.PointF
import com.ourcanvas.ui.viewmodels.CanvasViewModel.CanvasEvent
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.TextField
import com.ourcanvas.ui.viewmodels.CanvasViewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController

// ... existing code ...

@Preview(showBackground = true)
@Composable
fun MoodIndicatorPreview() {
    MoodIndicator(mood = "😊")
}

@Preview(showBackground = true)
@Composable
fun DrawingControlsPreview() {
    DrawingControls(
        state = CanvasState(),
        onColorSelected = {},
        onStrokeWidthChanged = {}
    )
}

@Preview(showBackground = true)
@Composable
fun TextInputFieldPreview() {
    TextInputField(
        textObject = com.ourcanvas.data.model.TextObject(text = "Hello"),
        onDismiss = {},
        onTextChanged = {}
    )
}

@Preview(showBackground = true)
@Composable
fun SharedCanvasPreview() {
    SharedCanvas(
        modifier = Modifier.fillMaxSize(),
        state = CanvasState(
            drawingPaths = listOf(
                com.ourcanvas.data.model.DrawPath(
                    points = listOf(
                        PointF(100f, 100f),
                        PointF(200f, 200f)
                    )
                )
            ),
            textObjects = listOf(
                com.ourcanvas.data.model.TextObject(
                    text = "Hello Preview",
                    x = 300f,
                    y = 300f
                )
            )
        ),
        onDrawPath = {},
        onToggleTextField = {}
    )
}

@Composable
fun CanvasScreen(
    navController: NavController,
    viewModel: CanvasViewModel = hiltViewModel()
) {
    val state by viewModel.canvasState.collectAsState()
    val navigateToCoupleScreen by viewModel.navigateToCoupleScreen.collectAsState()

    if (navigateToCoupleScreen) {
        navController.navigate("couple") {
            popUpTo("canvas") { inclusive = true }
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            SharedCanvas(
                modifier = Modifier.fillMaxSize(),
                state = state,
                onDrawPath = { viewModel.onEvent(CanvasEvent.DrawPath(it)) },
                onToggleTextField = { viewModel.onEvent(CanvasEvent.ToggleTextField(it)) }
            )
            MoodIndicator(
                mood = state.currentUser?.mood ?: "😊",
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                onMoodSelected = { viewModel.onEvent(CanvasEvent.UpdateMood(it)) }
            )
            Button(
                onClick = { viewModel.onEvent(CanvasEvent.LeaveCouple) },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Text("Leave Couple")
            }
            DrawingControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                state = state,
                onColorSelected = { viewModel.onEvent(CanvasEvent.SelectColor(it)) },
                onStrokeWidthChanged = { viewModel.onEvent(CanvasEvent.SelectStrokeWidth(it)) }
            )
            if (state.showTextField) {
                state.currentTextObject?.let {
                    TextInputField(
                        textObject = it,
                        onDismiss = { viewModel.onEvent(CanvasEvent.ToggleTextField()) },
                        onTextChanged = {
                            viewModel.onEvent(CanvasEvent.AddOrUpdateText(it))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SharedCanvas(
    modifier: Modifier = Modifier,
    state: CanvasState,
    onDrawPath: (com.ourcanvas.data.model.DrawPath) -> Unit,
    onToggleTextField: (com.ourcanvas.data.model.TextObject) -> Unit
) {
    val currentPath = remember { mutableStateListOf<PointF>() }

    Canvas(modifier = modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                onToggleTextField(com.ourcanvas.data.model.TextObject(x = it.x, y = it.y))
            }
        )
    }.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                currentPath.clear()
            },
            onDrag = { change, _ ->
                currentPath.add(PointF(change.position.x, change.position.y))
                change.consume()
            },
            onDragEnd = {
                onDrawPath(com.ourcanvas.data.model.DrawPath(points = currentPath.toList(), color = state.selectedColor, strokeWidth = state.selectedStrokeWidth))
            }
        )
    }) {
        state.drawingPaths.forEach { path ->
            val composePath = Path()
            path.points.forEachIndexed { i, point ->
                if (i == 0) {
                    composePath.moveTo(point.x, point.y)
                } else {
                    composePath.lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = composePath,
                color = Color(path.color),
                style = Stroke(width = path.strokeWidth)
            )
        }

        state.textObjects.forEach { textObject ->
            drawContext.canvas.nativeCanvas.drawText(
                textObject.text,
                textObject.x,
                textObject.y,
                android.graphics.Paint().apply {
                    color = textObject.color.toInt()
                    textSize = textObject.fontSize
                }
            )
        }

        val composePath = Path()
        currentPath.forEachIndexed { i, point ->
            if (i == 0) {
                composePath.moveTo(point.x, point.y)
            } else {
                composePath.lineTo(point.x, point.y)
            }
        }
        drawPath(
            path = composePath,
            color = Color(state.selectedColor),
            style = Stroke(width = state.selectedStrokeWidth)
        )
    }
}

@Composable
fun MoodIndicator(
    mood: String,
    modifier: Modifier = Modifier,
    onMoodSelected: ((String) -> Unit)? = null
) {
    Text(
        text = mood,
        fontSize = 48.sp,
        modifier = modifier.clickable {
            onMoodSelected?.invoke(if (mood == "😊") "😢" else "😊")
        }
    )
}

@Composable
fun DrawingControls(
    modifier: Modifier = Modifier,
    state: CanvasState,
    onColorSelected: (Long) -> Unit,
    onStrokeWidthChanged: (Float) -> Unit
) {
    val colors = listOf(0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00)

    Column(modifier = modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Colors:", modifier = Modifier.padding(end = 8.dp))
            colors.forEach { color ->
                Button(
                    onClick = { onColorSelected(color) },
                    modifier = Modifier.size(40.dp).padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(color)))
                }
            }
        }
        Slider(
            value = state.selectedStrokeWidth,
            onValueChange = { onStrokeWidthChanged(it) },
            valueRange = 1f..50f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TextInputField(
    textObject: com.ourcanvas.data.model.TextObject,
    onDismiss: () -> Unit,
    onTextChanged: (com.ourcanvas.data.model.TextObject) -> Unit
) {
    var text by remember { mutableStateOf(textObject.text) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter text") }
            )
            Row {
                Button(onClick = {
                    onTextChanged(textObject.copy(text = text))
                    onDismiss()
                }) {
                    Text("OK")
                }
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}
