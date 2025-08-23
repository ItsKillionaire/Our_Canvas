package com.ourcanvas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ourcanvas.ui.viewmodels.CoupleViewModel

@Composable
fun CoupleScreen(
    navController: NavController,
    viewModel: CoupleViewModel = hiltViewModel()
) {
    val coupleScreenState by viewModel.coupleScreenState.collectAsState()

    if (coupleScreenState.isLoading) {
        CircularProgressIndicator()
    }

    coupleScreenState.error?.let {
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

    LaunchedEffect(coupleScreenState.coupleId) {
        if (coupleScreenState.coupleId != null) {
            navController.navigate("canvas") {
                popUpTo("couple") { inclusive = true }
            }
        }
    }
    var showJoinDialog by remember { mutableStateOf(false) }
    var coupleId by remember { mutableStateOf("") }

    

    CoupleScreenContent(
        onCreateCouple = { viewModel.createCouple() },
        onShowJoinDialog = { showJoinDialog = true }
    )

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Couple") },
            text = {
                OutlinedTextField(
                    value = coupleId,
                    onValueChange = { coupleId = it },
                    label = { Text("Couple ID") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.joinCouple(coupleId)
                        showJoinDialog = false
                    }
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                Button(onClick = { showJoinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CoupleScreenContent(
    onCreateCouple: () -> Unit,
    onShowJoinDialog: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onCreateCouple) {
            Text("Create Couple")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onShowJoinDialog) {
            Text("Join Couple")
        }
    }
}