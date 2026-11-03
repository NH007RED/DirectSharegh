package com.directshare.app.webrtc.peerconnection

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription?) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(error: String?) = Unit
    override fun onSetFailure(error: String?) = Unit
}

suspend fun createSdpSuspend(block: (SdpObserver) -> Unit): SessionDescription =
    suspendCancellableCoroutine { cont ->
        block(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (!cont.isActive) return
                if (sdp != null) {
                    cont.resume(sdp)
                } else {
                    cont.resumeWithException(IllegalStateException("Created SessionDescription was null"))
                }
            }

            override fun onCreateFailure(error: String?) {
                if (cont.isActive) {
                    cont.resumeWithException(IllegalStateException("SDP creation failed: ${error ?: "unknown"}"))
                }
            }
        })
    }

suspend fun setSdpSuspend(block: (SdpObserver) -> Unit) =
    suspendCancellableCoroutine<Unit> { cont ->
        block(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onSetFailure(error: String?) {
                if (cont.isActive) {
                    cont.resumeWithException(IllegalStateException("Setting SDP failed: ${error ?: "unknown"}"))
                }
            }
        })
    }
