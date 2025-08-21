package com.ourcanvas.domain.usecase

import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.repository.CanvasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDrawingPaths @Inject constructor(
    private val repository: CanvasRepository
) {
    operator fun invoke(coupleId: String): Flow<DrawPath> =
        repository.getDrawingPaths(coupleId)
}