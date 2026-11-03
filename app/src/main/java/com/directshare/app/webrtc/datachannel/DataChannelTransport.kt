package com.directshare.app.webrtc.datachannel

/**
 * Abstraction over RTCDataChannel so FileChunkSender and FileChunkReceiver can run both
 * against real native WebRTC DataChannels on Android devices and against deterministic
 * in-memory loopback channels with simulated backpressure in unit tests.
 */
interface DataChannelTransport {
    fun isOpen(): Boolean
    fun bufferedAmount(): Long
    fun sendFrame(bytes: ByteArray, isBinary: Boolean = true): Boolean
}
