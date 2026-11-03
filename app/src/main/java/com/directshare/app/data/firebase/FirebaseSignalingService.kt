package com.directshare.app.data.firebase

import com.directshare.app.domain.model.PeerDevice
import com.directshare.app.domain.model.PeerPresenceStatus
import com.directshare.app.domain.model.SessionIceCandidate
import com.directshare.app.domain.model.SessionSdpPayload
import com.directshare.app.domain.model.SessionStatus
import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.utils.AppLogger
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages temporary WebRTC signaling over Firebase Realtime Database.
 * Strictly exchanges session metadata, SDP offer/answer, and ICE candidates.
 * NEVER touches file bytes or file chunks.
 */
class FirebaseSignalingService(
    private val database: FirebaseDatabase,
    private val authManager: FirebaseAuthManager
) {
    private val activeSessionIds = ConcurrentHashMap.newKeySet<String>()
    private val activePairingCodes = ConcurrentHashMap.newKeySet<String>()

    fun observeConnectionState(): Flow<Boolean> = callbackFlow {
        val connectedRef = database.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                trySend(connected)
            }

            override fun onCancelled(error: DatabaseError) {
                AppLogger.w("FirebaseSignalingService", "Connection state listener cancelled: ${error.message}")
                trySend(false)
            }
        }
        connectedRef.addValueEventListener(listener)
        awaitClose { connectedRef.removeEventListener(listener) }
    }

    suspend fun publishOnlinePresence(deviceName: String, activeSessionId: String?): Result<Unit> {
        val uid = authManager.ensureSignedIn().getOrElse { return Result.failure(it) }
        return try {
            val peerRef = database.getReference("onlinePeers").child(uid)
            val payload = mapOf(
                "uid" to uid,
                "deviceName" to deviceName.take(64),
                "status" to if (activeSessionId == null) PeerPresenceStatus.ONLINE_AVAILABLE.name else PeerPresenceStatus.IN_SESSION.name,
                "activeSessionId" to activeSessionId,
                "lastSeen" to ServerValue.TIMESTAMP
            )
            peerRef.onDisconnect().removeValue()
            peerRef.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("FirebaseSignalingService", "Failed to publish online presence", e)
            Result.failure(e)
        }
    }

    suspend fun removeOnlinePresence(): Result<Unit> {
        val uid = authManager.getCurrentUid() ?: return Result.success(Unit)
        return try {
            database.getReference("onlinePeers").child(uid).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeOnlinePeers(): Flow<List<PeerDevice>> = callbackFlow {
        val peersRef = database.getReference("onlinePeers")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val myUid = authManager.getCurrentUid()
                val now = System.currentTimeMillis()
                val peers = mutableListOf<PeerDevice>()
                for (child in snapshot.children) {
                    val uid = child.child("uid").getValue(String::class.java) ?: continue
                    if (uid == myUid) continue
                    val name = child.child("deviceName").getValue(String::class.java) ?: "Android Peer"
                    val statusStr = child.child("status").getValue(String::class.java)
                        ?: PeerPresenceStatus.ONLINE_AVAILABLE.name
                    val lastSeen = child.child("lastSeen").getValue(Long::class.java) ?: now
                    val sessionId = child.child("activeSessionId").getValue(String::class.java)
                    val status = runCatching { PeerPresenceStatus.valueOf(statusStr) }
                        .getOrDefault(PeerPresenceStatus.ONLINE_AVAILABLE)

                    // Filter out stale peers older than 15 minutes
                    if (now - lastSeen <= 15 * 60 * 1000L) {
                        peers.add(
                            PeerDevice(
                                uid = uid,
                                deviceName = name,
                                status = status,
                                lastSeenTimestamp = lastSeen,
                                activeSessionId = sessionId
                            )
                        )
                    }
                }
                trySend(peers.sortedByDescending { it.lastSeenTimestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                AppLogger.w("FirebaseSignalingService", "observeOnlinePeers cancelled: ${error.message}")
                trySend(emptyList())
            }
        }
        peersRef.addValueEventListener(listener)
        awaitClose { peersRef.removeEventListener(listener) }
    }

    suspend fun createHostingSession(deviceName: String): Result<SignalingSession> {
        val hostUid = authManager.ensureSignedIn().getOrElse { return Result.failure(it) }
        return try {
            val sessionId = UUID.randomUUID().toString()
            val pairingCode = SignalingSessionValidator.generatePairingCode()
            val now = System.currentTimeMillis()
            val expiresAt = now + SignalingSession.SESSION_TTL_MS

            val sessionRef = database.getReference("signalingSessions").child(sessionId)
            val codeRef = database.getReference("pairingCodes").child(pairingCode)

            val sessionData = mapOf(
                "sessionId" to sessionId,
                "pairingCode" to pairingCode,
                "createdAt" to now,
                "expiresAt" to expiresAt,
                "status" to SessionStatus.WAITING_FOR_PEER.name,
                "participants" to mapOf(
                    "hostUid" to hostUid,
                    "hostDeviceName" to deviceName.take(64),
                    "authorized" to false
                )
            )

            val codeData = mapOf(
                "sessionId" to sessionId,
                "hostUid" to hostUid,
                "expiresAt" to expiresAt
            )

            sessionRef.onDisconnect().removeValue()
            codeRef.onDisconnect().removeValue()

            sessionRef.setValue(sessionData).await()
            codeRef.setValue(codeData).await()

            activeSessionIds.add(sessionId)
            activePairingCodes.add(pairingCode)
            publishOnlinePresence(deviceName, sessionId)

            val session = SignalingSession(
                sessionId = sessionId,
                pairingCode = pairingCode,
                hostUid = hostUid,
                hostDeviceName = deviceName,
                status = SessionStatus.WAITING_FOR_PEER,
                createdAt = now,
                expiresAt = expiresAt
            )
            AppLogger.i("FirebaseSignalingService", "Created signaling session=$sessionId code=$pairingCode")
            Result.success(session)
        } catch (e: Exception) {
            AppLogger.e("FirebaseSignalingService", "Failed to create hosting session", e)
            Result.failure(e)
        }
    }

    suspend fun joinSessionByPairingCode(pairingCode: String, guestDeviceName: String): Result<SignalingSession> {
        val codeRef = database.getReference("pairingCodes").child(pairingCode.uppercase())
        return try {
            val snapshot = codeRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalArgumentException("Pairing code not found or already expired"))
            }
            val sessionId = snapshot.child("sessionId").getValue(String::class.java)
                ?: return Result.failure(IllegalStateException("Malformed pairing code record"))
            val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java) ?: 0L
            if (System.currentTimeMillis() >= expiresAt) {
                return Result.failure(IllegalStateException("Pairing code has expired"))
            }
            joinSessionById(sessionId, guestDeviceName)
        } catch (e: Exception) {
            AppLogger.e("FirebaseSignalingService", "Failed to resolve pairing code", e)
            Result.failure(e)
        }
    }

    suspend fun joinSessionById(sessionId: String, guestDeviceName: String): Result<SignalingSession> {
        val guestUid = authManager.ensureSignedIn().getOrElse { return Result.failure(it) }
        val sessionRef = database.getReference("signalingSessions").child(sessionId)
        return try {
            val snapshot = sessionRef.get().await()
            val existing = parseSessionSnapshot(snapshot)
                ?: return Result.failure(IllegalArgumentException("Signaling session does not exist"))

            when (val check = SignalingSessionValidator.canJoinSession(existing, guestUid)) {
                is SignalingSessionValidator.ValidationOutcome.Denied -> {
                    return Result.failure(SecurityException(check.reason))
                }
                SignalingSessionValidator.ValidationOutcome.Allowed -> Unit
            }

            val updates = mapOf(
                "participants/guestUid" to guestUid,
                "participants/guestDeviceName" to guestDeviceName.take(64),
                "participants/authorized" to false,
                "status" to SessionStatus.AWAITING_AUTHORIZATION.name
            )
            sessionRef.updateChildren(updates).await()
            activeSessionIds.add(sessionId)

            Result.success(
                existing.copy(
                    guestUid = guestUid,
                    guestDeviceName = guestDeviceName,
                    isAuthorized = false,
                    status = SessionStatus.AWAITING_AUTHORIZATION
                )
            )
        } catch (e: Exception) {
            AppLogger.e("FirebaseSignalingService", "Failed to join session $sessionId", e)
            Result.failure(e)
        }
    }

    suspend fun authorizeGuest(sessionId: String, authorized: Boolean): Result<Unit> {
        val uid = authManager.ensureSignedIn().getOrElse { return Result.failure(it) }
        val sessionRef = database.getReference("signalingSessions").child(sessionId)
        return try {
            val snapshot = sessionRef.get().await()
            val session = parseSessionSnapshot(snapshot)
                ?: return Result.failure(IllegalStateException("Session no longer exists"))
            if (session.hostUid != uid) {
                return Result.failure(SecurityException("Only the session host can authorize a peer"))
            }
            val nextStatus = if (authorized) SessionStatus.AUTHORIZED else SessionStatus.REJECTED
            val updates = mapOf(
                "participants/authorized" to authorized,
                "status" to nextStatus.name
            )
            sessionRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("FirebaseSignalingService", "Failed to update guest authorization", e)
            Result.failure(e)
        }
    }

    suspend fun updateSessionStatus(sessionId: String, status: SessionStatus): Result<Unit> {
        return try {
            database.getReference("signalingSessions")
                .child(sessionId)
                .child("status")
                .setValue(status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSession(sessionId: String): Flow<SignalingSession?> = callbackFlow {
        val sessionRef = database.getReference("signalingSessions").child(sessionId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseSessionSnapshot(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                AppLogger.w("FirebaseSignalingService", "observeSession cancelled: ${error.message}")
                trySend(null)
            }
        }
        sessionRef.addValueEventListener(listener)
        awaitClose { sessionRef.removeEventListener(listener) }
    }

    suspend fun sendOffer(sessionId: String, sdp: String): Result<Unit> {
        val senderUid = authManager.ensureSignedIn().getOrElse { return Result.failure(it) }
        val sessionRef = database.getReference("signalingSessions").child(sessionId)
        return try {
            val session = parseSessionSnapshot(sessionRef.get().await())
                ?: return Result.failure(IllegalStateException("Session not found"))
            when (val outcome = SignalingSessionValidator.canWriteOffer(session, senderUid)) {
                is SignalingSessionValidator.ValidationOutcome.Denied ->
                    return Result.failure(SecurityException(outcome.reason))
                SignalingSessionValidator.ValidationOutcome.Allowed -> Unit
            }

            val offerData = mapOf(
                "type" to "offer",
                "sdp" to sdp,
                "senderUid" to senderUid,
                "timestamp" to System.currentTimeMillis()
            )
            sessionRef.child("offer").setValue(offerData).await()
            sessionRef.child("status").setValue(SessionStatus.NEGOTIATING.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("FirebaseSignalingService", "Failed to publish SDP offer", e)
            Result.failure(e)
        }
    }

    suspend fun sendAnswer(sessionId: String, sdp: String): Result<Unit> {
        val senderUid = authManager.ensureSignedIn().getOrElse { return Result.failure(it) }
        val sessionRef = database.getReference("signalingSessions").child(sessionId)
        return try {
            val session = parseSessionSnapshot(sessionRef.get().await())
                ?: return Result.failure(IllegalStateException("Session not found"))
            when (val outcome = SignalingSessionValidator.canWriteAnswer(session, senderUid)) {
                is SignalingSessionValidator.ValidationOutcome.Denied ->
                    return Result.failure(SecurityException(outcome.reason))
                SignalingSessionValidator.ValidationOutcome.Allowed -> Unit
            }

            val answerData = mapOf(
                "type" to "answer",
                "sdp" to sdp,
                "senderUid" to senderUid,
                "timestamp" to System.currentTimeMillis()
            )
            sessionRef.child("answer").setValue(answerData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("FirebaseSignalingService", "Failed to publish SDP answer", e)
            Result.failure(e)
        }
    }

    fun observeOffer(sessionId: String): Flow<SessionSdpPayload?> = callbackFlow {
        val offerRef = database.getReference("signalingSessions").child(sessionId).child("offer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseSdpSnapshot(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        offerRef.addValueEventListener(listener)
        awaitClose { offerRef.removeEventListener(listener) }
    }

    fun observeAnswer(sessionId: String): Flow<SessionSdpPayload?> = callbackFlow {
        val answerRef = database.getReference("signalingSessions").child(sessionId).child("answer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(parseSdpSnapshot(snapshot))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        answerRef.addValueEventListener(listener)
        awaitClose { answerRef.removeEventListener(listener) }
    }

    suspend fun sendIceCandidate(
        sessionId: String,
        sdpMid: String,
        sdpMLineIndex: Int,
        sdp: String
    ): Result<Unit> {
        val senderUid = authManager.ensureSignedIn().getOrElse { return Result.failure(it) }
        val candidatesRef = database.getReference("signalingSessions")
            .child(sessionId)
            .child("iceCandidates")
            .child(senderUid)
            .push()
        return try {
            val payload = mapOf(
                "sdpMid" to sdpMid,
                "sdpMLineIndex" to sdpMLineIndex,
                "sdp" to sdp,
                "senderUid" to senderUid,
                "timestamp" to System.currentTimeMillis()
            )
            candidatesRef.setValue(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w("FirebaseSignalingService", "Failed to send ICE candidate", e)
            Result.failure(e)
        }
    }

    fun observeRemoteIceCandidates(
        sessionId: String,
        remoteUid: String
    ): Flow<SessionIceCandidate> = callbackFlow {
        val candidatesRef = database.getReference("signalingSessions")
            .child(sessionId)
            .child("iceCandidates")
            .child(remoteUid)

        val childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val sdp = snapshot.child("sdp").getValue(String::class.java) ?: return
                val sdpMid = snapshot.child("sdpMid").getValue(String::class.java) ?: ""
                val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                val senderUid = snapshot.child("senderUid").getValue(String::class.java) ?: remoteUid
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                trySend(
                    SessionIceCandidate(
                        candidateId = snapshot.key ?: UUID.randomUUID().toString(),
                        sdpMid = sdpMid,
                        sdpMLineIndex = sdpMLineIndex,
                        sdp = sdp,
                        senderUid = senderUid,
                        timestamp = timestamp
                    )
                )
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onChildRemoved(snapshot: DataSnapshot) = Unit
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) = Unit
        }
        candidatesRef.addChildEventListener(childListener)
        awaitClose { candidatesRef.removeEventListener(childListener) }
    }

    suspend fun cleanupSession(sessionId: String, pairingCode: String?): Result<Unit> {
        return try {
            if (!pairingCode.isNullOrBlank()) {
                database.getReference("pairingCodes").child(pairingCode).removeValue().await()
                activePairingCodes.remove(pairingCode)
            }
            database.getReference("signalingSessions").child(sessionId).removeValue().await()
            activeSessionIds.remove(sessionId)
            AppLogger.i("FirebaseSignalingService", "Cleaned up signaling session=$sessionId")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.w("FirebaseSignalingService", "Cleanup warning for session=$sessionId", e)
            Result.failure(e)
        }
    }

    suspend fun disconnectAllSessions(): Result<Unit> {
        return try {
            for (code in activePairingCodes.toList()) {
                runCatching { database.getReference("pairingCodes").child(code).removeValue().await() }
                activePairingCodes.remove(code)
            }
            for (sessionId in activeSessionIds.toList()) {
                runCatching { database.getReference("signalingSessions").child(sessionId).removeValue().await() }
                activeSessionIds.remove(sessionId)
            }
            removeOnlinePresence()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSessionSnapshot(snapshot: DataSnapshot): SignalingSession? {
        if (!snapshot.exists()) return null
        val sessionId = snapshot.child("sessionId").getValue(String::class.java) ?: snapshot.key ?: return null
        val pairingCode = snapshot.child("pairingCode").getValue(String::class.java) ?: ""
        val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java)
            ?: (createdAt + SignalingSession.SESSION_TTL_MS)
        val statusStr = snapshot.child("status").getValue(String::class.java)
            ?: SessionStatus.WAITING_FOR_PEER.name
        val status = runCatching { SessionStatus.valueOf(statusStr) }
            .getOrDefault(SessionStatus.WAITING_FOR_PEER)

        val participantsNode = snapshot.child("participants")
        val hostUid = participantsNode.child("hostUid").getValue(String::class.java) ?: return null
        val hostName = participantsNode.child("hostDeviceName").getValue(String::class.java) ?: "Host Phone"
        val guestUid = participantsNode.child("guestUid").getValue(String::class.java)
        val guestName = participantsNode.child("guestDeviceName").getValue(String::class.java)
        val authorized = participantsNode.child("authorized").getValue(Boolean::class.java) ?: false

        return SignalingSession(
            sessionId = sessionId,
            pairingCode = pairingCode,
            hostUid = hostUid,
            hostDeviceName = hostName,
            guestUid = guestUid,
            guestDeviceName = guestName,
            isAuthorized = authorized,
            status = status,
            createdAt = createdAt,
            expiresAt = expiresAt
        )
    }

    private fun parseSdpSnapshot(snapshot: DataSnapshot): SessionSdpPayload? {
        if (!snapshot.exists()) return null
        val type = snapshot.child("type").getValue(String::class.java) ?: return null
        val sdp = snapshot.child("sdp").getValue(String::class.java) ?: return null
        val senderUid = snapshot.child("senderUid").getValue(String::class.java) ?: ""
        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
        return SessionSdpPayload(
            type = type,
            sdp = sdp,
            senderUid = senderUid,
            timestamp = timestamp
        )
    }
}
