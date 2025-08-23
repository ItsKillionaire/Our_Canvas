package com.ourcanvas.ui.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.ourcanvas.domain.usecase.SignInAnonymously
import com.ourcanvas.domain.usecase.SignInWithGoogle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInAnonymously: SignInAnonymously,
    private val signInWithGoogle: SignInWithGoogle,
    private val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun signInAnonymously(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = signInAnonymously()
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun handleSignInResult(resultCode: Int, data: Intent?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val result = signInWithGoogle(idToken)
                    if (result.isSuccess) {
                        onSuccess()
                    }
                }
            } catch (e: ApiException) {
                // Handle error
            }
        }
    }
}
