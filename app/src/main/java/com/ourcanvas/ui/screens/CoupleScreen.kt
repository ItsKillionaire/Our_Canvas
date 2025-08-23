package com.ourcanvas.ui.screens

import androidx.compose.animation.AnimatedVisibility
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

    CoupleScreenContent(
        onCreateCouple = { viewModel.createCouple() },
        onJoinCouple = { viewModel.joinCouple(it) }
    )
}

@Composable
fun CoupleScreenContent(
    onCreateCouple: () -> Unit,
    onJoinCouple: (String) -> Unit
) {
    var coupleId by remember { mutableStateOf("") }
    var showJoinForm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onCreateCouple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Couple")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showJoinForm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !showJoinForm
                ) {
                    Text("Join Couple")
                }
            }
        }

        AnimatedVisibility(visible = showJoinForm) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = coupleId,
                        onValueChange = { coupleId = it },
                        label = { Text("Couple ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onJoinCouple(coupleId) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = coupleId.isNotBlank()
                    ) {
                        Text("Join")
                    }
                }
            }
        }
    }
}