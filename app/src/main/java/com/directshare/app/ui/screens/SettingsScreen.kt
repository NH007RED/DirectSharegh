package com.directshare.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.directshare.app.domain.model.AppSettings
import com.directshare.app.domain.model.ThemePreference
import com.directshare.app.ui.components.AnimatedGradientButton
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.components.GlassSegmentedSelector
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepBlack
import com.directshare.app.ui.theme.GlassBorderCrimson
import com.directshare.app.ui.theme.GlassBorderSubtle
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateDeviceName: (String) -> Unit,
    onUpdateSaveDestination: (Uri?) -> Unit,
    onUpdateTheme: (ThemePreference) -> Unit,
    onUpdateSound: (Boolean) -> Unit,
    onUpdateVibration: (Boolean) -> Unit,
    onUpdateAutoAccept: (Boolean) -> Unit,
    onUpdateMaxConcurrent: (Int) -> Unit,
    onUpdateChunkSize: (Int) -> Unit,
    onUpdateTurnServer: (String, String, String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenPrivacyInfo: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onDisconnectAllSessions: () -> Unit
) {
    var deviceNameInput by remember(settings.deviceDisplayName) { mutableStateOf(settings.deviceDisplayName) }
    var turnUrlInput by remember(settings.customTurnUrl) { mutableStateOf(settings.customTurnUrl) }
    var turnUserInput by remember(settings.customTurnUsername) { mutableStateOf(settings.customTurnUsername) }
    var turnPassInput by remember(settings.customTurnCredential) { mutableStateOf(settings.customTurnCredential) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateSaveDestination(uri)
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedBorderColor = CrimsonRed,
        unfocusedBorderColor = GlassBorderSubtle,
        focusedLabelColor = CrimsonRed,
        unfocusedLabelColor = TextSecondary,
        cursorColor = CrimsonRed,
        focusedContainerColor = DeepBlack.copy(alpha = 0.65f),
        unfocusedContainerColor = DeepBlack.copy(alpha = 0.45f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Customize your device name, save folder, transfer speed, and app preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Device Identity & Save Folder
        GlassCard(
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Device Display Name",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = deviceNameInput,
                onValueChange = { deviceNameInput = it },
                label = { Text("Your name shown to the other user") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            AnimatedGradientButton(
                text = "Save Display Name",
                icon = Icons.Default.Save,
                onClick = { onUpdateDeviceName(deviceNameInput) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Default Save Destination",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = settings.defaultSaveDestinationUri ?: "Downloads/DirectShare (Default Folder)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedGradientButton(
                    text = "Choose Save Folder",
                    icon = Icons.Default.Folder,
                    secondaryStyle = true,
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.weight(1f)
                )
                if (settings.defaultSaveDestinationUri != null) {
                    TextButton(onClick = { onUpdateSaveDestination(null) }) {
                        Text("Reset", color = CrimsonRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Theme Appearance & Transfer Alerts
        GlassCard(
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Theme Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            GlassSegmentedSelector(
                options = ThemePreference.entries.toList(),
                selectedOption = settings.themePreference,
                labelProvider = { it.name },
                onOptionSelected = onUpdateTheme
            )

            Spacer(modifier = Modifier.height(12.dp))
            CompactSettingSwitchRow(
                title = "Transfer Sound Effects",
                subtitle = "Chime on transfer completion",
                checked = settings.soundEnabled,
                onCheckedChange = onUpdateSound
            )
            CompactSettingSwitchRow(
                title = "Haptic Vibration",
                subtitle = "Vibrate on incoming transfer prompt",
                checked = settings.vibrationEnabled,
                onCheckedChange = onUpdateVibration
            )
            CompactSettingSwitchRow(
                title = "Auto-Accept from Paired Device",
                subtitle = "Disabled by default so every transfer requires confirmation",
                checked = settings.autoAcceptFromAuthorizedPeer,
                onCheckedChange = onUpdateAutoAccept
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Transfer Speed Packet Size & Concurrency
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
                    text = "Transfer Speed Packet Size",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${settings.chunkSizeBytes / 1024} KB",
                    style = MaterialTheme.typography.labelLarge,
                    color = CrimsonRed,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            GlassSegmentedSelector(
                options = listOf(16 * 1024, 64 * 1024, 128 * 1024, 256 * 1024),
                selectedOption = settings.chunkSizeBytes,
                labelProvider = { "${it / 1024} KB" },
                onOptionSelected = onUpdateChunkSize
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Max Simultaneous Transfers",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${settings.maxConcurrentTransfers}",
                    style = MaterialTheme.typography.labelLarge,
                    color = CrimsonRed,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            GlassSegmentedSelector(
                options = listOf(1, 2, 3),
                selectedOption = settings.maxConcurrentTransfers,
                labelProvider = { "$it Stream${if (it > 1) "s" else ""}" },
                onOptionSelected = onUpdateMaxConcurrent
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Advanced Connection Boost (Optional)
        GlassCard(
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Advanced Connection Boost (Optional)",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Optional custom connection boost server for restricted mobile or office networks.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = turnUrlInput,
                onValueChange = { turnUrlInput = it },
                label = { Text("Boost Server Address (Optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = turnUserInput,
                    onValueChange = { turnUserInput = it },
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = turnPassInput,
                    onValueChange = { turnPassInput = it },
                    label = { Text("Access Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            AnimatedGradientButton(
                text = "Save Connection Boost",
                icon = Icons.Default.Save,
                secondaryStyle = true,
                onClick = { onUpdateTurnServer(turnUrlInput, turnUserInput, turnPassInput) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Responsive 2x2 Utility Grid
        GlassCard(
            contentPadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedGradientButton(
                    text = "System Status",
                    icon = Icons.Default.NetworkCheck,
                    onClick = onOpenDiagnostics,
                    modifier = Modifier.weight(1f)
                )
                AnimatedGradientButton(
                    text = "Privacy Guide",
                    icon = Icons.Default.Lock,
                    secondaryStyle = true,
                    onClick = onOpenPrivacyInfo,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedGradientButton(
                    text = "Clear History",
                    icon = Icons.Default.DeleteForever,
                    secondaryStyle = true,
                    onClick = onClearHistory,
                    modifier = Modifier.weight(1f)
                )
                AnimatedGradientButton(
                    text = "Disconnect All",
                    icon = Icons.Default.LinkOff,
                    secondaryStyle = true,
                    onClick = onDisconnectAllSessions,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CompactSettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CrimsonRed,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DeepBlack.copy(alpha = 0.85f),
                uncheckedBorderColor = GlassBorderCrimson.copy(alpha = 0.45f)
            )
        )
    }
}
