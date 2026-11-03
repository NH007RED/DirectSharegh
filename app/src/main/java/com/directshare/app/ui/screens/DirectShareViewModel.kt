package com.directshare.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directshare.app.domain.model.AppSettings
import com.directshare.app.domain.model.ConnectionDiagnostics
import com.directshare.app.domain.model.FileTransferItem
import com.directshare.app.domain.model.IceServerConfig
import com.directshare.app.domain.model.PeerDevice
import com.directshare.app.domain.model.SessionStatus
import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.domain.model.ThemePreference
import com.directshare.app.domain.model.TransferFailureReason
import com.directshare.app.domain.model.TransferHistoryRecord
import com.directshare.app.domain.model.TransferState
import com.directshare.app.domain.model.TransferStatisticsSummary
import com.directshare.app.domain.repository.SettingsRepository
import com.directshare.app.domain.repository.SignalingRepository
import com.directshare.app.domain.usecase.CreatePairingSessionUseCase
import com.directshare.app.domain.usecase.JoinPairingSessionUseCase
import com.directshare.app.domain.usecase.ObserveTransferHistoryUseCase
import com.directshare.app.domain.usecase.ValidateFileSelectionUseCase
import com.directshare.app.utils.AppLogger
import com.directshare.app.utils.QrCodeHelper
import com.directshare.app.utils.StorageFileHelper
import com.directshare.app.webrtc.peerconnection.WebRtcClient
import com.directshare.app.webrtc.signaling.SignalingSessionCoordinator
import com.directshare.app.webrtc.transfer.TransferCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import javax.inject.Inject

