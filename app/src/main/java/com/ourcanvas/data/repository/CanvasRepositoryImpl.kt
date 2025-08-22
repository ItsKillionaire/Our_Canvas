package com.ourcanvas.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.ourcanvas.data.model.DrawPath
import com.ourcanvas.data.model.TextObject
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
            val coupleRef = firestore.collection("couples").document()
            val coupleId = coupleRef.id
            withContext(Dispatchers.IO) {
                coupleRef.set(mapOf("users" to listOf(uid))).await()
                firestore.collection("users").document(uid).update("coupleId", coupleId).await()
            }
            Result.success(coupleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinCouple(uid: String, coupleId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val coupleRef = firestore.collection("couples").document(coupleId)
                coupleRef.update("users", FieldValue.arrayUnion(uid)).await()
                firestore.collection("users").document(uid).update("coupleId", coupleId).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
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
                        trySend(path).isSuccess
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
                pathsRef.push().setValue(path).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

   override fun getPartnerMood(uid: String, coupleId: String): Flow<UserProfile> = callbackFlow {
        val coupleRef = firestore.collection("couples").document(coupleId)
        var partnerListener: com.google.firebase.firestore.ListenerRegistration? = null

        val coupleListener = coupleRef.addSnapshotListener { coupleSnapshot, coupleError ->
            if (coupleError != null) {
                close(coupleError)
                return@addSnapshotListener
            }

            partnerListener?.remove() // remove previous listener

            if (coupleSnapshot != null && coupleSnapshot.exists()) {
                val users = (coupleSnapshot.get("users") as? List<*>)?.mapNotNull { it as? String }
                val partnerId = users?.firstOrNull { it != uid }

                if (partnerId != null) {
                    val partnerRef = firestore.collection("users").document(partnerId)
                    partnerListener = partnerRef.addSnapshotListener { partnerSnapshot, partnerError ->
                        if (partnerError != null) {
                            close(partnerError)
                            return@addSnapshotListener
                        }

                        if (partnerSnapshot != null && partnerSnapshot.exists()) {
                            val userProfile = partnerSnapshot.toObject(UserProfile::class.java)
                            if (userProfile != null) {
                                trySend(userProfile)
                            }
                        }
                    }
                } else {
                    trySend(UserProfile(mood = "…")).isSuccess
                }
            } else {
                trySend(UserProfile(mood = "…")).isSuccess
            }
        }

        awaitClose {
            coupleListener.remove()
            partnerListener?.remove()
        }
    }


    override fun getTextObjects(coupleId: String): Flow<List<TextObject>> = callbackFlow {
        val collectionRef = firestore.collection("canvases/$coupleId/texts")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val textObjects = snapshot.toObjects(TextObject::class.java)
                trySend(textObjects).isSuccess
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun addOrUpdateTextObject(coupleId: String, textObject: TextObject): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val docRef = firestore.collection("canvases/$coupleId/texts").document(textObject.id)
                docRef.set(textObject).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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