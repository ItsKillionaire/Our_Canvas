package com.ourcanvas.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourcanvas.domain.usecase.CreateUserProfile
import com.ourcanvas.domain.usecase.GetUserProfile
import com.ourcanvas.domain.usecase.SignInAnonymously
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val signInAnonymously: SignInAnonymously,
    private val getUserProfile: GetUserProfile,
    private val createUserProfile: CreateUserProfile
) : ViewModel() {

    private val _destination = MutableStateFlow<String?>(null)
    val destination: StateFlow<String?> = _destination

    init {
        android.util.Log.d("SplashViewModel", "init: ViewModel created")
        checkUserState()
    }

    private fun checkUserState() {
        viewModelScope.launch {
            android.util.Log.d("SplashViewModel", "checkUserState: Starting anonymous sign-in")
            val result = withTimeoutOrNull(10000) { signInAnonymously() }
            android.util.Log.d("SplashViewModel", "checkUserState: Sign-in result: $result")
            if (result == null) {
                android.util.Log.d("SplashViewModel", "checkUserState: Sign-in timed out")
                _destination.value = "couple"
                return@launch
            }
            result.onSuccess { uid ->
                android.util.Log.d("SplashViewModel", "checkUserState: Sign-in successful, UID: $uid")
                android.util.Log.d("SplashViewModel", "checkUserState: Getting user profile...")
                val userProfile = getUserProfile(uid).firstOrNull()
                android.util.Log.d("SplashViewModel", "checkUserState: User profile: $userProfile")
                if (userProfile == null) {
                    android.util.Log.d("SplashViewModel", "checkUserState: User profile is null, creating new profile")
                    val createProfileResult = createUserProfile(uid)
                    android.util.Log.d("SplashViewModel", "checkUserState: Create profile result: $createProfileResult")
                    createProfileResult.onSuccess {
                        android.util.Log.d("SplashViewModel", "checkUserState: Profile creation successful, navigating to couple screen")
                        _destination.value = "couple"
                    }.onFailure { e ->
                        android.util.Log.d("SplashViewModel", "checkUserState: Profile creation failed: ${e.message}")
                        _destination.value = "couple"
                    }
                } else {
                    if (userProfile.coupleId != null) {
                        android.util.Log.d("SplashViewModel", "checkUserState: User has couple ID, navigating to canvas screen")
                        _destination.value = "canvas"
                    } else {
                        android.util.Log.d("SplashViewModel", "checkUserState: User has no couple ID, navigating to couple screen")
                        _destination.value = "couple"
                    }
                }
            }.onFailure {
                android.util.Log.d("SplashViewModel", "checkUserState: Sign-in failed: ${it.message}")
                _destination.value = "couple"
            }
        }
    }
}
