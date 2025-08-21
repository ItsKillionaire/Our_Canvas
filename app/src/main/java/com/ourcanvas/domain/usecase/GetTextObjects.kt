package com.ourcanvas.domain.usecase

import com.ourcanvas.data.model.TextObject
import com.ourcanvas.data.repository.CanvasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTextObjects @Inject constructor(
    private val repository: CanvasRepository
) {
    operator fun invoke(coupleId: String): Flow<List<TextObject>> =
        repository.getTextObjects(coupleId)
}