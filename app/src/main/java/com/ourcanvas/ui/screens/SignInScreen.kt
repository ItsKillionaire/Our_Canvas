package com.ourcanvas.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ourcanvas.ui.viewmodels.SignInViewModel

@Composable
fun SignInScreen(
    onSignInAnonymously: () -> Unit,
    onSignInSuccess: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.handleSignInResult(result.resultCode, onSignInSuccess)
    }

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
                Text(
                    text = "Welcome to OurCanvas",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.signInAnonymously(onSignInAnonymously) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sign In Anonymously")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch(viewModel.getSignInIntent()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sign In with Google")
                }
            }
        }
    }
}
