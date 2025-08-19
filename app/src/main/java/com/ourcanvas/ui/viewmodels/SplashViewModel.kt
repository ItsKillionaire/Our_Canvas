
package com.ourcanvas.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourcanvas.domain.usecase.GetUserProfile
import com.ourcanvas.domain.usecase.SignInAnonymously
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val signInAnonymously: SignInAnonymously,
    private val getUserProfile: GetUserProfile
) : ViewModel() {

    fun checkUserState(navigate: (String) -> Unit) {
        viewModelScope.launch {
            val result = signInAnonymously()
            result.onSuccess {
                viewModelScope.launch {
                    val userProfile = getUserProfile(it).first()
                    if (userProfile.coupleId != null) {
                        navigate("canvas")
                    } else {
                        navigate("couple")
                    }
                }
            }.onFailure {
                navigate("couple")
            }
        }
    }
}
