package com.ourcanvas.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.model.UserProfile
import com.ourcanvas.domain.usecase.GetDrawingPaths
import com.ourcanvas.domain.usecase.GetPartnerMood
import com.ourcanvas.domain.usecase.GetUserProfile
import com.ourcanvas.domain.usecase.LeaveCouple
import com.ourcanvas.domain.usecase.SendDrawingPath
import com.ourcanvas.domain.usecase.UpdateUserMood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val getUserProfile: GetUserProfile,
    private val getPartnerMood: GetPartnerMood,
    private val getDrawingPaths: GetDrawingPaths,
    private val sendDrawingPath: SendDrawingPath,
    private val updateUserMood: UpdateUserMood,
    private val leaveCouple: LeaveCouple
) : ViewModel() {

    private val _canvasState = MutableStateFlow(CanvasState())
    val canvasState: StateFlow<CanvasState> = _canvasState

    private val _navigateToCoupleScreen = MutableStateFlow(false)
    val navigateToCoupleScreen: StateFlow<Boolean> = _navigateToCoupleScreen

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            startObservers(uid)
        } else {
            _canvasState.value = _canvasState.value.copy(error = "User not signed in", isLoading = false)
        }
    }

    private fun startObservers(uid: String) {
        viewModelScope.launch {
            _canvasState.value = _canvasState.value.copy(isLoading = true)
            getUserProfile(uid).catch {
                _canvasState.value = _canvasState.value.copy(error = it.message, isLoading = false)
            }.collect { userProfile ->
                if (userProfile != null) {
                    _canvasState.value = _canvasState.value.copy(currentUser = userProfile, coupleId = userProfile.coupleId, isLoading = false)
                    val coupleId = userProfile.coupleId
                    if (coupleId != null) {
                        observePartnerMood(uid, coupleId)
                        observeDrawingPaths(coupleId)
                    }
                } else {
                    _canvasState.value = _canvasState.value.copy(error = "User profile not found", isLoading = false)
                }
            }
        }
    }

    private fun observePartnerMood(uid: String, coupleId: String) {
        viewModelScope.launch {
            getPartnerMood(uid, coupleId).catch {
                _canvasState.value = _canvasState.value.copy(error = it.message)
            }.collect {
                _canvasState.value = _canvasState.value.copy(partnerUser = it)
            }
        }
    }

    private fun observeDrawingPaths(coupleId: String) {
        viewModelScope.launch {
            getDrawingPaths(coupleId).catch {
                _canvasState.value = _canvasState.value.copy(error = it.message)
            }.collect { path ->
                val currentPaths = _canvasState.value.drawingPaths.toMutableList()
                val existingPathIndex = currentPaths.indexOfFirst { it.id == path.id }
                if (existingPathIndex != -1) {
                    // Do not update if the path is the same as the one in the undonePaths
                    if (_canvasState.value.undonePaths.any { it.id == path.id }) {
                        return@collect
                    }
                    currentPaths[existingPathIndex] = path
                } else {
                    currentPaths.add(path)
                }
                _canvasState.value = _canvasState.value.copy(drawingPaths = currentPaths)
            }
        }
    }

    fun onEvent(event: CanvasEvent) {
        viewModelScope.launch {
            when (event) {
                is CanvasEvent.DrawPath -> {
                    _canvasState.value.currentUser?.coupleId?.let {
                        sendDrawingPath(it, event.path)
                    }
                }
                is CanvasEvent.UpdateMood -> {
                    _canvasState.value.currentUser?.let {
                        updateUserMood(it.uid, event.mood)
                    }
                }
                is CanvasEvent.SelectColor -> {
                    _canvasState.value = _canvasState.value.copy(selectedColor = event.color, isEraserSelected = false)
                }
                is CanvasEvent.SelectStrokeWidth -> {
                    _canvasState.value = _canvasState.value.copy(selectedStrokeWidth = event.width)
                }
                is CanvasEvent.Undo -> {
                    if (_canvasState.value.drawingPaths.isNotEmpty()) {
                        val lastPath = _canvasState.value.drawingPaths.last()
                        val currentPaths = _canvasState.value.drawingPaths.toMutableList()
                        currentPaths.remove(lastPath)
                        val currentUndonePaths = _canvasState.value.undonePaths.toMutableList()
                        currentUndonePaths.add(lastPath)
                        _canvasState.value = _canvasState.value.copy(drawingPaths = currentPaths, undonePaths = currentUndonePaths)
                    }
                }
                is CanvasEvent.Redo -> {
                    if (_canvasState.value.undonePaths.isNotEmpty()) {
                        val lastUndonePath = _canvasState.value.undonePaths.last()
                        val currentUndonePaths = _canvasState.value.undonePaths.toMutableList()
                        currentUndonePaths.remove(lastUndonePath)
                        val currentPaths = _canvasState.value.drawingPaths.toMutableList()
                        currentPaths.add(lastUndonePath)
                        _canvasState.value = _canvasState.value.copy(drawingPaths = currentPaths, undonePaths = currentUndonePaths)
                    }
                }
                is CanvasEvent.ToggleEraser -> {
                    _canvasState.value = _canvasState.value.copy(isEraserSelected = !_canvasState.value.isEraserSelected)
                }
                is CanvasEvent.LeaveCouple -> {
                    _canvasState.value.currentUser?.let {
                        leaveCouple(it.uid).onSuccess {
                            _navigateToCoupleScreen.value = true
                        }
                    }
                }
            }
        }
    }

    data class CanvasState(
        val isLoading: Boolean = true,
        val currentUser: UserProfile? = null,
        val partnerUser: UserProfile? = null,
        val drawingPaths: List<DrawPath> = emptyList(),
        val undonePaths: List<DrawPath> = emptyList(),
        val redonePaths: List<DrawPath> = emptyList(),
        val error: String? = null,
        val selectedColor: Long = 0xFF000000,
        val selectedStrokeWidth: Float = 8f,
        val isEraserSelected: Boolean = false,
        val coupleId: String? = null
    )

    sealed class CanvasEvent {
        data class DrawPath(val path: com.ourcanvas.data.model.DrawPath) : CanvasEvent()
        data class UpdateMood(val mood: String) : CanvasEvent()
        data class SelectColor(val color: Long) : CanvasEvent()
        data class SelectStrokeWidth(val width: Float) : CanvasEvent()
        object Undo : CanvasEvent()
        object Redo : CanvasEvent()
        object ToggleEraser : CanvasEvent()
        object LeaveCouple : CanvasEvent()
    }

    fun onNavigationDone() {
        _navigateToCoupleScreen.value = false
    }
}
