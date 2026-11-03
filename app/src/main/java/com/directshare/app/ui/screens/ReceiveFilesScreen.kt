package com.directshare.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.domain.model.TransferState
import com.directshare.app.ui.components.AnimatedConnectionRing
import com.directshare.app.ui.components.AnimatedGradientButton
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.components.QrCodeDialog
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.NeonEmerald
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary
import com.directshare.app.utils.FormatUtils

@Composable
fun ReceiveFilesScreen(
    activeSession: SignalingSession?,
    transferState: TransferState,
    onStartHostingSession: () -> Unit,
    onNavigatePairing: () -> Unit,
    onAcceptTransfer: () -> Unit,
    onDeclineTransfer: () -> Unit
) {
    var showQrModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Receive Files",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Explicit confirmation required before any incoming file is saved.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (transferState) {
            is TransferState.IncomingTransferPrompt -> {
                val offer = transferState.offer
                GlassCard(
                    accentBorder = true,
                    glowTint = CrimsonRed,
                    contentPadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Incoming Transfer Request",
                        style = MaterialTheme.typography.titleLarge,
                        color = CrimsonRed,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sender: ${offer.senderDeviceName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${offer.files.size} file(s) • Total Size: ${FormatUtils.formatBytes(offer.totalBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    offer.files.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = FormatUtils.formatBytes(file.sizeBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedGradientButton(
                            text = "Decline",
                            icon = Icons.Default.Close,
                            secondaryStyle = true,
                            onClick = onDeclineTransfer,
                            modifier = Modifier.weight(1f)
                        )
                        AnimatedGradientButton(
                            text = "Accept",
                            icon = Icons.Default.Check,
                            onClick = onAcceptTransfer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            is TransferState.PeerConnectedReady -> {
                AnimatedConnectionRing(
                    size = 180.dp,
                    isConnected = true,
                    centerLabel = "Ready",
                    subLabel = transferState.peerDeviceName
                )
                Spacer(modifier = Modifier.height(16.dp))
                GlassCard(
                    accentBorder = true,
                    glowTint = NeonEmerald,
                    contentPadding = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Connected to ${transferState.peerDeviceName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonEmerald,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Secure direct connection is active. Ask ${transferState.peerDeviceName} to tap 'Send' on their phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            else -> {
                AnimatedConnectionRing(
                    size = 180.dp,
                    isConnected = activeSession != null,
                    centerLabel = activeSession?.pairingCode ?: "Standby",
                    subLabel = if (activeSession != null) "Session Active" else "Not Paired"
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (activeSession != null) {
                    GlassCard(
                        accentBorder = true,
                        contentPadding = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pairing Code: ${activeSession.pairingCode}",
                            style = MaterialTheme.typography.titleLarge,
                            color = CrimsonRed,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter this 6-digit code on the other phone (with both apps open at the same time) to connect instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedGradientButton(
                            text = "Show QR Code",
                            icon = Icons.Default.QrCode2,
                            onClick = { showQrModal = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    AnimatedGradientButton(
                        text = "Host Receive Session",
                        icon = Icons.Default.WifiTethering,
                        onClick = onStartHostingSession,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    AnimatedGradientButton(
                        text = "Enter Sender's Pairing Code",
                        icon = Icons.Default.QrCode2,
                        secondaryStyle = true,
                        onClick = onNavigatePairing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showQrModal && activeSession != null) {
        QrCodeDialog(
            sessionId = activeSession.sessionId,
            pairingCode = activeSession.pairingCode,
            onDismiss = { showQrModal = false }
        )
    }
}
