
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoupleScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val coupleId: String? = null
)

@HiltViewModel
class CoupleViewModel @Inject constructor(
    private val createCouple: CreateCouple,
    private val joinCouple: JoinCouple,
    private val getUserProfile: GetUserProfile,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _coupleId = MutableStateFlow<String?>(null)
    val coupleId: StateFlow<String?> = _coupleId

    private val _coupleScreenState = MutableStateFlow(CoupleScreenState())
    val coupleScreenState: StateFlow<CoupleScreenState> = _coupleScreenState

    val userProfile = getUserProfile(auth.currentUser?.uid ?: "").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            userProfile.collect {
                _coupleId.value = it?.coupleId
            }
        }
    }

    fun createCouple() {
        viewModelScope.launch {
            _coupleScreenState.value = CoupleScreenState(isLoading = true)
            auth.currentUser?.uid?.let {
                val result = createCouple(it)
                if (result.isSuccess) {
                    _coupleScreenState.value = CoupleScreenState(coupleId = result.getOrNull())
                } else {
                    _coupleScreenState.value = CoupleScreenState(error = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun clearError() {
        _coupleScreenState.value = _coupleScreenState.value.copy(error = null)
    }

    fun joinCouple(coupleId: String) {
        viewModelScope.launch {
            _coupleScreenState.value = CoupleScreenState(isLoading = true)
            if (auth.currentUser == null) {
                _coupleScreenState.value = CoupleScreenState(error = "User not authenticated")
                return@launch
            }
            auth.currentUser?.uid?.let {
                val result = joinCouple(it, coupleId)
                if (result.isSuccess) {
                    _coupleScreenState.value = CoupleScreenState(coupleId = coupleId)
                } else {
                    _coupleScreenState.value = CoupleScreenState(error = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}
