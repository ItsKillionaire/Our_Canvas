package com.ourcanvas.data.repository

import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.model.TextObject
import com.ourcanvas.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface CanvasRepository {
    // AUTH
    suspend fun signInAnonymously(): Result<String>

    // MOOD
    suspend fun createCouple(uid: String): Result<String>
    suspend fun joinCouple(uid: String, coupleId: String): Result<Unit>
    fun getUserProfile(uid: String): Flow<UserProfile>
    suspend fun updateUserMood(uid: String, mood: String): Result<Unit>
    fun getPartnerMood(uid: String): Flow<UserProfile>

    // CANVAS - DRAWING (Realtime Database)
    fun getDrawingPaths(): Flow<DrawPath>
    suspend fun sendDrawingPath(path: DrawPath): Result<Unit>

    // CANVAS - TEXT (Firestore)
    fun getTextObjects(): Flow<List<TextObject>>
    suspend fun addOrUpdateTextObject(textObject: TextObject): Result<Unit>
}
