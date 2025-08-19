
package com.ourcanvas.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ourcanvas.domain.usecase.CreateCouple
import com.ourcanvas.domain.usecase.JoinCouple
import com.ourcanvas.domain.usecase.SignInAnonymously
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoupleViewModel @Inject constructor(
    private val createCouple: CreateCouple,
    private val joinCouple: JoinCouple,
    private val signInAnonymously: SignInAnonymously
) : ViewModel() {

    fun createCouple(navigate: () -> Unit) {
        viewModelScope.launch {
            val result = signInAnonymously()
            result.onSuccess { uid ->
                val createCoupleResult = createCouple(uid)
                createCoupleResult.onSuccess {
                    navigate()
                }.onFailure {
                    // Handle createCouple error
                }
            }.onFailure {
                // Handle signInAnonymously error
            }
        }
    }

    fun joinCouple(coupleId: String, navigate: () -> Unit) {
        viewModelScope.launch {
            val result = signInAnonymously()
            result.onSuccess { uid ->
                val joinCoupleResult = joinCouple(uid, coupleId)
                joinCoupleResult.onSuccess {
                    navigate()
                }.onFailure {
                    // Handle joinCouple error
                }
            }.onFailure {
                // Handle signInAnonymously error
            }
        }
    }
}
