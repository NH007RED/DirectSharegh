package com.directshare.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.directshare.app.domain.model.ConnectionDiagnostics
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.theme.CoralError
import com.directshare.app.ui.theme.CrimsonRed

@Composable
fun DiagnosticsScreen(
    diagnostics: ConnectionDiagnostics,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CrimsonRed)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "System & Connection Status",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            accentBorder = true,
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            DiagRow("Anonymous Session", if (diagnostics.firebaseAuthenticated) "Active" else "Standby")
            DiagRow("Anonymous Device Tag", diagnostics.firebaseUid?.take(10) ?: "None")
            DiagRow("Global Network Status", if (diagnostics.signalingConnected) "ONLINE" else "OFFLINE")
            DiagRow("Active Pairing Room", diagnostics.activeSessionId?.take(12) ?: "None")
            DiagRow("Route State", diagnostics.iceConnectionState)
            DiagRow("Device Link State", diagnostics.peerConnectionState)
            DiagRow("Transfer Channel", diagnostics.dataChannelState)
            DiagRow("Local Network Paths", "${diagnostics.localCandidatesCount}")
            DiagRow("Remote Network Paths", "${diagnostics.remoteCandidatesCount}")
            DiagRow("Connection Mode", diagnostics.selectedCandidatePairType)
            DiagRow("Connection Boost", if (diagnostics.turnConfigured) "Enabled" else "Standard Direct")

            if (!diagnostics.lastError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Recent Notice: ${diagnostics.lastError}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoralError
                )
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.48f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = CrimsonRed,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.52f)
        )
    }
}
