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

    override suspend fun createCouple(uid: String): Result<String> {
        return try {
            Log.d("CanvasRepositoryImpl", "Creating couple for user: $uid")
            val coupleRef = firestore.collection("couples").document()
            val coupleId = coupleRef.id
            Log.d("CanvasRepositoryImpl", "Couple ID: $coupleId")
            withContext(Dispatchers.IO) {
                coupleRef.set(mapOf("users" to listOf(uid))).await()
                Log.d("CanvasRepositoryImpl", "Couple document created in Firestore")
                firestore.collection("users").document(uid).update("coupleId", coupleId).await()
                Log.d("CanvasRepositoryImpl", "User document updated in Firestore")
                db.getReference("canvases/$coupleId/users").setValue(mapOf(uid to true)).await()
                Log.d("CanvasRepositoryImpl", "Users list created in Realtime Database")
            }
            Result.success(coupleId)
        } catch (e: Exception) {
            Log.e("CanvasRepositoryImpl", "Error creating couple: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun joinCouple(uid: String, coupleId: String): Result<Unit> {
        return try {
            Log.d("CanvasRepositoryImpl", "Joining couple for user: $uid, coupleId: $coupleId")
            withContext(Dispatchers.IO) {
                val coupleRef = firestore.collection("couples").document(coupleId)
                val coupleDoc = coupleRef.get().await()
                val users = coupleDoc.get("users") as? List<*>
                val otherUser = users?.firstOrNull { it != uid } as? String

                coupleRef.update("users", FieldValue.arrayUnion(uid)).await()
                Log.d("CanvasRepositoryImpl", "Couple document updated in Firestore")
                firestore.collection("users").document(uid).update("coupleId", coupleId).await()
                Log.d("CanvasRepositoryImpl", "User document updated in Firestore")
                if (otherUser != null) {
                    firestore.collection("users").document(otherUser).update("coupleId", coupleId).await()
                    Log.d("CanvasRepositoryImpl", "Other user document updated in Firestore")
                }
                val usersMap = users?.associate { it as String to true }?.toMutableMap() ?: mutableMapOf()
                usersMap[uid] = true
                db.getReference("canvases/$coupleId/users").setValue(usersMap).await()
                Log.d("CanvasRepositoryImpl", "Users list updated in Realtime Database")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CanvasRepositoryImpl", "Error joining couple: ${e.message}")
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
                    val coupleRef = firestore.collection("couples").document(it)
                    coupleRef.update("moods.$uid", mood).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDrawingPaths(coupleId: String): Flow<DrawPath> = callbackFlow {
        val pathsRef = db.getReference("canvases/$coupleId/drawing_paths")

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

    override suspend fun sendDrawingPath(coupleId: String, path: DrawPath): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val pathsRef = db.getReference("canvases/$coupleId/drawing_paths")
                pathsRef.push().setValue(path.copy(id = "")).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

   override fun getPartnerMood(uid: String, coupleId: String): Flow<UserProfile> = callbackFlow {
        val coupleRef = firestore.collection("couples").document(coupleId)

        val listener = coupleRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val moods = snapshot.get("moods") as? Map<String, String>
                val partnerId = (snapshot.get("users") as? List<*>)?.mapNotNull { it as? String }?.firstOrNull { it != uid }
                val partnerMood = moods?.get(partnerId) ?: "…"
                trySend(UserProfile(mood = partnerMood))
            } else {
                trySend(UserProfile(mood = "…")).isSuccess
            }
        }

        awaitClose { listener.remove() }
    }


    

    override suspend fun leaveCouple(uid: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                firestore.collection("users").document(uid).update("coupleId", null).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}