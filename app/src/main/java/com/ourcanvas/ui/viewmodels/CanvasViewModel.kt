
package com.ourcanvas.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ourcanvas.domain.usecase.CreateCanvas
import com.ourcanvas.domain.usecase.GetUserProfile
import com.ourcanvas.domain.usecase.JoinCanvas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CanvasScreenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val canvasId: String? = null
)

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val createCanvas: CreateCanvas,
    private val joinCanvas: JoinCanvas,
    private val getUserProfile: GetUserProfile,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _canvasId = MutableStateFlow<String?>(null)
    val canvasId: StateFlow<String?> = _canvasId

    private val _canvasScreenState = MutableStateFlow(CanvasScreenState())
    val canvasScreenState: StateFlow<CanvasScreenState> = _canvasScreenState

    val userProfile = getUserProfile(auth.currentUser?.uid ?: "").stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            userProfile.collect {
                _canvasId.value = it?.coupleId
            }
        }
    }

    fun createCanvas() {
        viewModelScope.launch {
            _canvasScreenState.value = CanvasScreenState(isLoading = true)
            auth.currentUser?.uid?.let {
                val result = createCanvas(it)
                if (result.isSuccess) {
                    _canvasScreenState.value = CanvasScreenState(canvasId = result.getOrNull())
                } else {
                    _canvasScreenState.value = CanvasScreenState(error = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun clearError() {
        _canvasScreenState.value = _canvasScreenState.value.copy(error = null)
    }

    fun joinCanvas(canvasId: String) {
        viewModelScope.launch {
            _canvasScreenState.value = CanvasScreenState(isLoading = true)
            if (auth.currentUser == null) {
                _canvasScreenState.value = CanvasScreenState(error = "User not authenticated")
                return@launch
            }
            auth.currentUser?.uid?.let {
                val result = joinCanvas(it, canvasId)
                if (result.isSuccess) {
                    _canvasScreenState.value = CanvasScreenState(canvasId = canvasId)
                } else {
                    _canvasScreenState.value = CanvasScreenState(error = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}
