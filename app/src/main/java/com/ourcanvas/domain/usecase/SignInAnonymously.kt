package com.ourcanvas.domain.usecase

import com.ourcanvas.data.repository.CanvasRepository
import javax.inject.Inject

class SignInAnonymously @Inject constructor(
    private val repository: CanvasRepository
) {
    suspend operator fun invoke(): Result<String> =
        repository.signInAnonymously()
}