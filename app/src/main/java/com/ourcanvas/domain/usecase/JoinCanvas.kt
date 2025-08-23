package com.ourcanvas.domain.usecase

import com.ourcanvas.data.repository.CanvasRepository
import javax.inject.Inject

class JoinCanvas @Inject constructor(
    private val repository: CanvasRepository
) {
    suspend operator fun invoke(uid: String, canvasId: String): Result<Unit> {
        return repository.joinCanvas(uid, canvasId)
    }
}