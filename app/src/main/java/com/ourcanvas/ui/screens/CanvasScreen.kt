package com.ourcanvas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.* 
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ourcanvas.ui.viewmodels.CanvasViewModel

@Composable
fun CanvasScreen(
    navController: NavController,
    viewModel: CanvasViewModel = hiltViewModel()
) {
    val canvasScreenState by viewModel.canvasScreenState.collectAsState()

    if (canvasScreenState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    canvasScreenState.error?.let {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Error") },
            text = { Text(it) },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    LaunchedEffect(canvasScreenState.canvasId) {
        if (canvasScreenState.canvasId != null) {
            navController.navigate("canvas") {
                popUpTo("canvas") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CanvasScreenContent(
            onCreateCanvas = { viewModel.createCanvas() },
            onJoinCanvas = { viewModel.joinCanvas(it) }
        )
    }
}

@Composable
fun CanvasScreenContent(
    onCreateCanvas: () -> Unit,
    onJoinCanvas: (String) -> Unit
) {
    var canvasId by remember { mutableStateOf("") }
    var showJoinForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onCreateCanvas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Create Canvas")
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showJoinForm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !showJoinForm
                ) {
                    Text("Join Canvas")
                }
            }
        }

        AnimatedVisibility(visible = showJoinForm) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    OutlinedTextField(
                        value = canvasId,
                        onValueChange = { canvasId = it },
                        label = { Text("Canvas ID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onJoinCanvas(canvasId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = canvasId.isNotBlank(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Join")
                    }
                }
            }
        }
    }
}