package com.directshare.app.webrtc.transfer

import android.content.Context
import com.directshare.app.domain.model.CompletedReceivedFile
import com.directshare.app.domain.model.FileTransferItem
import com.directshare.app.domain.model.IncomingFileDescriptor
import com.directshare.app.domain.model.IncomingTransferOffer
import com.directshare.app.domain.model.TransferDirection
import com.directshare.app.domain.model.TransferFailureReason
import com.directshare.app.domain.model.TransferHistoryRecord
import com.directshare.app.domain.model.TransferProgress
import com.directshare.app.domain.model.TransferRecordStatus
import com.directshare.app.domain.model.TransferState
import com.directshare.app.domain.repository.SettingsRepository
import com.directshare.app.domain.repository.TransferHistoryRepository
import com.directshare.app.utils.AppLogger
import com.directshare.app.utils.StorageFileHelper
import com.directshare.app.webrtc.datachannel.PacketProtocol
import com.directshare.app.webrtc.peerconnection.WebRtcClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferCoordinator @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val webRtcClient: WebRtcClient,
    private val storageFileHelper: StorageFileHelper,
    private val historyRepository: TransferHistoryRepository,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()

    private var activeSender: FileChunkSender? = null
    private var activeReceiver: FileChunkReceiver? = null
    private var activeTransferJob: Job? = null
    private var pendingReceiverDecision: CompletableDeferred<PacketProtocol.ControlMessage.TransferDecision>? = null
    private var pendingIncomingOffer: IncomingTransferOffer? = null
    private var connectedPeerName: String = "Connected Device"
    private var activeBatchId: String? = null
    private var transferStartTimestampMs: Long = 0L

    fun updateState(newState: TransferState) {
        _transferState.value = newState
    }

    fun setConnectedPeer(sessionId: String, peerUid: String, peerName: String) {
        connectedPeerName = peerName
        _transferState.value = TransferState.PeerConnectedReady(
            sessionId = sessionId,
            peerUid = peerUid,
            peerDeviceName = peerName
        )
    }

    fun startSendingFiles(
        senderUid: String,
        senderDeviceName: String,
        peerDeviceName: String,
        selectedFiles: List<FileTransferItem>
    ) {
        if (!webRtcClient.isOpen()) {
            _transferState.value = TransferState.Failed(
                reason = TransferFailureReason.RECEIVER_UNAVAILABLE,
                detailedMessage = "Direct connection is not active. Please reconnect both phones and try again.",
                peerDeviceName = peerDeviceName
            )
            return
        }

        activeTransferJob?.cancel()
        activeTransferJob = scope.launch {
            val batchId = UUID.randomUUID().toString()
            activeBatchId = batchId
            connectedPeerName = peerDeviceName

            val preparedFiles = mutableListOf<FileTransferItem>()
            for (item in selectedFiles) {
                val sha256 = item.sha256Checksum ?: storageFileHelper.computeUriSha256(item.uri)
                if (sha256 == null) {
                    _transferState.value = TransferState.Failed(
                        reason = TransferFailureReason.FILE_PERMISSION_ERROR,
                        detailedMessage = "Could not read file '${item.name}'. Please re-select the file and try again.",
                        peerDeviceName = peerDeviceName
                    )
                    return@launch
                }
                preparedFiles.add(item.copy(sha256Checksum = sha256))
            }

            val totalBytes = preparedFiles.sumOf { it.sizeBytes }
            val descriptors = preparedFiles.map {
                IncomingFileDescriptor(
                    fileId = it.fileId,
                    name = it.name,
                    mimeType = it.mimeType,
                    sizeBytes = it.sizeBytes,
                    sha256Checksum = it.sha256Checksum.orEmpty()
                )
            }

            val offer = IncomingTransferOffer(
                batchId = batchId,
                senderUid = senderUid,
                senderDeviceName = senderDeviceName,
                files = descriptors,
                totalBytes = totalBytes
            )

            val decisionDeferred = CompletableDeferred<PacketProtocol.ControlMessage.TransferDecision>()
            pendingReceiverDecision = decisionDeferred

            _transferState.value = TransferState.WaitingForReceiverApproval(
                batchId = batchId,
                peerDeviceName = peerDeviceName,
                fileCount = preparedFiles.size,
                totalBytes = totalBytes
            )

            val offerFrame = PacketProtocol.encodeControlMessage(
                PacketProtocol.ControlMessage.TransferOffer(offer)
            )
            if (!webRtcClient.sendFrame(offerFrame, isBinary = true)) {
                _transferState.value = TransferState.Failed(
                    reason = TransferFailureReason.CONNECTION_LOST,
                    detailedMessage = "Unable to send transfer request to the other phone.",
                    peerDeviceName = peerDeviceName
                )
                return@launch
            }

            val decision = withTimeoutOrNull(90_000L) {
                decisionDeferred.await()
            }

            if (decision == null) {
                _transferState.value = TransferState.Failed(
                    reason = TransferFailureReason.RECEIVER_UNAVAILABLE,
                    detailedMessage = "The other user did not respond within 90 seconds.",
                    peerDeviceName = peerDeviceName
                )
                return@launch
            }

            if (!decision.accepted) {
                recordHistoryForBatch(
                    files = descriptors,
                    direction = TransferDirection.SENT,
                    status = TransferRecordStatus.REJECTED,
                    peerName = peerDeviceName
                )
                _transferState.value = TransferState.Failed(
                    reason = TransferFailureReason.RECEIVER_REJECTED,
                    detailedMessage = decision.reason.ifBlank { "$peerDeviceName declined the incoming file transfer." },
                    peerDeviceName = peerDeviceName
                )
                return@launch
            }

            val settings = settingsRepository.settingsFlow.first()
            transferStartTimestampMs = System.currentTimeMillis()
            TransferForegroundService.startOrUpdate(
                context = appContext,
                title = "Sending to $peerDeviceName",
                subtitle = "Preparing ${preparedFiles.size} file(s)...",
                progressPercent = 0
            )

            val sender = FileChunkSender(
                transport = webRtcClient,
                inputStreamProvider = { fileItem -> storageFileHelper.openInputStream(fileItem.uri) }
            )
            activeSender = sender

            val sendResult = sender.sendBatch(
                batchId = batchId,
                filesWithChecksums = preparedFiles,
                chunkSizeBytes = settings.chunkSizeBytes,
                onProgress = { progress ->
                    _transferState.value = TransferState.Sending(
                        peerDeviceName = peerDeviceName,
                        progress = progress
                    )
                    TransferForegroundService.startOrUpdate(
                        context = appContext,
                        title = "Sending ${progress.currentFileName}",
                        subtitle = "${progress.overallPercentage}% • $peerDeviceName",
                        progressPercent = progress.overallPercentage
                    )
                }
            )

            TransferForegroundService.stop(appContext)
            activeSender = null

            sendResult.fold(
                onSuccess = {
                    val durationMs = (System.currentTimeMillis() - transferStartTimestampMs).coerceAtLeast(1L)
                    val avgSpeed = (totalBytes * 1000L) / durationMs
                    recordHistoryForBatch(
                        files = descriptors,
                        direction = TransferDirection.SENT,
                        status = TransferRecordStatus.COMPLETED,
                        peerName = peerDeviceName
                    )
                    _transferState.value = TransferState.Completed(
                        isSender = true,
                        peerDeviceName = peerDeviceName,
                        totalFiles = preparedFiles.size,
                        totalBytes = totalBytes,
                        durationMs = durationMs,
                        averageSpeedBytesPerSec = avgSpeed
                    )
                },
                onFailure = { err ->
                    recordHistoryForBatch(
                        files = descriptors,
                        direction = TransferDirection.SENT,
                        status = TransferRecordStatus.FAILED,
                        peerName = peerDeviceName
                    )
                    _transferState.value = TransferState.Failed(
                        reason = TransferFailureReason.CONNECTION_LOST,
                        detailedMessage = err.message ?: "File transfer was interrupted.",
                        peerDeviceName = peerDeviceName
                    )
                }
            )
        }
    }

    fun respondToIncomingTransferOffer(accepted: Boolean) {
        val offer = pendingIncomingOffer ?: return
        scope.launch {
            if (!accepted) {
                val frame = PacketProtocol.encodeControlMessage(
                    PacketProtocol.ControlMessage.TransferDecision(
                        batchId = offer.batchId,
                        accepted = false,
                        reason = "Declined by recipient"
                    )
                )
                webRtcClient.sendFrame(frame, isBinary = true)
                recordHistoryForBatch(
                    files = offer.files,
                    direction = TransferDirection.RECEIVED,
                    status = TransferRecordStatus.REJECTED,
                    peerName = offer.senderDeviceName
                )
                pendingIncomingOffer = null
                _transferState.value = TransferState.Cancelled(
                    cancelledByLocalUser = true,
                    peerDeviceName = offer.senderDeviceName
                )
                return@launch
            }

            if (!storageFileHelper.hasEnoughFreeSpace(offer.totalBytes)) {
                val frame = PacketProtocol.encodeControlMessage(
                    PacketProtocol.ControlMessage.TransferDecision(
                        batchId = offer.batchId,
                        accepted = false,
                        reason = "Recipient has insufficient free storage space"
                    )
                )
                webRtcClient.sendFrame(frame, isBinary = true)
                pendingIncomingOffer = null
                _transferState.value = TransferState.Failed(
                    reason = TransferFailureReason.INSUFFICIENT_STORAGE,
                    detailedMessage = "Not enough free storage space to receive ${offer.files.size} file(s).",
                    peerDeviceName = offer.senderDeviceName
                )
                return@launch
            }

            val settings = settingsRepository.settingsFlow.first()
            activeBatchId = offer.batchId
            connectedPeerName = offer.senderDeviceName
            transferStartTimestampMs = System.currentTimeMillis()

            val receiver = FileChunkReceiver(
                transport = webRtcClient,
                tempFileCreator = { fileId -> storageFileHelper.createTempTransferFile(fileId) },
                finalizeFileCallback = { tempFile, name, mime ->
                    storageFileHelper.finalizeReceivedFile(
                        tempFile = tempFile,
                        displayName = name,
                        mimeType = mime,
                        customSafTreeUriString = settings.defaultSaveDestinationUri
                    )
                },
                onProgress = { progress ->
                    _transferState.value = TransferState.Receiving(
                        peerDeviceName = offer.senderDeviceName,
                        progress = progress
                    )
                    TransferForegroundService.startOrUpdate(
                        context = appContext,
                        title = "Receiving ${progress.currentFileName}",
                        subtitle = "${progress.overallPercentage}% from ${offer.senderDeviceName}",
                        progressPercent = progress.overallPercentage
                    )
                }
            )
            receiver.initializeBatch(offer.batchId, offer.totalBytes)
            activeReceiver = receiver

            _transferState.value = TransferState.Receiving(
                peerDeviceName = offer.senderDeviceName,
                progress = TransferProgress(
                    batchId = offer.batchId,
                    currentFileIndex = 0,
                    totalFiles = offer.files.size,
                    currentFileName = offer.files.firstOrNull()?.name ?: "",
                    currentFileBytesTransferred = 0L,
                    currentFileTotalBytes = offer.files.firstOrNull()?.sizeBytes ?: 0L,
                    overallBytesTransferred = 0L,
                    overallTotalBytes = offer.totalBytes,
                    bytesPerSecond = 0L,
                    estimatedRemainingSeconds = null
                )
            )

            val acceptFrame = PacketProtocol.encodeControlMessage(
                PacketProtocol.ControlMessage.TransferDecision(
                    batchId = offer.batchId,
                    accepted = true
                )
            )
            webRtcClient.sendFrame(acceptFrame, isBinary = true)
        }
    }

    fun cancelActiveTransfer() {
        scope.launch {
            val batchId = activeBatchId
            if (!batchId.isNullOrBlank() && webRtcClient.isOpen()) {
                val cancelFrame = PacketProtocol.encodeControlMessage(
                    PacketProtocol.ControlMessage.CancelTransfer(
                        batchId = batchId,
                        reason = "Cancelled by user"
                    )
                )
                webRtcClient.sendFrame(cancelFrame, isBinary = true)
            }
            activeTransferJob?.cancel()
            activeTransferJob = null
            activeSender = null
            activeReceiver?.cleanupIncomplete()
            activeReceiver = null
            storageFileHelper.cleanupTempFiles()
            TransferForegroundService.stop(appContext)
            _transferState.value = TransferState.Cancelled(
                cancelledByLocalUser = true,
                peerDeviceName = connectedPeerName
            )
        }
    }

    fun onDataChannelFrameReceived(bytes: ByteArray) {
        scope.launch {
            try {
                when (val decoded = PacketProtocol.decodeFrame(bytes)) {
                    is PacketProtocol.DecodedFrame.Control -> handleControlMessage(decoded.message)
                    is PacketProtocol.DecodedFrame.Chunk -> {
                        val res = activeReceiver?.onChunkReceived(decoded.chunkFrame)
                        if (res != null && res.isFailure) {
                            handleReceiveFailure(
                                TransferFailureReason.UNKNOWN_ERROR,
                                res.exceptionOrNull()?.message ?: "File write error"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("TransferCoordinator", "Failed to process incoming packet", e)
            }
        }
    }

    fun onPeerDisconnectedUnexpectedly() {
        val currentState = _transferState.value
        if (currentState is TransferState.Sending ||
            currentState is TransferState.Receiving ||
            currentState is TransferState.WaitingForReceiverApproval
        ) {
            activeTransferJob?.cancel()
            activeSender = null
            activeReceiver?.cleanupIncomplete()
            activeReceiver = null
            storageFileHelper.cleanupTempFiles()
            TransferForegroundService.stop(appContext)
            _transferState.value = TransferState.Failed(
                reason = TransferFailureReason.CONNECTION_LOST,
                detailedMessage = "Connection was interrupted before the transfer completed. Both users must keep the app open at the same time.",
                peerDeviceName = connectedPeerName
            )
        }
    }

    private suspend fun handleControlMessage(msg: PacketProtocol.ControlMessage) {
        when (msg) {
            is PacketProtocol.ControlMessage.TransferOffer -> {
                pendingIncomingOffer = msg.offer
                connectedPeerName = msg.offer.senderDeviceName
                val settings = settingsRepository.settingsFlow.first()
                if (settings.autoAcceptFromAuthorizedPeer) {
                    respondToIncomingTransferOffer(accepted = true)
                } else {
                    _transferState.value = TransferState.IncomingTransferPrompt(msg.offer)
                }
            }
            is PacketProtocol.ControlMessage.TransferDecision -> {
                pendingReceiverDecision?.complete(msg)
            }
            is PacketProtocol.ControlMessage.FileStart -> {
                val res = activeReceiver?.onFileStart(msg)
                if (res != null && res.isFailure) {
                    handleReceiveFailure(
                        TransferFailureReason.FILE_PERMISSION_ERROR,
                        res.exceptionOrNull()?.message ?: "Failed to create destination file"
                    )
                }
            }
            is PacketProtocol.ControlMessage.FileEnd -> {
                val receiver = activeReceiver ?: return
                val res = receiver.onFileEnd(msg)
                res.fold(
                    onSuccess = { completedFile ->
                        historyRepository.insertRecord(
                            TransferHistoryRecord(
                                fileName = completedFile.name,
                                fileSizeBytes = completedFile.sizeBytes,
                                mimeType = completedFile.mimeType,
                                direction = TransferDirection.RECEIVED,
                                timestamp = System.currentTimeMillis(),
                                status = TransferRecordStatus.COMPLETED,
                                peerDisplayName = connectedPeerName,
                                sha256Checksum = completedFile.sha256Digest,
                                savedFileUri = completedFile.savedUri.toString()
                            )
                        )
                    },
                    onFailure = { err ->
                        handleReceiveFailure(
                            TransferFailureReason.CHECKSUM_MISMATCH,
                            err.message ?: "Received file failed integrity verification"
                        )
                    }
                )
            }
            is PacketProtocol.ControlMessage.FileVerifiedAck -> {
                activeSender?.onFileVerifiedAckReceived(msg)
            }
            is PacketProtocol.ControlMessage.BatchComplete -> {
                val completedFiles: List<CompletedReceivedFile> = activeReceiver?.getCompletedFiles().orEmpty()
                activeReceiver = null
                pendingIncomingOffer = null
                TransferForegroundService.stop(appContext)
                val durationMs = (System.currentTimeMillis() - transferStartTimestampMs).coerceAtLeast(1L)
                val avgSpeed = (msg.totalBytes * 1000L) / durationMs
                _transferState.value = TransferState.Completed(
                    isSender = false,
                    peerDeviceName = connectedPeerName,
                    totalFiles = msg.totalFiles,
                    totalBytes = msg.totalBytes,
                    durationMs = durationMs,
                    averageSpeedBytesPerSec = avgSpeed,
                    receivedFiles = completedFiles
                )
            }
            is PacketProtocol.ControlMessage.CancelTransfer -> {
                activeTransferJob?.cancel()
                activeSender = null
                activeReceiver?.cleanupIncomplete()
                activeReceiver = null
                storageFileHelper.cleanupTempFiles()
                TransferForegroundService.stop(appContext)
                _transferState.value = TransferState.Cancelled(
                    cancelledByLocalUser = false,
                    peerDeviceName = connectedPeerName
                )
            }
        }
    }

    private fun handleReceiveFailure(reason: TransferFailureReason, message: String) {
        activeReceiver?.cleanupIncomplete()
        activeReceiver = null
        storageFileHelper.cleanupTempFiles()
        TransferForegroundService.stop(appContext)
        _transferState.value = TransferState.Failed(
            reason = reason,
            detailedMessage = message,
            peerDeviceName = connectedPeerName
        )
    }

    private suspend fun recordHistoryForBatch(
        files: List<IncomingFileDescriptor>,
        direction: TransferDirection,
        status: TransferRecordStatus,
        peerName: String
    ) {
        val now = System.currentTimeMillis()
        for (f in files) {
            historyRepository.insertRecord(
                TransferHistoryRecord(
                    fileName = f.name,
                    fileSizeBytes = f.sizeBytes,
                    mimeType = f.mimeType,
                    direction = direction,
                    timestamp = now,
                    status = status,
                    peerDisplayName = peerName,
                    sha256Checksum = f.sha256Checksum
                )
            )
        }
    }
}
