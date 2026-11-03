package com.directshare.app.domain.usecase

import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.domain.repository.SignalingRepository
import javax.inject.Inject

class JoinPairingSessionUseCase @Inject constructor(
    private val signalingRepository: SignalingRepository
) {
    suspend fun byPairingCode(rawCode: String, deviceName: String): Result<SignalingSession> {
        val normalizedCode = rawCode.trim().uppercase()
        if (normalizedCode.length != 6 || !normalizedCode.all { it.isLetterOrDigit() }) {
            return Result.failure(IllegalArgumentException("Pairing code must be 6 alphanumeric characters"))
        }
        val authResult = signalingRepository.ensureAuthenticated()
        if (authResult.isFailure) {
            return Result.failure(
                authResult.exceptionOrNull() ?: IllegalStateException("Unable to initialize secure session")
            )
        }
        return signalingRepository.joinSessionByPairingCode(normalizedCode, deviceName.trim().take(64))
    }

    suspend fun bySessionId(sessionId: String, deviceName: String): Result<SignalingSession> {
        val cleanId = sessionId.trim()
        if (cleanId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Session ID cannot be empty"))
        }
        val authResult = signalingRepository.ensureAuthenticated()
        if (authResult.isFailure) {
            return Result.failure(
                authResult.exceptionOrNull() ?: IllegalStateException("Unable to initialize secure session")
            )
        }
        return signalingRepository.joinSessionById(cleanId, deviceName.trim().take(64))
    }
}
