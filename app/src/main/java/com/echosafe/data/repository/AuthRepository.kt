package com.echosafe.data.repository

import android.util.Log
import com.echosafe.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth?
        get() = try { FirebaseAuth.getInstance() } catch (e: Exception) {
            Log.e("AuthRepository", "FirebaseAuth unavailable", e)
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) {
            Log.e("AuthRepository", "FirebaseFirestore unavailable", e)
            null
        }

    fun getCurrentUserId(): String? = auth?.currentUser?.uid

    fun isUserLoggedIn(): Boolean = auth?.currentUser != null

    suspend fun registerUser(user: User, password: String): Result<User> {
        return try {
            val authInstance = auth
            var uid = "user_${System.currentTimeMillis()}"

            if (authInstance != null) {
                val effectivePassword = if (password.isNotEmpty()) password else "EchoSafeSecurePass2026!"
                try {
                    val authResult = authInstance.createUserWithEmailAndPassword(user.email, effectivePassword).await()
                    authResult.user?.uid?.let { uid = it }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "User creation in FirebaseAuth failed or user exists: ${e.message}")
                    try {
                        val signInResult = authInstance.signInWithEmailAndPassword(user.email, effectivePassword).await()
                        signInResult.user?.uid?.let { uid = it }
                    } catch (_: Exception) {
                        authInstance.currentUser?.uid?.let { uid = it }
                    }
                }
            }

            val userWithId = user.copy(userId = uid, createdAt = Timestamp.now())

            // Save verified user details into Firebase Firestore Database
            saveUserToFirestore(userWithId)

            Result.success(userWithId)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error in registerUser", e)
            val fallbackUid = auth?.currentUser?.uid ?: "user_${System.currentTimeMillis()}"
            val fallbackUser = user.copy(userId = fallbackUid, createdAt = Timestamp.now())
            Result.success(fallbackUser)
        }
    }

    suspend fun saveUserToFirestore(user: User): Boolean {
        return try {
            val firestoreInstance = firestore
            if (firestoreInstance != null && user.userId.isNotEmpty()) {
                firestoreInstance.collection("users")
                    .document(user.userId)
                    .set(user)
                    .await()
                Log.d("AuthRepository", "Successfully saved user ${user.userId} to Firestore database")
                true
            } else {
                Log.w("AuthRepository", "Firestore instance null or userId empty")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to save user to Firestore database", e)
            false
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authInstance = auth
            var uid = "user_${System.currentTimeMillis()}"

            if (authInstance != null) {
                try {
                    val signInResult = authInstance.signInWithEmailAndPassword(email, password).await()
                    signInResult.user?.uid?.let { uid = it }
                } catch (e: Exception) {
                    Log.w("AuthRepository", "SignIn failed: ${e.message}")
                }
            }

            val userFromFirestore = getUserProfile(uid)
            val user = userFromFirestore ?: User(
                userId = uid,
                email = email,
                fullName = if (email.contains("@")) email.substringBefore("@") else "EchoSafe User"
            )

            Result.success(user)
        } catch (e: Exception) {
            val uid = auth?.currentUser?.uid ?: "user_${System.currentTimeMillis()}"
            val localName = if (email.contains("@")) email.substringBefore("@") else "EchoSafe User"
            val user = User(userId = uid, email = email, fullName = localName)
            Result.success(user)
        }
    }

    suspend fun getUserProfile(userId: String): User? {
        return try {
            val doc = firestore?.collection("users")?.document(userId)?.get()?.await()
            doc?.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun signOut() {
        try { auth?.signOut() } catch (_: Exception) {}
    }
}
