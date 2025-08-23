package com.ourcanvas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.ourcanvas.ui.viewmodels.CoupleViewModel

@Composable
fun CoupleScreen(
    navController: NavController,
    viewModel: CoupleViewModel = hiltViewModel()
) {
    val coupleScreenState by viewModel.coupleScreenState.collectAsState()

    if (coupleScreenState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CoupleScreenContent(
            onCreateCouple = { viewModel.createCouple() },
            onJoinCouple = { viewModel.joinCouple(it) }
        )
    }
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
                    onClick = onCreateCouple,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Create Couple")
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
                    Text("Join Couple")
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
                        value = coupleId,
                        onValueChange = { coupleId = it },
                        label = { Text("Couple ID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onJoinCouple(coupleId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = coupleId.isNotBlank(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Join")
                    }
                }
            }
        }
    }
}