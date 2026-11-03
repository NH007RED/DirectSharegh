package com.directshare.app.ui.screens

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
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.directshare.app.domain.model.PeerDevice
import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.domain.model.TransferHistoryRecord
import com.directshare.app.domain.model.TransferState
import com.directshare.app.domain.model.TransferStatisticsSummary
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.components.HeroGlassActionTile
import com.directshare.app.ui.components.OrbitalPeerRadar
import com.directshare.app.ui.components.StatusPill
import com.directshare.app.ui.components.TransferSparklineChart
import com.directshare.app.ui.theme.AmberWarning
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepRuby
import com.directshare.app.ui.theme.NeonEmerald
import com.directshare.app.ui.theme.RoseNeon
import com.directshare.app.ui.theme.ScarletRed
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary
import com.directshare.app.utils.FormatUtils

@Composable
fun HomeScreen(
    deviceName: String,
    firebaseConnected: Boolean,
    activeSession: SignalingSession?,
    transferState: TransferState,
    onlinePeers: List<PeerDevice>,
    recentTransfers: List<TransferHistoryRecord>,
    statistics: TransferStatisticsSummary,
    onNavigateSend: () -> Unit,
    onNavigateReceive: () -> Unit,
    onNavigateConnect: () -> Unit,
    onNavigateActiveTransfer: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
    onDisconnectSession: () -> Unit
) {
    val isPeerConnected = transferState is TransferState.PeerConnectedReady
    val connectedPeerName = (transferState as? TransferState.PeerConnectedReady)?.peerDeviceName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Flagship Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                            append("Direct")
                        }
                        withStyle(SpanStyle(color = CrimsonRed, fontWeight = FontWeight.Black)) {
                            append("Share")
                        }
                    },
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = if (firebaseConnected) "Online" else "Offline",
                    color = if (firebaseConnected) NeonEmerald else AmberWarning
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onNavigateDiagnostics,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = "Connection Status",
                        tint = CrimsonRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Orbital Peer Radar
        OrbitalPeerRadar(
            peers = onlinePeers,
            isConnected = isPeerConnected || activeSession != null,
            centerLabel = when {
                isPeerConnected -> "Linked"
                activeSession != null -> activeSession.pairingCode
                else -> "Radar"
            },
            subLabel = when {
                connectedPeerName != null -> connectedPeerName.take(12)
                activeSession != null -> "Hosting"
                else -> "${onlinePeers.size} Active"
            },
            onPeerClicked = { onNavigateConnect() },
            radarSize = 188.dp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Equal-Height Side-by-Side Hero Action Tiles: Quick Send & Receive
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroGlassActionTile(
                title = "Quick Send",
                subtitle = "Select & Share",
                icon = Icons.Default.Send,
                primaryTint = CrimsonRed,
                secondaryTint = ScarletRed,
                onClick = onNavigateSend,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            HeroGlassActionTile(
                title = "Receive",
                subtitle = activeSession?.pairingCode ?: "Code / QR",
                icon = Icons.Default.CallReceived,
                primaryTint = RoseNeon,
                secondaryTint = DeepRuby,
                onClick = onNavigateReceive,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Session / Pairing Banner Card
        GlassCard(
            cornerRadius = 18.dp,
            contentPadding = 14.dp,
            accentBorder = isPeerConnected || activeSession != null,
            glowTint = if (isPeerConnected) NeonEmerald else CrimsonRed,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (transferState is TransferState.Sending ||
                    transferState is TransferState.Receiving ||
                    transferState is TransferState.IncomingTransferPrompt
                ) {
                    onNavigateActiveTransfer()
                } else {
                    onNavigateConnect()
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CrimsonRed.copy(alpha = 0.25f), DeepRuby.copy(alpha = 0.22f))
                                )
                            )
                            .border(1.dp, CrimsonRed.copy(alpha = 0.65f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeSession != null) Icons.Default.QrCode2 else Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = CrimsonRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                connectedPeerName != null -> "Connected to $connectedPeerName"
                                activeSession != null -> "Pairing Code: ${activeSession.pairingCode}"
                                else -> "Pair or Scan QR to Connect"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                connectedPeerName != null -> "Secure Direct Link Active • Tap to view"
                                activeSession != null -> "Status: ${activeSession.status.name} • Tap for QR"
                                else -> "${onlinePeers.size} active device(s) ready to connect"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (connectedPeerName != null) NeonEmerald else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (activeSession != null || isPeerConnected) {
                    IconButton(
                        onClick = onDisconnectSession,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = "Disconnect Session",
                            tint = AmberWarning,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Synchronized Intrinsic-Height Bento Statistics & Recent Shares Grid
        val totalSharedBytes = statistics.totalBytesSent + statistics.totalBytesReceived
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.06f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    cornerRadius = 18.dp,
                    contentPadding = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TOTAL SHARED",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatBytes(totalSharedBytes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "↑ ${FormatUtils.formatBytes(statistics.totalBytesSent)} • ↓ ${FormatUtils.formatBytes(statistics.totalBytesReceived)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                GlassCard(
                    cornerRadius = 18.dp,
                    contentPadding = 14.dp,
                    onClick = onNavigateHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Shares",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelSmall,
                            color = CrimsonRed
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (recentTransfers.isEmpty()) {
                        Text(
                            text = "No transfers recorded yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        recentTransfers.take(2).forEach { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rec.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Right Bento Card: Transfers Done + Sparkline Chart matched to exact left-column height
            GlassCard(
                cornerRadius = 18.dp,
                contentPadding = 14.dp,
                modifier = Modifier
                    .weight(0.94f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "TRANSFERS DONE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${statistics.successfulTransfersCount}",
                    style = MaterialTheme.typography.displayMedium,
                    color = CrimsonRed,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${statistics.totalSentCount} sent • ${statistics.totalReceivedCount} rcvd",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(8.dp))
                TransferSparklineChart(
                    dataPoints = recentTransfers
                        .take(6)
                        .map { (it.fileSizeBytes.toFloat() / (1024f * 1024f)).coerceAtLeast(0.15f) }
                        .reversed(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("M", "T", "W", "T", "F").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
