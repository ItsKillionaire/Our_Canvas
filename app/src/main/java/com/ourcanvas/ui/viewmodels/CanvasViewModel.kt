package com.ourcanvas.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.model.TextObject
import com.ourcanvas.data.model.UserProfile
import com.ourcanvas.domain.usecase.AddOrUpdateTextObject
import com.ourcanvas.domain.usecase.GetDrawingPaths
import com.ourcanvas.domain.usecase.GetPartnerMood
import com.ourcanvas.domain.usecase.GetTextObjects
import com.ourcanvas.domain.usecase.GetUserProfile
import com.ourcanvas.domain.usecase.SendDrawingPath
import com.ourcanvas.domain.usecase.SignInAnonymously
import com.ourcanvas.domain.usecase.UpdateUserMood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CanvasViewModel @Inject constructor(
    private val signInAnonymously: SignInAnonymously,
    private val getUserProfile: GetUserProfile,
    private val getPartnerMood: GetPartnerMood,
    private val getDrawingPaths: GetDrawingPaths,
    private val getTextObjects: GetTextObjects,
    private val sendDrawingPath: SendDrawingPath,
    private val updateUserMood: UpdateUserMood,
    private val addOrUpdateTextObject: AddOrUpdateTextObject
) : ViewModel() {

    private val _canvasState = MutableStateFlow(CanvasState())
    val canvasState: StateFlow<CanvasState> = _canvasState

    init {
        signIn()
    }

    private fun signIn() {
        viewModelScope.launch {
            _canvasState.value = _canvasState.value.copy(isLoading = true)
            val result = signInAnonymously()
            result.fold(
                onSuccess = {
                    val uid = it
                    _canvasState.value = _canvasState.value.copy(currentUser = UserProfile(uid = uid))
                    startObservers(uid)
                },
                onFailure = {
                    _canvasState.value = _canvasState.value.copy(error = it.message, isLoading = false)
                }
            )
        }
    }

    private fun startObservers(uid: String) {
        viewModelScope.launch {
            getUserProfile(uid).catch {
                _canvasState.value = _canvasState.value.copy(error = it.message)
            }.collect {
                _canvasState.value = _canvasState.value.copy(currentUser = it)
                if (it.coupleId != null) {
                    observePartnerMood(uid)
                    observeDrawingPaths()
                    observeTextObjects()
                }
                _canvasState.value = _canvasState.value.copy(isLoading = false)
            }
        }
    }

    private fun observePartnerMood(uid: String) {
        viewModelScope.launch {
            getPartnerMood(uid).catch {
                _canvasState.value = _canvasState.value.copy(error = it.message)
            }.collect {
                _canvasState.value = _canvasState.value.copy(partnerUser = it)
            }
        }
    }

    private fun observeDrawingPaths() {
        viewModelScope.launch {
            getDrawingPaths().catch {
                _canvasState.value = _canvasState.value.copy(error = it.message)
            }.collect {
                val currentPaths = _canvasState.value.drawingPaths.toMutableList()
                currentPaths.add(it)
                _canvasState.value = _canvasState.value.copy(drawingPaths = currentPaths)
            }
        }
    }

    private fun observeTextObjects() {
        viewModelScope.launch {
            getTextObjects().catch {
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
                    sendDrawingPath(event.path)
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
                    addOrUpdateTextObject(event.text)
                }
                is CanvasEvent.ToggleTextField -> {
                    _canvasState.value = _canvasState.value.copy(
                        showTextField = event.textObject != null,
                        currentTextObject = event.textObject
                    )
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
    }
}