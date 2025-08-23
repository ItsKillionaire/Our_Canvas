package com.ourcanvas.domain.usecase

import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.repository.CanvasRepository
import javax.inject.Inject

class SendDrawingPath @Inject constructor(
    private val repository: CanvasRepository
) {
    suspend operator fun invoke(canvasId: String, path: DrawPath): Result<Unit> =
        repository.sendDrawingPath(canvasId, path)
}