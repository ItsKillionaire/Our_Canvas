package com.ourcanvas.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.model.TextObject
import com.ourcanvas.data.model.UserProfile
import com.ourcanvas.domain.usecase.AddOrUpdateTextObject
import com.ourcanvas.domain.usecase.GetDrawingPaths
import com.ourcanvas.domain.usecase.GetPartnerMood
import com.ourcanvas.domain.usecase.GetTextObjects
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
    private val getTextObjects: GetTextObjects,
    private val sendDrawingPath: SendDrawingPath,
    private val updateUserMood: UpdateUserMood,
    private val addOrUpdateTextObject: AddOrUpdateTextObject,
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
                    _canvasState.value = _canvasState.value.copy(currentUser = userProfile, isLoading = false)
                    val coupleId = userProfile.coupleId
                    if (coupleId != null) {
                        observePartnerMood(uid, coupleId)
                        observeDrawingPaths(coupleId)
                        observeTextObjects(coupleId)
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
            }.collect {
                val currentPaths = _canvasState.value.drawingPaths.toMutableList()
                currentPaths.add(it)
                _canvasState.value = _canvasState.value.copy(drawingPaths = currentPaths)
            }
        }
    }

    private fun observeTextObjects(coupleId: String) {
        viewModelScope.launch {
            getTextObjects(coupleId).catch {
                _canvasState.value = _canvasState.value.copy(error = it.message)
            }.collect {
                _canvasState.value = _canvasState.value.copy(textObjects = it)
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
                    _canvasState.value = _canvasState.value.copy(selectedColor = event.color)
                }
                is CanvasEvent.SelectStrokeWidth -> {
                    _canvasState.value = _canvasState.value.copy(selectedStrokeWidth = event.width)
                }
                is CanvasEvent.AddOrUpdateText -> {
                    _canvasState.value.currentUser?.coupleId?.let {
                        addOrUpdateTextObject(it, event.text)
                    }
                }
                is CanvasEvent.ToggleTextField -> {
                    _canvasState.value = _canvasState.value.copy(
                        showTextField = event.textObject != null,
                        currentTextObject = event.textObject
                    )
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
        val textObjects: List<TextObject> = emptyList(),
        val error: String? = null,
        val selectedColor: Long = 0xFF000000,
        val selectedStrokeWidth: Float = 8f,
        val showTextField: Boolean = false,
        val currentTextObject: TextObject? = null
    )

    sealed class CanvasEvent {
        data class DrawPath(val path: com.ourcanvas.data.model.DrawPath) : CanvasEvent()
        data class UpdateMood(val mood: String) : CanvasEvent()
        data class SelectColor(val color: Long) : CanvasEvent()
        data class SelectStrokeWidth(val width: Float) : CanvasEvent()
        data class AddOrUpdateText(val text: TextObject) : CanvasEvent()
        data class ToggleTextField(val textObject: TextObject? = null) : CanvasEvent()
        object LeaveCouple : CanvasEvent()
    }

    fun onNavigationDone() {
        _navigateToCoupleScreen.value = false
    }
}
