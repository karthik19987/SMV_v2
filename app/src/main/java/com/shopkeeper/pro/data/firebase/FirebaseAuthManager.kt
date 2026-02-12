package com.shopkeeper.pro.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.shopkeeper.pro.data.entity.User
import com.shopkeeper.pro.data.firebase.models.FirestoreUser
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseAuthManager {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = FirebaseConfig.firestore
    private val TAG = "FirebaseAuthManager"

    // Check if user is logged in
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    // Get current Firebase user
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Sign in with email and password
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User is null after sign in")

            // Fetch user data from Firestore
            val firestoreUser = fetchUserFromFirestore(firebaseUser.uid)
                ?: throw Exception("User data not found in Firestore")

            val localUser = firestoreUser.toLocalUser()
            Result.success(localUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    // Create new user account
    suspend fun createAccount(
        email: String,
        password: String,
        username: String,
        displayName: String,
        role: String = "user"
    ): Result<User> {
        return try {
            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User is null after creation")

            // Create Firestore user document
            val firestoreUser = FirestoreUser(
                id = firebaseUser.uid,
                username = username,
                displayName = displayName,
                email = email,
                role = role,
                isActive = true
            )

            // Save to Firestore
            firestore.collection(FirebaseConfig.Collections.USERS)
                .document(firebaseUser.uid)
                .set(firestoreUser, SetOptions.merge())
                .await()

            val localUser = firestoreUser.toLocalUser()
            Result.success(localUser)
        } catch (e: Exception) {
            Log.e(TAG, "Account creation failed", e)
            // If Firestore write fails, delete the auth user to maintain consistency
            auth.currentUser?.delete()
            Result.failure(e)
        }
    }

    // Sign out
    suspend fun signOut() {
        auth.signOut()
    }

    // Reset password
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            Result.failure(e)
        }
    }

    // Update user password
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user logged in")
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password update failed", e)
            Result.failure(e)
        }
    }

    // Fetch user data from Firestore
    private suspend fun fetchUserFromFirestore(userId: String): FirestoreUser? {
        return try {
            val document = firestore.collection(FirebaseConfig.Collections.USERS)
                .document(userId)
                .get()
                .await()

            document.toObject(FirestoreUser::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Firestore", e)
            null
        }
    }

    // Create demo users (for testing)
    suspend fun createDemoUsers(): Result<Unit> {
        return try {
            // Create admin user
            val adminResult = createAccount(
                email = "admin@shopkeeperpro.com",
                password = "admin123",
                username = "admin",
                displayName = "Shop Admin",
                role = "admin"
            )

            // Create regular user
            val userResult = createAccount(
                email = "user1@shopkeeperpro.com",
                password = "user1234",
                username = "user1",
                displayName = "Shop User",
                role = "user"
            )

            if (adminResult.isSuccess && userResult.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to create demo users"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating demo users", e)
            Result.failure(e)
        }
    }
}

// Extension function to convert Firestore user to local Room entity
private fun FirestoreUser.toLocalUser(): User {
    return User(
        id = this.id,
        username = this.username,
        displayName = this.displayName,
        role = this.role,
        password = "", // We don't store password locally when using Firebase Auth
        createdAt = this.createdAt?.toDate() ?: Date(),
        isActive = this.isActive
    )
}