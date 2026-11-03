package com.directshare.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.theme.CrimsonRed

@Composable
fun PrivacySecurityScreen(onBack: () -> Unit) {
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
                text = "Privacy & Anonymous Sharing",
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
            Text(
                text = "1. Share Anywhere in the World Anonymously",
                style = MaterialTheme.typography.titleMedium,
                color = CrimsonRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "You can send photos, videos, documents, and large files anywhere in the world anonymously without creating an account, entering a phone number, or revealing your identity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "2. Both Users Must Open the App Together",
                style = MaterialTheme.typography.titleMedium,
                color = CrimsonRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "To connect and transfer files, both users (sender and receiver) must keep the DirectShare app open at the same time on their phones and pair using a temporary 6-digit code or QR code.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "3. Zero Cloud Storage & Explicit Approval",
                style = MaterialTheme.typography.titleMedium,
                color = CrimsonRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your files move directly from one phone to the other and are never stored on any server. Every connection and incoming file transfer requires explicit approval before anything is saved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
