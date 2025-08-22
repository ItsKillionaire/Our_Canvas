package com.ourcanvas.domain.usecase

import com.ourcanvas.data.repository.CanvasRepository
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(
    private val repository: CanvasRepository
) {
    suspend operator fun invoke(idToken: String) = repository.signInWithGoogle(idToken)
}