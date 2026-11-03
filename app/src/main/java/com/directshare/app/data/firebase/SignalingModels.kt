package com.directshare.app.data.firebase

import com.directshare.app.domain.model.SessionStatus
import com.directshare.app.domain.model.SignalingSession

/**
 * Pure validator and helper logic for signaling sessions, ensuring strict participant
 * membership checks, session expiration checks, and pairing code generation.
 * Also used directly in unit tests to verify authorization rules.
 */
object SignalingSessionValidator {
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generatePairingCode(length: Int = 6): String {
        val sb = StringBuilder(length)
        val secureRandom = java.security.SecureRandom()
        repeat(length) {
            sb.append(CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)])
        }
        return sb.toString()
    }

    fun canJoinSession(
        session: SignalingSession,
        requestingUid: String,
        nowMs: Long = System.currentTimeMillis()
    ): ValidationOutcome {
        if (session.isExpired(nowMs) || session.status == SessionStatus.EXPIRED || session.status == SessionStatus.CLOSED) {
            return ValidationOutcome.Denied("Session has expired or is already closed")
        }
        if (session.hostUid == requestingUid) {
            return ValidationOutcome.Allowed
        }
        if (!session.guestUid.isNullOrBlank() && session.guestUid != requestingUid) {
            return ValidationOutcome.Denied("Session already has an active peer participant")
        }
        return ValidationOutcome.Allowed
    }

    fun canWriteOffer(session: SignalingSession, senderUid: String, nowMs: Long = System.currentTimeMillis()): ValidationOutcome {
        if (session.isExpired(nowMs)) return ValidationOutcome.Denied("Session expired")
        if (session.hostUid != senderUid) {
            return ValidationOutcome.Denied("Only the session host can publish an SDP offer")
        }
        if (!session.isAuthorized) {
            return ValidationOutcome.Denied("Peer has not been authorized by the host yet")
        }
        return ValidationOutcome.Allowed
    }

    fun canWriteAnswer(session: SignalingSession, senderUid: String, nowMs: Long = System.currentTimeMillis()): ValidationOutcome {
        if (session.isExpired(nowMs)) return ValidationOutcome.Denied("Session expired")
        if (session.guestUid != senderUid) {
            return ValidationOutcome.Denied("Only the joined guest peer can publish an SDP answer")
        }
        if (!session.isAuthorized) {
            return ValidationOutcome.Denied("Guest is not authorized to answer in this session")
        }
        return ValidationOutcome.Allowed
    }

    fun canWriteIceCandidate(session: SignalingSession, senderUid: String, nowMs: Long = System.currentTimeMillis()): ValidationOutcome {
        if (session.isExpired(nowMs)) return ValidationOutcome.Denied("Session expired")
        val isParticipant = (session.hostUid == senderUid || session.guestUid == senderUid)
        if (!isParticipant) {
            return ValidationOutcome.Denied("Unauthorized user cannot write ICE candidates to this session")
        }
        if (!session.isAuthorized) {
            return ValidationOutcome.Denied("ICE candidates cannot be exchanged before host authorization")
        }
        return ValidationOutcome.Allowed
    }

    sealed interface ValidationOutcome {
        data object Allowed : ValidationOutcome
        data class Denied(val reason: String) : ValidationOutcome
    }
}
