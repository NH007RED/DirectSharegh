package com.directshare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.directshare.app.domain.model.PeerDevice
import com.directshare.app.domain.model.SignalingSession
import com.directshare.app.domain.model.TransferState
import com.directshare.app.ui.components.AnimatedConnectionRing
import com.directshare.app.ui.components.AnimatedGradientButton
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.components.GlassSegmentedSelector
import com.directshare.app.ui.components.GlowingQrCodeBox
import com.directshare.app.ui.components.StatusPill
import com.directshare.app.ui.theme.AmberWarning
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepBlack
import com.directshare.app.ui.theme.GlassBorderSubtle
import com.directshare.app.ui.theme.NeonEmerald
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary

@Composable
fun DiscoveryPairingScreen(
    onlinePeers: List<PeerDevice>,
    activeSession: SignalingSession?,
    isHost: Boolean,
    transferState: TransferState,
    onStartHosting: () -> Unit,
    onJoinByCode: (String) -> Unit,
    onConnectPeer: (PeerDevice) -> Unit,
    onAuthorizeGuest: (Boolean) -> Unit,
    onDisconnect: () -> Unit
) {
    var pairingInput by remember { mutableStateOf("") }
    var showMyQrMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Scan or Enter Code to Pair",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Both users must keep the app open at the same time to connect",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Mode Switcher: "Host / My QR Code" vs "Join with Code"
        GlassSegmentedSelector(
            options = listOf(true, false),
            selectedOption = showMyQrMode,
            labelProvider = { if (it) "Host / My QR Code" else "Join with Code" },
            onOptionSelected = { showMyQrMode = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (transferState is TransferState.WaitingForAuthorization) {
            GlassCard(
                accentBorder = true,
                glowTint = CrimsonRed,
                contentPadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (transferState.isHost) {
                    Text(
                        text = "Approve Incoming Device",
                        style = MaterialTheme.typography.titleLarge,
                        color = CrimsonRed,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "'${transferState.guestDeviceName}' wants to connect with code ${activeSession?.pairingCode}. Allow connection?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnimatedGradientButton(
                            text = "Reject",
                            icon = Icons.Default.Close,
                            secondaryStyle = true,
                            onClick = { onAuthorizeGuest(false) },
                            modifier = Modifier.weight(1f)
                        )
                        AnimatedGradientButton(
                            text = "Approve",
                            icon = Icons.Default.Check,
                            onClick = { onAuthorizeGuest(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedConnectionRing(
                            size = 148.dp,
                            isConnected = false,
                            centerLabel = "Waiting",
                            subLabel = "Approval"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Waiting for '${transferState.guestDeviceName}' to approve your connection request...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmberWarning,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        GlassCard(
            accentBorder = true,
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showMyQrMode) {
                    if (activeSession != null) {
                        GlowingQrCodeBox(
                            sessionId = activeSession.sessionId,
                            pairingCode = activeSession.pairingCode,
                            boxSize = 190.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeSession.pairingCode.forEach { ch ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .size(width = 36.dp, height = 42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CrimsonRed.copy(alpha = 0.16f))
                                        .border(1.dp, CrimsonRed.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ch.toString(),
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        StatusPill(
                            text = activeSession.status.name,
                            color = if (transferState is TransferState.PeerConnectedReady) NeonEmerald else CrimsonRed
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedGradientButton(
                            text = "Disconnect Session",
                            icon = Icons.Default.LinkOff,
                            secondaryStyle = true,
                            onClick = onDisconnect,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = CrimsonRed,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generate a 6-digit pairing code and QR code for the other phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedGradientButton(
                            text = "Generate Code & QR",
                            icon = Icons.Default.WifiTethering,
                            onClick = onStartHosting,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = "Enter Other Phone's 6-Digit Code",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pairingInput,
                        onValueChange = { pairingInput = it.uppercase() },
                        label = { Text("e.g. K7M9P2") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CrimsonRed,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedLabelColor = CrimsonRed,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = CrimsonRed,
                            focusedContainerColor = DeepBlack.copy(alpha = 0.65f),
                            unfocusedContainerColor = DeepBlack.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedGradientButton(
                        text = "Connect with Code",
                        icon = Icons.Default.Link,
                        enabled = pairingInput.trim().length >= 6,
                        onClick = { onJoinByCode(pairingInput) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Online Devices",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                StatusPill(text = "${onlinePeers.size} Active", color = CrimsonRed)
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (onlinePeers.isEmpty()) {
                Text(
                    text = "No other phones are currently active. Open DirectShare on both phones at the same time to connect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                onlinePeers.forEach { peer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CrimsonRed.copy(alpha = 0.16f))
                                    .border(1.dp, CrimsonRed.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peer.deviceName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (peer.activeSessionId != null) "Ready to Pair" else "Online (Standby)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (peer.activeSessionId != null) NeonEmerald else TextSecondary
                                )
                            }
                        }
                        if (peer.activeSessionId != null) {
                            TextButton(onClick = { onConnectPeer(peer) }) {
                                Text("Connect", color = CrimsonRed, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
