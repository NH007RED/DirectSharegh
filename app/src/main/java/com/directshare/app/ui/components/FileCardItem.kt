package com.directshare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.directshare.app.domain.model.FileTransferItem
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepRuby
import com.directshare.app.ui.theme.RoseNeon
import com.directshare.app.ui.theme.ScarletRed
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary
import com.directshare.app.utils.FormatUtils

@Composable
fun FileCardItem(
    item: FileTransferItem,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (primaryTint, secondaryTint) = colorsForMimeType(item.mimeType, item.name)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 15.dp,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(listOf(CrimsonRed, ScarletRed))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primaryTint.copy(alpha = 0.26f),
                                secondaryTint.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        primaryTint.copy(alpha = 0.65f),
                        RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForMimeType(item.mimeType, item.name),
                    contentDescription = item.mimeType,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${FormatUtils.formatBytes(item.sizeBytes)} • ${item.mimeType.substringAfterLast('/').uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${item.name}",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun iconForMimeType(mimeType: String, fileName: String): ImageVector {
    val lowerMime = mimeType.lowercase()
    val lowerName = fileName.lowercase()
    return when {
        lowerMime.startsWith("image/") -> Icons.Default.Image
        lowerMime.startsWith("video/") -> Icons.Default.VideoFile
        lowerMime.startsWith("audio/") -> Icons.Default.AudioFile
        lowerName.endsWith(".apk") || lowerMime.contains("android.package-archive") -> Icons.Default.Android
        lowerName.endsWith(".zip") || lowerMime.contains("zip") || lowerMime.contains("compressed") -> Icons.Default.FolderZip
        lowerMime.contains("pdf") || lowerMime.startsWith("text/") -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }
}

fun colorsForMimeType(mimeType: String, fileName: String): Pair<Color, Color> {
    val lowerMime = mimeType.lowercase()
    val lowerName = fileName.lowercase()
    return when {
        lowerMime.startsWith("image/") -> CrimsonRed to RoseNeon
        lowerMime.startsWith("video/") -> ScarletRed to DeepRuby
        lowerMime.startsWith("audio/") -> RoseNeon to CrimsonRed
        lowerName.endsWith(".apk") -> CrimsonRed to DeepRuby
        else -> ScarletRed to CrimsonRed
    }
}
