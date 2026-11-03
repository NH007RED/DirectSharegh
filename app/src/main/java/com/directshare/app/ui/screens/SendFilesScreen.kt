package com.directshare.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.directshare.app.domain.model.FileTransferItem
import com.directshare.app.domain.model.TransferState
import com.directshare.app.ui.components.AnimatedGradientButton
import com.directshare.app.ui.components.FileCardItem
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.components.HeroGlassActionTile
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepBlack
import com.directshare.app.ui.theme.DeepRuby
import com.directshare.app.ui.theme.GlassBorderCrimson
import com.directshare.app.ui.theme.ObsidianSurface
import com.directshare.app.ui.theme.RoseNeon
import com.directshare.app.ui.theme.ScarletRed
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary
import com.directshare.app.utils.FormatUtils

@Composable
fun SendFilesScreen(
    selectedFiles: List<FileTransferItem>,
    transferState: TransferState,
    onFilesPicked: (List<Uri>) -> Unit,
    onRemoveFile: (String) -> Unit,
    onClearAll: () -> Unit,
    onNavigateConnect: () -> Unit,
    onConfirmSend: () -> Unit
) {
    var showConfirmRecipientDialog by remember { mutableStateOf(false) }

    val openMultipleDocumentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onFilesPicked(uris)
        }
    }

    val totalBytes = selectedFiles.sumOf { it.sizeBytes }
    val connectedPeerName = (transferState as? TransferState.PeerConnectedReady)?.peerDeviceName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Select Files to Send",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${selectedFiles.size} file(s) queued • ${FormatUtils.formatBytes(totalBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CrimsonRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (selectedFiles.isNotEmpty()) {
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear selected files",
                        tint = RoseNeon
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Equal-Height 4-Column Bento Row (Images, Videos, Docs, Music)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeroGlassActionTile(
                title = "Images",
                icon = Icons.Default.Image,
                primaryTint = CrimsonRed,
                secondaryTint = ScarletRed,
                compact = true,
                onClick = { openMultipleDocumentsLauncher.launch(arrayOf("image/*")) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            HeroGlassActionTile(
                title = "Videos",
                icon = Icons.Default.VideoLibrary,
                primaryTint = ScarletRed,
                secondaryTint = DeepRuby,
                compact = true,
                onClick = { openMultipleDocumentsLauncher.launch(arrayOf("video/*")) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            HeroGlassActionTile(
                title = "Docs",
                icon = Icons.Default.Description,
                primaryTint = RoseNeon,
                secondaryTint = CrimsonRed,
                compact = true,
                onClick = {
                    openMultipleDocumentsLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "text/*",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.*"
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            HeroGlassActionTile(
                title = "Music",
                icon = Icons.Default.AudioFile,
                primaryTint = CrimsonRed,
                secondaryTint = DeepRuby,
                compact = true,
                onClick = { openMultipleDocumentsLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pill Row for APK, ZIP, and All Files
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactGlassChip("APK Packages", Icons.Default.Android) {
                openMultipleDocumentsLauncher.launch(
                    arrayOf("application/vnd.android.package-archive", "application/octet-stream")
                )
            }
            CompactGlassChip("ZIP Archives", Icons.Default.FolderZip) {
                openMultipleDocumentsLauncher.launch(
                    arrayOf("application/zip", "application/x-zip-compressed")
                )
            }
            CompactGlassChip("Browse Storage", Icons.Default.InsertDriveFile) {
                openMultipleDocumentsLauncher.launch(arrayOf("*/*"))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedFiles.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                cornerRadius = 20.dp,
                contentPadding = 24.dp,
                onClick = { openMultipleDocumentsLauncher.launch(arrayOf("*/*")) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        CrimsonRed.copy(alpha = 0.25f),
                                        DeepRuby.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(1.2.dp, CrimsonRed.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = CrimsonRed,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Files Queued",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap any category above or tap here to choose files from your phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedFiles, key = { it.fileId }) { fileItem ->
                    FileCardItem(
                        item = fileItem,
                        onRemove = { onRemoveFile(fileItem.fileId) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedGradientButton(
                text = "Add",
                icon = Icons.Default.Add,
                secondaryStyle = true,
                onClick = { openMultipleDocumentsLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(0.36f)
            )

            if (connectedPeerName != null) {
                AnimatedGradientButton(
                    text = "Send ${selectedFiles.size} (${FormatUtils.formatBytes(totalBytes)})",
                    icon = Icons.Default.Send,
                    enabled = selectedFiles.isNotEmpty(),
                    onClick = { showConfirmRecipientDialog = true },
                    modifier = Modifier.weight(0.64f)
                )
            } else {
                AnimatedGradientButton(
                    text = if (selectedFiles.isEmpty()) "Pair Device First" else "Pair (${FormatUtils.formatBytes(totalBytes)})",
                    icon = Icons.Default.WifiTethering,
                    onClick = onNavigateConnect,
                    modifier = Modifier.weight(0.64f)
                )
            }
        }
    }

    if (showConfirmRecipientDialog && connectedPeerName != null) {
        AlertDialog(
            onDismissRequest = { showConfirmRecipientDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Confirm File Transfer", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Send ${selectedFiles.size} file(s) (${FormatUtils.formatBytes(totalBytes)}) directly to '$connectedPeerName'?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmRecipientDialog = false
                        onConfirmSend()
                    }
                ) {
                    Text("Send Now", color = CrimsonRed, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRecipientDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun CompactGlassChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DeepBlack.copy(alpha = 0.88f))
            .border(1.dp, GlassBorderCrimson.copy(alpha = 0.50f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CrimsonRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            maxLines = 1
        )
    }
}
