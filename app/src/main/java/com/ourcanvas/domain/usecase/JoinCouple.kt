package com.ourcanvas.domain.usecase

import com.ourcanvas.data.repository.CanvasRepository
import javax.inject.Inject

class JoinCouple @Inject constructor(
    private val repository: CanvasRepository
) {
    suspend operator fun invoke(uid: String, coupleId: String): Result<Unit> {
        return repository.joinCouple(uid, coupleId)
    }
}