package com.directshare.app.domain.usecase

import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.domain.repository.SignalingRepository
import javax.inject.Inject

class CreatePairingSessionUseCase @Inject constructor(
    private val signalingRepository: SignalingRepository
) {
    suspend operator fun invoke(deviceName: String): Result<SignalingSession> {
        val sanitizedName = deviceName.trim().ifBlank { "Android Phone" }.take(64)
        val authResult = signalingRepository.ensureAuthenticated()
        if (authResult.isFailure) {
            return Result.failure(
                authResult.exceptionOrNull() ?: IllegalStateException("Unable to initialize secure session")
            )
        }
        return signalingRepository.createHostingSession(sanitizedName)
    }
}
