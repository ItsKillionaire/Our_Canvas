package com.ourcanvas.domain.usecase

import com.ourcanvas.data.repository.CanvasRepository
import javax.inject.Inject

class UpdateUserMood @Inject constructor(
    private val repository: CanvasRepository
) {
    suspend operator fun invoke(uid: String, mood: String): Result<Unit> {
        return repository.updateUserMood(uid, mood)
    }
}