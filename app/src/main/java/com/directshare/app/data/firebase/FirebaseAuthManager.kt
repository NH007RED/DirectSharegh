package com.directshare.app.data.firebase

import com.directshare.app.utils.AppLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Ensures every peer is authenticated with Firebase Authentication before reading or writing
 * to any /signalingSessions or /onlinePeers node in Firebase Realtime Database.
 */
class FirebaseAuthManager(
    private val firebaseAuth: FirebaseAuth
) {
    fun getCurrentUid(): String? = firebaseAuth.currentUser?.uid

    suspend fun ensureSignedIn(): Result<String> {
        val existingUid = firebaseAuth.currentUser?.uid
        if (!existingUid.isNullOrBlank()) {
            return Result.success(existingUid)
        }
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val uid = authResult.user?.uid
                ?: return Result.failure(IllegalStateException("Firebase Auth returned null UID"))
            AppLogger.i("FirebaseAuthManager", "Authenticated with Firebase Auth UID=$uid")
            Result.success(uid)
        } catch (e: Exception) {
            AppLogger.e("FirebaseAuthManager", "Firebase anonymous sign-in failed", e)
            Result.failure(e)
        }
    }
}
