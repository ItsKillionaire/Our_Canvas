
package com.ourcanvas.ui.viewmodels

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ourcanvas.domain.usecase.CreateCouple
import com.ourcanvas.domain.usecase.GetUserProfile
import com.ourcanvas.domain.usecase.JoinCouple
import com.ourcanvas.domain.usecase.LeaveCouple
import com.ourcanvas.domain.usecase.SignInAnonymously
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoupleViewModel @Inject constructor(
    private val createCouple: CreateCouple,
    private val joinCouple: JoinCouple,
    private val getUserProfile: GetUserProfile,
    private val auth: FirebaseAuth
) : ViewModel() {

    val userProfile = getUserProfile(auth.currentUser?.uid ?: "").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun createCouple() {
        viewModelScope.launch {
            auth.currentUser?.uid?.let {
                val result = createCouple(it)
                if (result.isFailure) {
                    Log.e("CoupleViewModel", "Error creating couple: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun joinCouple(coupleId: String) {
        viewModelScope.launch {
            auth.currentUser?.uid?.let {
                val result = joinCouple(it, coupleId)
                if (result.isFailure) {
                    Log.e("CoupleViewModel", "Error joining couple: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
}