@HiltViewModel
class DirectShareViewModel @Inject constructor(
    private val signalingRepository: SignalingRepository,
    private val settingsRepository: SettingsRepository,
    private val createPairingSessionUseCase: CreatePairingSessionUseCase,
    private val joinPairingSessionUseCase: JoinPairingSessionUseCase,
    private val observeTransferHistoryUseCase: ObserveTransferHistoryUseCase,
    private val validateFileSelectionUseCase: ValidateFileSelectionUseCase,
    private val signalingCoordinator: SignalingSessionCoordinator,
    private val transferCoordinator: TransferCoordinator,
    private val webRtcClient: WebRtcClient,
    private val storageFileHelper: StorageFileHelper
) : ViewModel(), WebRtcClient.Listener {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val historyRecords: StateFlow<List<TransferHistoryRecord>> =
        observeTransferHistoryUseCase.observeRecords().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transferStats: StateFlow<TransferStatisticsSummary> =
        observeTransferHistoryUseCase.observeStats().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransferStatisticsSummary()
        )

    val onlinePeers: StateFlow<List<PeerDevice>> =
        signalingRepository.observeOnlinePeers().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val firebaseConnected: StateFlow<Boolean> =
        signalingRepository.observeFirebaseConnectionState().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val transferState: StateFlow<TransferState> = transferCoordinator.transferState

    private val _selectedFiles = MutableStateFlow<List<FileTransferItem>>(emptyList())
    val selectedFiles: StateFlow<List<FileTransferItem>> = _selectedFiles.asStateFlow()

    private val _activeSession = MutableStateFlow<SignalingSession?>(null)
    val activeSession: StateFlow<SignalingSession?> = _activeSession.asStateFlow()

    private val _isHostOfSession = MutableStateFlow(false)
    val isHostOfSession: StateFlow<Boolean> = _isHostOfSession.asStateFlow()

    private val _uiBannerMessage = MutableStateFlow<String?>(null)
    val uiBannerMessage: StateFlow<String?> = _uiBannerMessage.asStateFlow()

    private val _lastDiagnosticError = MutableStateFlow<String?>(null)

    val diagnostics: StateFlow<ConnectionDiagnostics> = combine(
        firebaseConnected,
        _activeSession,
        webRtcClient.iceConnectionState,
        webRtcClient.peerConnectionState,
        webRtcClient.dataChannelState
    ) { fbConnected, session, iceState, pcState, dcState ->
        val currentSettings = settings.value
        ConnectionDiagnostics(
            firebaseAuthenticated = signalingRepository.getCurrentUid() != null,
            firebaseUid = signalingRepository.getCurrentUid(),
            signalingConnected = fbConnected,
            activeSessionId = session?.sessionId,
            iceConnectionState = iceState,
            peerConnectionState = pcState,
            dataChannelState = dcState,
            localCandidatesCount = webRtcClient.localCandidatesCount.get(),
            remoteCandidatesCount = webRtcClient.remoteCandidatesCount.get(),
            selectedCandidatePairType = if (currentSettings.customTurnUrl.isNotBlank()) "Direct + Relay Boost" else "Direct High-Speed Link",
            turnConfigured = currentSettings.customTurnUrl.isNotBlank(),
            lastError = _lastDiagnosticError.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectionDiagnostics()
    )

    private var sessionObserverJob: Job? = null
    private var webrtcNegotiationStartedForSession: String? = null

    init {
        webRtcClient.setListener(this)
        initializeSignalingPresence()
    }

    private fun initializeSignalingPresence() {
        viewModelScope.launch {
            val authRes = signalingRepository.ensureAuthenticated()
            if (authRes.isSuccess) {
                val currentName = settings.first().deviceDisplayName
                signalingRepository.publishOnlinePresence(currentName, null)
            } else {
                _lastDiagnosticError.value = authRes.exceptionOrNull()?.message
                    ?: "Network connection service is currently unreachable"
            }
        }
    }

    fun dismissBannerMessage() {
        _uiBannerMessage.value = null
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }

    // File Selection Actions
    fun onFilesPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val result = validateFileSelectionUseCase(uris)
            result.fold(
                onSuccess = { newItems ->
                    val merged = (_selectedFiles.value + newItems).distinctBy { it.uri }
                    _selectedFiles.value = merged
                },
                onFailure = { err ->
                    _uiBannerMessage.value = err.message ?: "Unable to read selected file(s)"
                }
            )
        }
    }

    fun removeSelectedFile(fileId: String) {
        _selectedFiles.value = _selectedFiles.value.filterNot { it.fileId == fileId }
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    // Session & Pairing Actions
    fun startHostingSession() {
        viewModelScope.launch {
            val name = settings.value.deviceDisplayName
            val result = createPairingSessionUseCase(name)
            result.fold(
                onSuccess = { session ->
                    _isHostOfSession.value = true
                    _activeSession.value = session
                    transferCoordinator.updateState(
                        TransferState.FindingDevices(
                            onlinePeers = onlinePeers.value,
                            hostedSession = session
                        )
                    )
                    observeSignalingSession(session.sessionId, isHost = true)
                },
                onFailure = { err ->
                    _lastDiagnosticError.value = err.message
                    transferCoordinator.updateState(
                        TransferState.Failed(
                            reason = TransferFailureReason.AUTHENTICATION_FAILED,
                            detailedMessage = err.message
                                ?: "Unable to start a pairing session. Please check your internet connection."
                        )
                    )
                }
            )
        }
    }

    fun joinSessionWithInput(rawCodeOrQrUri: String) {
        viewModelScope.launch {
            val name = settings.value.deviceDisplayName
            val (parsedSessionId, parsedCode) = QrCodeHelper.parsePairingPayload(rawCodeOrQrUri)
            val result = when {
                !parsedCode.isNullOrBlank() -> joinPairingSessionUseCase.byPairingCode(parsedCode, name)
                !parsedSessionId.isNullOrBlank() -> joinPairingSessionUseCase.bySessionId(parsedSessionId, name)
                else -> Result.failure(IllegalArgumentException("Please enter a valid 6-character pairing code"))
            }

            result.fold(
                onSuccess = { session ->
                    _isHostOfSession.value = false
                    _activeSession.value = session
                    transferCoordinator.updateState(
                        TransferState.WaitingForAuthorization(
                            sessionId = session.sessionId,
                            guestUid = session.guestUid.orEmpty(),
                            guestDeviceName = session.hostDeviceName,
                            isHost = false
                        )
                    )
                    observeSignalingSession(session.sessionId, isHost = false)
                },
                onFailure = { err ->
                    _lastDiagnosticError.value = err.message
                    transferCoordinator.updateState(
                        TransferState.Failed(
                            reason = TransferFailureReason.INVALID_PAIRING_CODE,
                            detailedMessage = err.message ?: "Invalid or expired pairing code."
                        )
                    )
                }
            )
        }
    }

    fun connectToDiscoveredPeer(peer: PeerDevice) {
        val targetSessionId = peer.activeSessionId
        if (targetSessionId.isNullOrBlank()) {
            _uiBannerMessage.value = "${peer.deviceName} is online but has not generated a pairing code yet. Tap 'Generate Code & QR' on either phone first."
            return
        }
        joinSessionWithInput(targetSessionId)
    }

    fun respondToGuestAuthorization(authorized: Boolean) {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            val res = signalingRepository.authorizeGuest(session.sessionId, authorized)
            if (res.isFailure) {
                _uiBannerMessage.value = "Unable to update device approval"
            } else if (!authorized) {
                disconnectCurrentSession()
            }
        }
    }

    private fun observeSignalingSession(sessionId: String, isHost: Boolean) {
        sessionObserverJob?.cancel()
        webrtcNegotiationStartedForSession = null

        sessionObserverJob = viewModelScope.launch {
            signalingRepository.observeSession(sessionId).collect { session ->
                _activeSession.value = session
                if (session == null) {
                    return@collect
                }

                if (session.isExpired()) {
                    transferCoordinator.updateState(
                        TransferState.Failed(
                            reason = TransferFailureReason.INVALID_PAIRING_CODE,
                            detailedMessage = "The pairing session expired before connection completed."
                        )
                    )
                    return@collect
                }

                when (session.status) {
                    SessionStatus.WAITING_FOR_PEER -> {
                        transferCoordinator.updateState(
                            TransferState.FindingDevices(
                                onlinePeers = onlinePeers.value,
                                hostedSession = session
                            )
                        )
                    }
                    SessionStatus.AWAITING_AUTHORIZATION -> {
                        transferCoordinator.updateState(
                            TransferState.WaitingForAuthorization(
                                sessionId = session.sessionId,
                                guestUid = session.guestUid.orEmpty(),
                                guestDeviceName = if (isHost) session.guestDeviceName.orEmpty() else session.hostDeviceName,
                                isHost = isHost
                            )
                        )
                    }
                    SessionStatus.REJECTED -> {
                        transferCoordinator.updateState(
                            TransferState.Failed(
                                reason = TransferFailureReason.UNAUTHORIZED_PEER,
                                detailedMessage = "The other user declined the connection request.",
                                peerDeviceName = session.hostDeviceName
                            )
                        )
                    }
                    SessionStatus.AUTHORIZED, SessionStatus.NEGOTIATING -> {
                        if (session.isAuthorized && webrtcNegotiationStartedForSession != session.sessionId) {
                            webrtcNegotiationStartedForSession = session.sessionId
                            val peerName = if (isHost) {
                                session.guestDeviceName ?: "Remote Phone"
                            } else {
                                session.hostDeviceName
                            }
                            transferCoordinator.updateState(
                                TransferState.Connecting(
                                    sessionId = session.sessionId,
                                    peerDeviceName = peerName,
                                    stepDescription = "Establishing secure direct link between both phones..."
                                )
                            )
                            signalingCoordinator.startWatchingAuthorizedSession(
                                scope = viewModelScope,
                                session = session,
                                isHost = isHost,
                                iceServers = buildIceServerList(settings.value),
                                onWebRtcNegotiationStarted = {
                                    AppLogger.i("DirectShareViewModel", "Connection negotiation started for ${session.sessionId}")
                                },
                                onNegotiationError = { errorMsg ->
                                    _lastDiagnosticError.value = errorMsg
                                    transferCoordinator.updateState(
                                        TransferState.Failed(
                                            reason = TransferFailureReason.WEBRTC_NEGOTIATION_FAILED,
                                            detailedMessage = errorMsg,
                                            peerDeviceName = peerName
                                        )
                                    )
                                }
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun buildIceServerList(currentSettings: AppSettings): List<IceServerConfig> {
        val list = IceServerConfig.defaultStunServers().toMutableList()
        if (currentSettings.customTurnUrl.isNotBlank()) {
            list.add(
                IceServerConfig(
                    urls = listOf(currentSettings.customTurnUrl.trim()),
                    username = currentSettings.customTurnUsername.takeIf { it.isNotBlank() },
                    credential = currentSettings.customTurnCredential.takeIf { it.isNotBlank() },
                    isTurnRelay = true
                )
            )
        }
        return list
    }

    // Transfer Actions
    fun sendSelectedFilesToConnectedPeer() {
        val files = _selectedFiles.value
        if (files.isEmpty()) {
            _uiBannerMessage.value = "Select at least one file before sending."
            return
        }
        val session = _activeSession.value
        val myUid = signalingRepository.getCurrentUid() ?: "local_uid"
        val myName = settings.value.deviceDisplayName
        val peerName = if (_isHostOfSession.value) {
            session?.guestDeviceName ?: "Connected Phone"
        } else {
            session?.hostDeviceName ?: "Connected Phone"
        }

        transferCoordinator.startSendingFiles(
            senderUid = myUid,
            senderDeviceName = myName,
            peerDeviceName = peerName,
            selectedFiles = files
        )
    }

    fun respondToIncomingTransfer(accept: Boolean) {
        transferCoordinator.respondToIncomingTransferOffer(accept)
    }

    fun cancelActiveTransfer() {
        transferCoordinator.cancelActiveTransfer()
    }

    fun resetTransferStateToReadyOrIdle() {
        val session = _activeSession.value
        if (webRtcClient.isOpen() && session != null) {
            val isHost = _isHostOfSession.value
            val peerUid = (if (isHost) session.guestUid else session.hostUid).orEmpty()
            val peerName = if (isHost) session.guestDeviceName.orEmpty() else session.hostDeviceName
            transferCoordinator.setConnectedPeer(session.sessionId, peerUid, peerName)
        } else {
            transferCoordinator.updateState(TransferState.Idle)
        }
    }

    fun disconnectCurrentSession() {
        viewModelScope.launch {
            sessionObserverJob?.cancel()
            signalingCoordinator.stopWatching()
            webRtcClient.close()
            val session = _activeSession.value
            if (session != null) {
                signalingRepository.cleanupSession(session.sessionId, session.pairingCode)
            }
            _activeSession.value = null
            _isHostOfSession.value = false
            transferCoordinator.updateState(TransferState.Idle)
            signalingRepository.publishOnlinePresence(settings.value.deviceDisplayName, null)
        }
    }

    fun disconnectAllSessions() {
        viewModelScope.launch {
            sessionObserverJob?.cancel()
            signalingCoordinator.stopWatching()
            webRtcClient.close()
            signalingRepository.disconnectAllSessions()
            _activeSession.value = null
            _isHostOfSession.value = false
            transferCoordinator.updateState(TransferState.Idle)
            _uiBannerMessage.value = "All active connections have been closed."
        }
    }

    // Helper for opening or sharing received files
    fun openReceivedFile(context: Context, uri: Uri, mimeType: String) {
        try {
            val intent = storageFileHelper.buildOpenFileIntent(uri, mimeType)
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiBannerMessage.value = "No installed app found to open this file type ($mimeType)."
        }
    }

    fun shareReceivedFile(context: Context, uri: Uri, mimeType: String, fileName: String) {
        try {
            val intent = storageFileHelper.buildShareFileIntent(uri, mimeType, fileName)
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiBannerMessage.value = "Unable to launch system share sheet."
        }
    }

    // Settings updates
    fun updateDeviceName(name: String) {
        viewModelScope.launch {
            settingsRepository.updateDeviceName(name)
            signalingRepository.publishOnlinePresence(name, _activeSession.value?.sessionId)
        }
    }

    fun updateDefaultSaveDestination(context: Context, treeUri: Uri?) {
        viewModelScope.launch {
            if (treeUri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            settingsRepository.updateDefaultSaveDestination(treeUri?.toString())
        }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch { settingsRepository.updateTheme(theme) }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateSoundEnabled(enabled) }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateVibrationEnabled(enabled) }
    }

    fun updateAutoAccept(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoAccept(enabled) }
    }

    fun updateMaxConcurrentTransfers(count: Int) {
        viewModelScope.launch { settingsRepository.updateMaxConcurrentTransfers(count) }
    }

    fun updateChunkSizeBytes(chunkSizeBytes: Int) {
        viewModelScope.launch { settingsRepository.updateChunkSizeBytes(chunkSizeBytes) }
    }

    fun updateTurnConfiguration(url: String, username: String, credential: String) {
        viewModelScope.launch {
            settingsRepository.updateTurnConfiguration(url, username, credential)
            _uiBannerMessage.value = "Connection boost settings saved."
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            observeTransferHistoryUseCase.clearAll()
            _uiBannerMessage.value = "Transfer history cleared."
        }
    }

    // WebRtcClient.Listener Callbacks
    override fun onLocalIceCandidateFound(candidate: IceCandidate) {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            signalingRepository.sendIceCandidate(
                sessionId = session.sessionId,
                sdpMid = candidate.sdpMid ?: "",
                sdpMLineIndex = candidate.sdpMLineIndex,
                sdp = candidate.sdp
            )
        }
    }

    override fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState) {
        if (state == PeerConnection.IceConnectionState.FAILED) {
            val detail = "Direct connection between both phones could not be completed on this network. Make sure both users have the app open at the same time and have an active internet connection."
            _lastDiagnosticError.value = detail
            transferCoordinator.updateState(
                TransferState.Failed(
                    reason = TransferFailureReason.STUN_TURN_CONNECTIVITY_FAILED,
                    detailedMessage = detail
                )
            )
        } else if (state == PeerConnection.IceConnectionState.DISCONNECTED) {
            transferCoordinator.onPeerDisconnectedUnexpectedly()
        }
    }

    override fun onPeerConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
        if (state == PeerConnection.PeerConnectionState.FAILED) {
            transferCoordinator.onPeerDisconnectedUnexpectedly()
        }
    }

    override fun onDataChannelOpened() {
        val session = _activeSession.value ?: return
        val isHost = _isHostOfSession.value
        val peerUid = (if (isHost) session.guestUid else session.hostUid).orEmpty()
        val peerName = if (isHost) {
            session.guestDeviceName ?: "Connected Phone"
        } else {
            session.hostDeviceName
        }

        viewModelScope.launch {
            signalingRepository.cleanupSession(session.sessionId, session.pairingCode)
        }
        transferCoordinator.setConnectedPeer(session.sessionId, peerUid, peerName)
    }

    override fun onDataChannelClosed() {
        transferCoordinator.onPeerDisconnectedUnexpectedly()
    }

    override fun onDataChannelPacketReceived(bytes: ByteArray, isBinary: Boolean) {
        transferCoordinator.onDataChannelFrameReceived(bytes)
    }

    override fun onBufferedAmountChanged(previousAmount: Long, currentAmount: Long) = Unit

    override fun onCleared() {
        super.onCleared()
        disconnectCurrentSession()
    }
}
