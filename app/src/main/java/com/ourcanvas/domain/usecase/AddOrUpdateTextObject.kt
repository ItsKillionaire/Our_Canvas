package com.ourcanvas.domain.usecase

import com.ourcanvas.data.model.TextObject
import com.ourcanvas.data.repository.CanvasRepository
import javax.inject.Inject

class AddOrUpdateTextObject @Inject constructor(
    private val repository: CanvasRepository
) {
    suspend operator fun invoke(textObject: TextObject): Result<Unit> =
        repository.addOrUpdateTextObject(textObject)
}