package com.ourcanvas.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class CanvasRepositoryImpl(
    private val auth: FirebaseAuth,
    private val db: FirebaseDatabase,
    private val firestore: FirebaseFirestore
) : CanvasRepository {

    override suspend fun createUserProfile(uid: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).set(UserProfile(uid = uid)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<String> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Result.success(currentUser.uid)
            } else {
                val result = auth.signInAnonymously().await()
                val uid = result.user?.uid
                if (uid != null) {
                    Result.success(uid)
                } else {
                    Result.failure(Exception("Unknown error occurred during sign-in."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<String> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid
            if (uid != null) {
                Result.success(uid)
            } else {
                Result.failure(Exception("Unknown error occurred during Google sign-in."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createCanvas(uid: String): Result<String> {
        return try {
            Log.d("CanvasRepositoryImpl", "[createCanvas] Creating canvas for user: $uid")
            val canvasRef = firestore.collection("canvases").document()
            val canvasId = canvasRef.id
            Log.d("CanvasRepositoryImpl", "[createCanvas] Generated Canvas ID: $canvasId")
            withContext(Dispatchers.IO) {
                canvasRef.set(mapOf("users" to listOf(uid))).await()
                Log.d("CanvasRepositoryImpl", "[createCanvas] Canvas document created in Firestore")
                firestore.collection("users").document(uid).update("canvasId", canvasId).await()
                Log.d("CanvasRepositoryImpl", "[createCanvas] User document updated in Firestore")
                db.getReference("canvases/$canvasId/users").setValue(mapOf(uid to true)).await()
                Log.d("CanvasRepositoryImpl", "[createCanvas] Users list created in Realtime Database")
            }
            Log.d("CanvasRepositoryImpl", "[createCanvas] Canvas creation successful")
            Result.success(canvasId)
        } catch (e: Exception) {
            Log.e("CanvasRepositoryImpl", "[createCanvas] Error creating canvas: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun joinCanvas(uid: String, canvasId: String): Result<Unit> {
        return try {
            Log.d("CanvasRepositoryImpl", "[joinCanvas] Joining canvas for user: $uid, canvasId: $canvasId")
            withContext(Dispatchers.IO) {
                val canvasRef = firestore.collection("canvases").document(canvasId)
                Log.d("CanvasRepositoryImpl", "[joinCanvas] Getting canvas document")
                val canvasDoc = canvasRef.get().await()

                if (!canvasDoc.exists()) {
                    Log.e("CanvasRepositoryImpl", "[joinCanvas] Canvas with ID $canvasId not found.")
                    throw Exception("Canvas with ID $canvasId not found.")
                }

                Log.d("CanvasRepositoryImpl", "[joinCanvas] Canvas document exists")
                val users = canvasDoc.get("users") as? List<*> 
                val otherUser = users?.firstOrNull { it != uid } as? String

                Log.d("CanvasRepositoryImpl", "[joinCanvas] Updating canvas document with new user")
                canvasRef.update("users", FieldValue.arrayUnion(uid)).await()
                Log.d("CanvasRepositoryImpl", "[joinCanvas] Canvas document updated in Firestore")
                firestore.collection("users").document(uid).update("canvasId", canvasId).await()
                Log.d("CanvasRepositoryImpl", "[joinCanvas] User document updated in Firestore")
                if (otherUser != null) {
                    Log.d("CanvasRepositoryImpl", "[joinCanvas] Updating other user\'s document")
                    firestore.collection("users").document(otherUser).update("canvasId", canvasId).await()
                    Log.d("CanvasRepositoryImpl", "[joinCanvas] Other user document updated in Firestore")
                }
                val usersMap = users?.associate { it as String to true }?.toMutableMap() ?: mutableMapOf()
                usersMap[uid] = true
                Log.d("CanvasRepositoryImpl", "[joinCanvas] Updating users list in Realtime Database")
                db.getReference("canvases/$canvasId/users").setValue(usersMap).await()
                Log.d("CanvasRepositoryImpl", "[joinCanvas] Users list updated in Realtime Database")
            }
            Log.d("CanvasRepositoryImpl", "[joinCanvas] Join canvas successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CanvasRepositoryImpl", "[joinCanvas] Error joining canvas: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val docRef = firestore.collection("users").document(uid)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val userProfile = snapshot.toObject(UserProfile::class.java)
                trySend(userProfile).isSuccess
            } else {
                trySend(null).isSuccess
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun updateUserMood(uid: String, mood: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val userRef = firestore.collection("users").document(uid)
                userRef.update("mood", mood).await()

                val userProfile = userRef.get().await().toObject(UserProfile::class.java)
                userProfile?.coupleId?.let {
                    val canvasRef = firestore.collection("canvases").document(it)
                    canvasRef.update("moods.$uid", mood).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDrawingPaths(canvasId: String): Flow<DrawPath> = callbackFlow {
        val pathsRef = db.getReference("canvases/$canvasId/drawing_paths")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val path = it.getValue(DrawPath::class.java)
                    if (path != null) {
                        val pathWithId = path.copy(id = it.key!!)
                        trySend(pathWithId).isSuccess
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        pathsRef.addValueEventListener(listener)
        awaitClose { pathsRef.removeEventListener(listener) }
    }

    override suspend fun sendDrawingPath(canvasId: String, path: DrawPath): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val pathsRef = db.getReference("canvases/$canvasId/drawing_paths")
                pathsRef.push().setValue(path.copy(id = "")).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

   override fun getPartnerMood(uid: String, coupleId: String): Flow<UserProfile> = callbackFlow {
        val canvasRef = firestore.collection("canvases").document(coupleId)

        val listener = canvasRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val moods = snapshot.get("moods") as? Map<*, *> 
                moods?.let { 
                    val partnerId = (snapshot.get("users") as? List<*>)?.mapNotNull { it as? String }?.firstOrNull { it != uid }
                    val partnerMood = it[partnerId] as? String ?: "…"
                    trySend(UserProfile(mood = partnerMood))
                }
            } else {
                trySend(UserProfile(mood = "…")).isSuccess
            }
        }

        awaitClose { listener.remove() }
    }


    

    override suspend fun leaveCanvas(uid: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val userProfile = firestore.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
                userProfile?.coupleId?.let {
                    val canvasRef = firestore.collection("canvases").document(it)
                    canvasRef.update("users", FieldValue.arrayRemove(uid)).await()
                }
                firestore.collection("users").document(uid).update("canvasId", null).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
