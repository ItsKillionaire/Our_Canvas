package com.ourcanvas.ui.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.ourcanvas.domain.usecase.SignInAnonymously
import com.ourcanvas.domain.usecase.SignInWithGoogle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInAnonymously: SignInAnonymously,
    private val signInWithGoogle: SignInWithGoogle
) : ViewModel() {

    fun getSignInIntent(): Intent {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        return AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
    }

    fun signInAnonymously(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = signInAnonymously()
            if (result.isSuccess) {
                onSuccess()
            }
        }
    }

    fun handleSignInResult(resultCode: Int, onSuccess: () -> Unit) {
        if (resultCode == -1) { // RESULT_OK
            onSuccess()
        }
    }
}
