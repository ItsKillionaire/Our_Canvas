package com.ourcanvas.domain.usecase

import com.ourcanvas.data.repository.CanvasRepository
import javax.inject.Inject

class LeaveCanvas @Inject constructor(
    private val repository: CanvasRepository
) {
    suspend operator fun invoke(uid: String) = repository.leaveCanvas(uid)
}