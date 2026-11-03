package com.directshare.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.directshare.app.domain.model.TransferProgress
import com.directshare.app.domain.model.TransferState
import com.directshare.app.ui.components.AnimatedConnectionRing
import com.directshare.app.ui.components.AnimatedGradientButton
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.components.PeerToPeerWaveBridge
import com.directshare.app.ui.components.StatusPill
import com.directshare.app.ui.components.TransferProgressGauge
import com.directshare.app.ui.theme.AmberWarning
import com.directshare.app.ui.theme.CoralError
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepRuby
import com.directshare.app.ui.theme.GlassBorderSubtle
import com.directshare.app.ui.theme.NeonEmerald
import com.directshare.app.ui.theme.ScarletRed
import com.directshare.app.utils.FormatUtils

@Composable
fun ActiveTransferScreen(
    transferState: TransferState,
    onCancelTransfer: () -> Unit,
    onAcceptIncoming: () -> Unit,
    onDeclineIncoming: () -> Unit,
    onOpenFile: (Uri, String) -> Unit,
    onShareFile: (Uri, String, String) -> Unit,
    onDoneOrRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (transferState) {
            is TransferState.FindingDevices -> {
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedConnectionRing(
                    size = 200.dp,
                    isConnected = false,
                    centerLabel = "Searching",
                    subLabel = "${transferState.onlinePeers.size} Active"
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Finding Active Devices...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            is TransferState.Connecting -> {
                Text(
                    text = "Connecting...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transferState.peerDeviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CrimsonRed,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                PeerToPeerWaveBridge(peerName = transferState.peerDeviceName, isSending = true)
                Spacer(modifier = Modifier.height(20.dp))
                AnimatedConnectionRing(
                    size = 188.dp,
                    isConnected = false,
                    centerLabel = "Linking",
                    subLabel = "Securing"
                )
                Spacer(modifier = Modifier.height(18.dp))
                GlassCard(accentBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = transferState.stepDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is TransferState.WaitingForReceiverApproval -> {
                Text(
                    text = "Waiting for Receiver...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transferState.peerDeviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CrimsonRed,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                PeerToPeerWaveBridge(peerName = transferState.peerDeviceName, isSending = true)
                Spacer(modifier = Modifier.height(20.dp))
                AnimatedConnectionRing(
                    size = 185.dp,
                    isConnected = true,
                    centerLabel = "Pending",
                    subLabel = "${transferState.fileCount} file(s)"
                )
                Spacer(modifier = Modifier.height(18.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Requested to send ${transferState.fileCount} file(s) (${FormatUtils.formatBytes(transferState.totalBytes)}) to ${transferState.peerDeviceName}. Waiting for recipient to tap Accept.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                AnimatedGradientButton(
                    text = "Cancel Request",
                    icon = Icons.Default.Cancel,
                    secondaryStyle = true,
                    onClick = onCancelTransfer,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is TransferState.IncomingTransferPrompt -> {
                ReceiveFilesScreen(
                    activeSession = null,
                    transferState = transferState,
                    onStartHostingSession = {},
                    onNavigatePairing = {},
                    onAcceptTransfer = onAcceptIncoming,
                    onDeclineTransfer = onDeclineIncoming
                )
            }

            is TransferState.Sending -> {
                ShowcaseLiveTransferLayout(
                    headline = "Transferring...",
                    peerName = transferState.peerDeviceName,
                    isSending = true,
                    progress = transferState.progress,
                    onCancel = onCancelTransfer
                )
            }

            is TransferState.Receiving -> {
                ShowcaseLiveTransferLayout(
                    headline = "Receiving...",
                    peerName = transferState.peerDeviceName,
                    isSending = false,
                    progress = transferState.progress,
                    onCancel = onCancelTransfer
                )
            }

            is TransferState.Completed -> {
                Spacer(modifier = Modifier.height(12.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Transfer Completed",
                    tint = NeonEmerald,
                    modifier = Modifier.size(84.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Transfer Completed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusPill(text = "100% Verified & Complete", color = NeonEmerald)
                Spacer(modifier = Modifier.height(16.dp))

                GlassCard(
                    accentBorder = true,
                    glowTint = NeonEmerald,
                    contentPadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (transferState.isSender) {
                            "Sent to ${transferState.peerDeviceName}"
                        } else {
                            "Received from ${transferState.peerDeviceName}"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryColumn(
                            label = "Total Size",
                            value = FormatUtils.formatBytes(transferState.totalBytes),
                            modifier = Modifier.weight(1f)
                        )
                        VerticalGlassDivider()
                        TelemetryColumn(
                            label = "Files",
                            value = "${transferState.totalFiles}",
                            modifier = Modifier.weight(1f)
                        )
                        VerticalGlassDivider()
                        TelemetryColumn(
                            label = "Avg Speed",
                            value = FormatUtils.formatSpeed(transferState.averageSpeedBytesPerSec),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (transferState.receivedFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Saved Files:",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonRed
                        )
                        transferState.receivedFiles.forEach { file ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Verified • Saved on device",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonEmerald
                                    )
                                }
                                Row {
                                    TextButton(onClick = { onOpenFile(file.savedUri, file.mimeType) }) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp), tint = CrimsonRed)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open", color = CrimsonRed, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = { onShareFile(file.savedUri, file.mimeType, file.name) }) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = CrimsonRed)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", color = CrimsonRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                AnimatedGradientButton(
                    text = "Done",
                    icon = Icons.Default.CheckCircle,
                    onClick = onDoneOrRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is TransferState.Failed -> {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Transfer Failed",
                    tint = CoralError,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Transfer Failed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                StatusPill(text = transferState.reason.displayTitle, color = CoralError)
                Spacer(modifier = Modifier.height(16.dp))

                GlassCard(
                    glowTint = CoralError,
                    contentPadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = transferState.detailedMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Incomplete temporary files were automatically cleaned up. Keep both phones open at the same time to restart the transfer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                AnimatedGradientButton(
                    text = "Back / Retry",
                    icon = Icons.Default.Refresh,
                    onClick = onDoneOrRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is TransferState.Cancelled -> {
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Transfer Cancelled",
                    tint = AmberWarning,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Transfer Cancelled",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                GlassCard(
                    contentPadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (transferState.cancelledByLocalUser) {
                            "You cancelled the active transfer. Temporary files have been removed."
                        } else {
                            "${transferState.peerDeviceName ?: "The other user"} cancelled the transfer. Temporary files have been removed."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                AnimatedGradientButton(
                    text = "Return to Dashboard",
                    icon = Icons.Default.Refresh,
                    onClick = onDoneOrRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "No Active Transfer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedGradientButton(
                    text = "Return Home",
                    onClick = onDoneOrRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ShowcaseLiveTransferLayout(
    headline: String,
    peerName: String,
    isSending: Boolean,
    progress: TransferProgress,
    onCancel: () -> Unit
) {
    Text(
        text = headline,
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = peerName,
        style = MaterialTheme.typography.bodyLarge,
        color = CrimsonRed,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(12.dp))

    PeerToPeerWaveBridge(
        peerName = peerName,
        isSending = isSending
    )

    Spacer(modifier = Modifier.height(14.dp))

    GlassCard(
        accentBorder = true,
        contentPadding = 18.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TransferProgressGauge(
                fraction = progress.overallFraction,
                percentageText = "${progress.overallPercentage}%",
                speedText = FormatUtils.formatSpeed(progress.bytesPerSecond),
                size = 192.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelemetryColumn(
                    label = "of ${FormatUtils.formatBytes(progress.overallTotalBytes)}",
                    value = FormatUtils.formatBytes(progress.overallBytesTransferred),
                    modifier = Modifier.weight(1f)
                )
                VerticalGlassDivider()
                TelemetryColumn(
                    label = "Speed",
                    value = FormatUtils.formatSpeed(progress.bytesPerSecond),
                    modifier = Modifier.weight(1f)
                )
                VerticalGlassDivider()
                TelemetryColumn(
                    label = "Est. Time",
                    value = FormatUtils.formatRemainingTime(progress.estimatedRemainingSeconds),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    GlassCard(
        cornerRadius = 18.dp,
        contentPadding = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(CrimsonRed.copy(alpha = 0.18f))
                    .border(1.dp, CrimsonRed.copy(alpha = 0.55f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = CrimsonRed
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = progress.currentFileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${progress.currentFileIndex + 1}/${progress.totalFiles}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.currentFileFraction.coerceIn(0.02f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CrimsonRed, ScarletRed, DeepRuby)
                                )
                            )
                    )
                }
            }
        }

        if (progress.isPausedForBackpressure) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Optimizing transfer stream (${FormatUtils.formatBytes(progress.bufferedAmountBytes)})",
                style = MaterialTheme.typography.labelSmall,
                color = AmberWarning
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    AnimatedGradientButton(
        text = "Cancel Transfer",
        icon = Icons.Default.Cancel,
        secondaryStyle = true,
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VerticalGlassDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .width(1.dp)
            .background(GlassBorderSubtle)
    )
}

@Composable
private fun TelemetryColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
