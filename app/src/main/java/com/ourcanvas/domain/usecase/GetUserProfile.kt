package com.ourcanvas.domain.usecase

import com.ourcanvas.data.model.UserProfile
import com.ourcanvas.data.repository.CanvasRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserProfile @Inject constructor(
    private val repository: CanvasRepository
) {
    operator fun invoke(uid: String): Flow<UserProfile> =
        repository.getUserProfile(uid)
}