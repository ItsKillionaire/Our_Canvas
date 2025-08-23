package com.ourcanvas.data.repository

import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface CanvasRepository {
    // AUTH
    suspend fun createUserProfile(uid: String): Result<Unit>
    suspend fun signInAnonymously(): Result<String>
    suspend fun signInWithGoogle(idToken: String): Result<String>

    // MOOD
    suspend fun createCanvas(uid: String): Result<String>
    suspend fun joinCanvas(uid: String, canvasId: String): Result<Unit>
    fun getUserProfile(uid: String): Flow<UserProfile?>
    suspend fun updateUserMood(uid: String, mood: String): Result<Unit>
    fun getPartnerMood(uid: String, canvasId: String): Flow<UserProfile>

    suspend fun leaveCanvas(uid: String): Result<Unit>

    // CANVAS - DRAWING (Realtime Database)
    fun getDrawingPaths(canvasId: String): Flow<DrawPath>
    suspend fun sendDrawingPath(canvasId: String, path: DrawPath): Result<Unit>

    
}
