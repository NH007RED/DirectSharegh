package com.directshare.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepBlack
import com.directshare.app.ui.theme.ScarletRed
import com.directshare.app.utils.QrCodeHelper

/**
 * Glowing Crimson Corner-Bracket QR Code Card with animated red laser scan line.
 */
@Composable
fun GlowingQrCodeBox(
    sessionId: String,
    pairingCode: String,
    boxSize: Dp = 210.dp
) {
    val qrPayload = remember(sessionId, pairingCode) {
        QrCodeHelper.encodePairingUri(sessionId, pairingCode)
    }
    val qrBitmap = remember(qrPayload) {
        QrCodeHelper.generateQrBitmap(qrPayload, 540)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "qrScanLine")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanY"
    )

    Box(
        modifier = Modifier
            .size(boxSize)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "Pairing QR Code $pairingCode",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DeepBlack)
                    .padding(10.dp)
            )
        }

        // Crimson Corner Brackets + Animated Horizontal Red Laser Scan Line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bracketLen = 32.dp.toPx()
            val stroke = 4.dp.toPx()

            // Top-Left corner bracket
            drawLine(CrimsonRed, Offset(0f, 0f), Offset(bracketLen, 0f), stroke, StrokeCap.Round)
            drawLine(CrimsonRed, Offset(0f, 0f), Offset(0f, bracketLen), stroke, StrokeCap.Round)

            // Top-Right corner bracket
            drawLine(CrimsonRed, Offset(w, 0f), Offset(w - bracketLen, 0f), stroke, StrokeCap.Round)
            drawLine(CrimsonRed, Offset(w, 0f), Offset(w, bracketLen), stroke, StrokeCap.Round)

            // Bottom-Left corner bracket
            drawLine(CrimsonRed, Offset(0f, h), Offset(bracketLen, h), stroke, StrokeCap.Round)
            drawLine(CrimsonRed, Offset(0f, h), Offset(0f, h - bracketLen), stroke, StrokeCap.Round)

            // Bottom-Right corner bracket
            drawLine(CrimsonRed, Offset(w, h), Offset(w - bracketLen, h), stroke, StrokeCap.Round)
            drawLine(CrimsonRed, Offset(w, h), Offset(w, h - bracketLen), stroke, StrokeCap.Round)

            // Glowing crimson laser scan line
            val scanY = h * scanProgress
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, CrimsonRed, ScarletRed, CrimsonRed, Color.Transparent)
                ),
                start = Offset(16.dp.toPx(), scanY),
                end = Offset(w - 16.dp.toPx(), scanY),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun QrCodeDialog(
    sessionId: String,
    pairingCode: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            accentBorder = true,
            glowTint = CrimsonRed,
            contentPadding = 18.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Scan to Connect",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scan this QR code or enter the 6-digit pairing key on the second Android phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                GlowingQrCodeBox(
                    sessionId = sessionId,
                    pairingCode = pairingCode,
                    boxSize = 216.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Responsive 6-Character Monospace Key Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pairingCode.forEach { ch ->
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .size(width = 38.dp, height = 46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CrimsonRed.copy(alpha = 0.15f))
                                .border(1.dp, CrimsonRed.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ch.toString(),
                                fontSize = 20.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                TextButton(onClick = onDismiss) {
                    Text("Done", color = CrimsonRed, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
