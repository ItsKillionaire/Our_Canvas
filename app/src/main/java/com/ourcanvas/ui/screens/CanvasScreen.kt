package com.ourcanvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ourcanvas.data.model.PointF
import com.ourcanvas.ui.viewmodels.CanvasViewModel
import com.ourcanvas.ui.viewmodels.CanvasViewModel.CanvasState

@Composable
fun CanvasScreen(
    navController: NavController,
    viewModel: CanvasViewModel = hiltViewModel()
) {
    val state by viewModel.canvasState.collectAsState()
    val navigateToCoupleScreen by viewModel.navigateToCoupleScreen.collectAsState()
    var showLeaveCoupleDialog by remember { mutableStateOf(false) }

    if (navigateToCoupleScreen) {
        navController.navigate("couple") {
            popUpTo("canvas") { inclusive = true }
        }
        viewModel.onNavigationDone()
    }

    if (showLeaveCoupleDialog) {
        LeaveCoupleDialog(
            onConfirm = {
                viewModel.onEvent(CanvasViewModel.CanvasEvent.LeaveCouple)
                showLeaveCoupleDialog = false
            },
            onDismiss = { showLeaveCoupleDialog = false }
        )
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Scaffold(
            topBar = {
                CanvasTopBar(
                    state = state,
                    onLeaveCouple = { showLeaveCoupleDialog = true },
                    onUpdateMood = { viewModel.onEvent(CanvasViewModel.CanvasEvent.UpdateMood(it)) }
                )
            },
            bottomBar = {
                DrawingControls(
                    state = state,
                    onColorSelected = { viewModel.onEvent(CanvasViewModel.CanvasEvent.SelectColor(it)) },
                    onStrokeWidthChanged = { viewModel.onEvent(CanvasViewModel.CanvasEvent.SelectStrokeWidth(it)) },
                    onUndo = { viewModel.onEvent(CanvasViewModel.CanvasEvent.Undo) },
                    onRedo = { viewModel.onEvent(CanvasViewModel.CanvasEvent.Redo) },
                    onToggleEraser = { viewModel.onEvent(CanvasViewModel.CanvasEvent.ToggleEraser) }
                )
            }
        ) { paddingValues ->
            SharedCanvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = state,
                onDrawPath = { viewModel.onEvent(CanvasViewModel.CanvasEvent.DrawPath(it)) }
            )
        }
    }
}

@Composable
fun LeaveCoupleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave Couple") },
        text = { Text("Are you sure you want to leave this canvas?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasTopBar(
    state: CanvasState,
    onLeaveCouple: () -> Unit,
    onUpdateMood: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    TopAppBar(
        title = {
            state.coupleId?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Couple ID: $it", modifier = Modifier.padding(end = 8.dp))
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(it)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Couple ID")
                    }
                }
            }
        },
        actions = {
            MoodIndicator(
                mood = state.currentUser?.mood ?: "😊",
                onMoodSelected = onUpdateMood
            )
            IconButton(onClick = onLeaveCouple) {
                Icon(Icons.Filled.Close, contentDescription = "Leave Couple")
            }
        }
    )
}

@Composable
fun SharedCanvas(
    modifier: Modifier = Modifier,
    state: CanvasState,
    onDrawPath: (com.ourcanvas.data.model.DrawPath) -> Unit
) {
    val currentPath = remember { mutableStateListOf<PointF>() }

    Canvas(modifier = modifier
        .background(Color.LightGray)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    currentPath.clear()
                },
                onDrag = { change, _ ->
                    currentPath.add(PointF(change.position.x, change.position.y))
                    change.consume()
                },
                onDragEnd = {
                    state.currentUser?.uid?.let { userId ->
                        onDrawPath(
                            com.ourcanvas.data.model.DrawPath(
                                points = currentPath.toList(),
                                color = if (state.isEraserSelected) Color.White.value.toLong() else state.selectedColor,
                                strokeWidth = state.selectedStrokeWidth,
                                userId = userId
                            )
                        )
                    }
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
            color = Color(if (state.isEraserSelected) Color.White.value.toLong() else state.selectedColor),
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
    onStrokeWidthChanged: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleEraser: () -> Unit
) {
    val colors = listOf(0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00)

    Column(modifier = modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            colors.forEach { color ->
                Button(
                    onClick = { onColorSelected(color) },
                    modifier = Modifier
                        .size(40.dp)
                        .padding(2.dp)
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color(color)))
                }
            }
            IconButton(onClick = onToggleEraser) {
                Icon(Icons.Filled.Delete, contentDescription = "Eraser")
            }
            IconButton(onClick = onUndo) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Undo")
            }
            IconButton(onClick = onRedo) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Redo")
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


