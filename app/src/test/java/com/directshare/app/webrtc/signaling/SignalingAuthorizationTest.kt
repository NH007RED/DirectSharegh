package com.directshare.app.webrtc.signaling

import com.directshare.app.data.firebase.SignalingSessionValidator
import com.directshare.app.data.firebase.SignalingSessionValidator.ValidationOutcome
import com.directshare.app.domain.model.SessionStatus
import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.utils.QrCodeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalingAuthorizationTest {

    @Test
    fun `unapproved guest or third-party intruder cannot write SDP offer, answer, or ICE candidates`() {
        val now = 1_700_000_000_000L
        val session = SignalingSession(
            sessionId = "sess-100",
            pairingCode = "A9K3M7",
            hostUid = "uid-host",
            hostDeviceName = "Host Phone",
            guestUid = "uid-guest",
            guestDeviceName = "Guest Phone",
            isAuthorized = false,
            status = SessionStatus.AWAITING_AUTHORIZATION,
            createdAt = now,
            expiresAt = now + 600_000L
        )

        // Before host authorization, neither host nor guest can exchange SDP/ICE
        assertTrue(SignalingSessionValidator.canWriteOffer(session, "uid-host", now) is ValidationOutcome.Denied)
        assertTrue(SignalingSessionValidator.canWriteAnswer(session, "uid-guest", now) is ValidationOutcome.Denied)
        assertTrue(SignalingSessionValidator.canWriteIceCandidate(session, "uid-host", now) is ValidationOutcome.Denied)

        // After host authorization
        val authorizedSession = session.copy(isAuthorized = true, status = SessionStatus.AUTHORIZED)
        assertEquals(ValidationOutcome.Allowed, SignalingSessionValidator.canWriteOffer(authorizedSession, "uid-host", now))
        assertEquals(ValidationOutcome.Allowed, SignalingSessionValidator.canWriteAnswer(authorizedSession, "uid-guest", now))
        assertEquals(ValidationOutcome.Allowed, SignalingSessionValidator.canWriteIceCandidate(authorizedSession, "uid-host", now))
        assertEquals(ValidationOutcome.Allowed, SignalingSessionValidator.canWriteIceCandidate(authorizedSession, "uid-guest", now))

        // Guest cannot write Offer, Host cannot write Answer, Intruder cannot write anything
        assertTrue(SignalingSessionValidator.canWriteOffer(authorizedSession, "uid-guest", now) is ValidationOutcome.Denied)
        assertTrue(SignalingSessionValidator.canWriteAnswer(authorizedSession, "uid-host", now) is ValidationOutcome.Denied)
        assertTrue(SignalingSessionValidator.canWriteIceCandidate(authorizedSession, "uid-intruder", now) is ValidationOutcome.Denied)
        assertTrue(SignalingSessionValidator.canJoinSession(authorizedSession, "uid-intruder", now) is ValidationOutcome.Denied)
    }

    @Test
    fun `expired session rejects all join and signaling operations`() {
        val createdAt = 1_700_000_000_000L
        val expiresAt = createdAt + 600_000L
        val afterExpiry = expiresAt + 1L

        val expiredSession = SignalingSession(
            sessionId = "sess-expired",
            pairingCode = "Z2X4C6",
            hostUid = "uid-host",
            hostDeviceName = "Host Phone",
            isAuthorized = true,
            status = SessionStatus.AUTHORIZED,
            createdAt = createdAt,
            expiresAt = expiresAt
        )

        assertTrue(expiredSession.isExpired(afterExpiry))
        assertTrue(SignalingSessionValidator.canJoinSession(expiredSession, "uid-guest", afterExpiry) is ValidationOutcome.Denied)
        assertTrue(SignalingSessionValidator.canWriteOffer(expiredSession, "uid-host", afterExpiry) is ValidationOutcome.Denied)
        assertTrue(SignalingSessionValidator.canWriteIceCandidate(expiredSession, "uid-host", afterExpiry) is ValidationOutcome.Denied)
    }

    @Test
    fun `QR code pairing URI encodes and parses sessionId and pairingCode accurately`() {
        val uri = QrCodeHelper.encodePairingUri("sess-uuid-42", "M8N4P9")
        val (parsedSessionId, parsedCode) = QrCodeHelper.parsePairingPayload(uri)
        assertEquals("sess-uuid-42", parsedSessionId)
        assertEquals("M8N4P9", parsedCode)
    }
}
