package com.ourcanvas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ourcanvas.ui.viewmodels.CoupleViewModel

@Composable
fun CoupleScreen(
    navController: NavController,
    viewModel: CoupleViewModel = hiltViewModel()
) {
    var coupleId by remember { mutableStateOf("") }

    CoupleScreenContent(
        coupleId = coupleId,
        onCoupleIdChange = { coupleId = it },
        onJoinCouple = { viewModel.joinCouple(coupleId) { navController.navigate("canvas") } },
        onCreateCouple = { viewModel.createCouple { navController.navigate("canvas") } }
    )
}

@Composable
fun CoupleScreenContent(
    coupleId: String,
    onCoupleIdChange: (String) -> Unit,
    onJoinCouple: () -> Unit,
    onCreateCouple: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = coupleId,
            onValueChange = onCoupleIdChange,
            label = { Text("Couple ID") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onJoinCouple,
            enabled = coupleId.isNotEmpty()
        ) {
            Text("Join Couple")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onCreateCouple) {
            Text("Create Couple")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoupleScreenPreview() {
    CoupleScreenContent(
        coupleId = "123456",
        onCoupleIdChange = {},
        onJoinCouple = {},
        onCreateCouple = {}
    )
}