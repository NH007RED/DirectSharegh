package com.directshare.app.webrtc.peerconnection

import android.content.Context
import com.directshare.app.domain.model.IceServerConfig
import com.directshare.app.utils.AppLogger
import com.directshare.app.webrtc.datachannel.DataChannelTransport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the native Android org.webrtc PeerConnectionFactory, PeerConnection, ICE candidate queuing,
 * and ordered reliable RTCDataChannel for peer-to-peer binary file transfers.
 */
@Singleton
class WebRtcClient @Inject constructor(
    @ApplicationContext private val appContext: Context
) : DataChannelTransport {

    interface Listener {
        fun onLocalIceCandidateFound(candidate: IceCandidate)
        fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState)
        fun onPeerConnectionStateChanged(state: PeerConnection.PeerConnectionState)
        fun onDataChannelOpened()
        fun onDataChannelClosed()
        fun onDataChannelPacketReceived(bytes: ByteArray, isBinary: Boolean)
        fun onBufferedAmountChanged(previousAmount: Long, currentAmount: Long)
    }

    private val factoryInitialized = AtomicBoolean(false)
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var activeDataChannel: DataChannel? = null
    private var listener: Listener? = null

    private val iceMutex = Mutex()
    private var remoteDescriptionSet = false
    private val pendingRemoteCandidates = mutableListOf<IceCandidate>()

    private val _iceConnectionState = MutableStateFlow("NEW")
    val iceConnectionState: StateFlow<String> = _iceConnectionState.asStateFlow()

    private val _peerConnectionState = MutableStateFlow("NEW")
    val peerConnectionState: StateFlow<String> = _peerConnectionState.asStateFlow()

    private val _dataChannelState = MutableStateFlow("CLOSED")
    val dataChannelState: StateFlow<String> = _dataChannelState.asStateFlow()

    val localCandidatesCount = AtomicInteger(0)
    val remoteCandidatesCount = AtomicInteger(0)

    fun setListener(newListener: Listener?) {
        this.listener = newListener
    }

    @Synchronized
    private fun ensureFactoryInitialized() {
        if (factoryInitialized.compareAndSet(false, true)) {
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
            AppLogger.i("WebRtcClient", "PeerConnectionFactory initialized")
        }
    }

    suspend fun initializePeerConnection(
        iceServerConfigs: List<IceServerConfig>,
        isOffererHost: Boolean
    ): Result<Unit> {
        close()
        ensureFactoryInitialized()
        val factory = peerConnectionFactory
            ?: return Result.failure(IllegalStateException("PeerConnectionFactory unavailable"))

        val rtcIceServers = iceServerConfigs.map { config ->
            val builder = PeerConnection.IceServer.builder(config.urls)
            if (!config.username.isNullOrBlank()) {
                builder.setUsername(config.username)
            }
            if (!config.credential.isNullOrBlank()) {
                builder.setPassword(config.credential)
            }
            builder.createIceServer()
        }

        val rtcConfig = PeerConnection.RTCConfiguration(rtcIceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }

        val pc = factory.createPeerConnection(
            rtcConfig,
            object : PeerConnectionObserverAdapter() {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    if (candidate != null) {
                        localCandidatesCount.incrementAndGet()
                        listener?.onLocalIceCandidateFound(candidate)
                    }
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                    if (newState != null) {
                        _iceConnectionState.value = newState.name
                        AppLogger.i("WebRtcClient", "ICE connection state -> $newState")
                        listener?.onIceConnectionStateChanged(newState)
                    }
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    if (newState != null) {
                        _peerConnectionState.value = newState.name
                        AppLogger.i("WebRtcClient", "PeerConnection state -> $newState")
                        listener?.onPeerConnectionStateChanged(newState)
                    }
                }

                override fun onDataChannel(dataChannel: DataChannel?) {
                    if (dataChannel != null) {
                        AppLogger.i("WebRtcClient", "Remote RTCDataChannel received: ${dataChannel.label()}")
                        attachDataChannel(dataChannel)
                    }
                }
            }
        ) ?: return Result.failure(IllegalStateException("Failed to create native PeerConnection"))

        peerConnection = pc

        if (isOffererHost) {
            val dcInit = DataChannel.Init().apply {
                ordered = true
                negotiated = false
            }
            val channel = pc.createDataChannel(DATA_CHANNEL_LABEL, dcInit)
                ?: return Result.failure(IllegalStateException("Failed to create RTCDataChannel"))
            attachDataChannel(channel)
        }

        return Result.success(Unit)
    }

    private fun attachDataChannel(channel: DataChannel) {
        activeDataChannel = channel
        _dataChannelState.value = channel.state().name
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {
                val current = channel.bufferedAmount()
                listener?.onBufferedAmountChanged(previousAmount, current)
            }

            override fun onStateChange() {
                val state = channel.state()
                _dataChannelState.value = state.name
                AppLogger.i("WebRtcClient", "RTCDataChannel state -> $state")
                when (state) {
                    DataChannel.State.OPEN -> listener?.onDataChannelOpened()
                    DataChannel.State.CLOSED -> listener?.onDataChannelClosed()
                    else -> Unit
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                if (buffer == null) return
                val byteBuffer = buffer.data
                val bytes = ByteArray(byteBuffer.remaining())
                byteBuffer.get(bytes)
                listener?.onDataChannelPacketReceived(bytes, buffer.binary)
            }
        })
        if (channel.state() == DataChannel.State.OPEN) {
            listener?.onDataChannelOpened()
        }
    }

    suspend fun createOffer(): Result<String> {
        val pc = peerConnection ?: return Result.failure(IllegalStateException("PeerConnection not initialized"))
        return try {
            val constraints = MediaConstraints()
            val offer = createSdpSuspend { observer -> pc.createOffer(observer, constraints) }
            setSdpSuspend { observer -> pc.setLocalDescription(observer, offer) }
            Result.success(offer.description)
        } catch (e: Exception) {
            AppLogger.e("WebRtcClient", "createOffer failed", e)
            Result.failure(e)
        }
    }

    suspend fun setRemoteOfferAndCreateAnswer(remoteOfferSdp: String): Result<String> {
        val pc = peerConnection ?: return Result.failure(IllegalStateException("PeerConnection not initialized"))
        return try {
            val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, remoteOfferSdp)
            setSdpSuspend { observer -> pc.setRemoteDescription(observer, remoteDesc) }
            drainQueuedRemoteCandidates(pc)

            val constraints = MediaConstraints()
            val answer = createSdpSuspend { observer -> pc.createAnswer(observer, constraints) }
            setSdpSuspend { observer -> pc.setLocalDescription(observer, answer) }
            Result.success(answer.description)
        } catch (e: Exception) {
            AppLogger.e("WebRtcClient", "setRemoteOfferAndCreateAnswer failed", e)
            Result.failure(e)
        }
    }

    suspend fun setRemoteAnswer(remoteAnswerSdp: String): Result<Unit> {
        val pc = peerConnection ?: return Result.failure(IllegalStateException("PeerConnection not initialized"))
        return try {
            val remoteDesc = SessionDescription(SessionDescription.Type.ANSWER, remoteAnswerSdp)
            setSdpSuspend { observer -> pc.setRemoteDescription(observer, remoteDesc) }
            drainQueuedRemoteCandidates(pc)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("WebRtcClient", "setRemoteAnswer failed", e)
            Result.failure(e)
        }
    }

    suspend fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        iceMutex.withLock {
            remoteCandidatesCount.incrementAndGet()
            val pc = peerConnection
            if (pc != null && remoteDescriptionSet) {
                pc.addIceCandidate(candidate)
            } else {
                pendingRemoteCandidates.add(candidate)
            }
        }
    }

    private suspend fun drainQueuedRemoteCandidates(pc: PeerConnection) {
        iceMutex.withLock {
            remoteDescriptionSet = true
            for (candidate in pendingRemoteCandidates) {
                pc.addIceCandidate(candidate)
            }
            pendingRemoteCandidates.clear()
        }
    }

    override fun isOpen(): Boolean {
        return activeDataChannel?.state() == DataChannel.State.OPEN
    }

    override fun bufferedAmount(): Long {
        return activeDataChannel?.bufferedAmount() ?: 0L
    }

    override fun sendFrame(bytes: ByteArray, isBinary: Boolean): Boolean {
        val channel = activeDataChannel ?: return false
        if (channel.state() != DataChannel.State.OPEN) return false
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(bytes), isBinary)
        return channel.send(buffer)
    }

    suspend fun close() {
        iceMutex.withLock {
            remoteDescriptionSet = false
            pendingRemoteCandidates.clear()
        }
        localCandidatesCount.set(0)
        remoteCandidatesCount.set(0)

        try {
            activeDataChannel?.unregisterObserver()
            activeDataChannel?.close()
            activeDataChannel?.dispose()
        } catch (_: Exception) {
        } finally {
            activeDataChannel = null
            _dataChannelState.value = "CLOSED"
        }

        try {
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (_: Exception) {
        } finally {
            peerConnection = null
            _iceConnectionState.value = "CLOSED"
            _peerConnectionState.value = "CLOSED"
        }
    }

    companion object {
        const val DATA_CHANNEL_LABEL = "directshare-file-channel-v1"
    }
}
