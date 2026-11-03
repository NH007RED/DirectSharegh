package com.directshare.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.directshare.app.domain.model.PeerDevice
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepBlack
import com.directshare.app.ui.theme.DeepRuby
import com.directshare.app.ui.theme.NeonEmerald
import com.directshare.app.ui.theme.RoseNeon
import com.directshare.app.ui.theme.ScarletRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive Crimson Radar Scanner with rotating sonar sweep beam, concentric distance rings,
 * crosshairs, and bounded orbital device badges.
 */
@Composable
fun OrbitalPeerRadar(
    peers: List<PeerDevice>,
    isConnected: Boolean,
    centerLabel: String,
    subLabel: String,
    onPeerClicked: (PeerDevice) -> Unit,
    modifier: Modifier = Modifier,
    radarSize: Dp = 200.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepRotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(radarSize + 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(radarSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension / 2f

            // Crimson outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CrimsonRed.copy(alpha = 0.18f),
                        ScarletRed.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR
                ),
                radius = maxR,
                center = center
            )

            // 4 Concentric Radar Rings
            listOf(0.28f, 0.52f, 0.76f, 0.95f).forEachIndexed { idx, factor ->
                drawCircle(
                    color = if (idx % 2 == 0) CrimsonRed.copy(alpha = 0.30f) else DeepRuby.copy(alpha = 0.26f),
                    radius = maxR * factor * if (idx == 3) pulse else 1f,
                    center = center,
                    style = Stroke(width = if (idx == 3) 1.6.dp.toPx() else 1.dp.toPx())
                )
            }

            // Crosshair axis lines
            drawLine(
                color = CrimsonRed.copy(alpha = 0.16f),
                start = Offset(center.x, center.y - maxR * 0.93f),
                end = Offset(center.x, center.y + maxR * 0.93f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = CrimsonRed.copy(alpha = 0.16f),
                start = Offset(center.x - maxR * 0.93f, center.y),
                end = Offset(center.x + maxR * 0.93f, center.y),
                strokeWidth = 1.dp.toPx()
            )

            // Rotating Crimson Radar Sweep Cone
            rotate(degrees = sweepAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.76f to Color.Transparent,
                        0.90f to DeepRuby.copy(alpha = 0.25f),
                        1.0f to CrimsonRed.copy(alpha = 0.55f),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = Offset(center.x - maxR * 0.92f, center.y - maxR * 0.92f),
                    size = Size(maxR * 1.84f, maxR * 1.84f)
                )

                // Leading laser beam line
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, CrimsonRed),
                        start = center,
                        end = Offset(center.x + maxR * 0.92f, center.y)
                    ),
                    start = center,
                    end = Offset(center.x + maxR * 0.92f, center.y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Center core badge
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isConnected) NeonEmerald.copy(alpha = 0.40f) else CrimsonRed.copy(alpha = 0.42f),
                        DeepBlack
                    ),
                    center = center,
                    radius = maxR * 0.27f
                ),
                radius = maxR * 0.27f,
                center = center
            )
            drawCircle(
                color = if (isConnected) NeonEmerald else CrimsonRed,
                radius = maxR * 0.27f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Status Label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            if (subLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnected) NeonEmerald else CrimsonRed
                )
            }
        }

        // Orbital Device Nodes around the Radar (up to 4 visible orbital badges)
        val orbitalAngles = listOf(-38.0, -142.0, 38.0, 142.0)
        peers.take(4).forEachIndexed { index, peer ->
            val angleRad = orbitalAngles[index % orbitalAngles.size] * (PI / 180.0)
            val orbitRadiusDp = radarSize.value * 0.34f
            val offsetX = (cos(angleRad) * orbitRadiusDp).dp
            val offsetY = (sin(angleRad) * orbitRadiusDp).dp
            val isHosting = peer.activeSessionId != null

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = offsetX.roundToPx(),
                            y = offsetY.roundToPx()
                        )
                    }
                    .clickable { onPeerClicked(peer) }
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isHosting) {
                                    listOf(CrimsonRed, RoseNeon)
                                } else {
                                    listOf(ScarletRed, DeepRuby)
                                }
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = peer.deviceName.take(2).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    color = DeepBlack.copy(alpha = 0.92f),
                    shape = PillShape,
                    border = BorderStroke(
                        1.dp,
                        if (isHosting) NeonEmerald.copy(alpha = 0.75f) else CrimsonRed.copy(alpha = 0.65f)
                    )
                ) {
                    Text(
                        text = peer.deviceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 96.dp)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedConnectionRing(
    size: Dp = 180.dp,
    isConnected: Boolean = false,
    centerLabel: String,
    subLabel: String = ""
) {
    OrbitalPeerRadar(
        peers = emptyList(),
        isConnected = isConnected,
        centerLabel = centerLabel,
        subLabel = subLabel,
        onPeerClicked = {},
        radarSize = size
    )
}

/**
 * Phone-to-Phone Wave Bridge header shown on the Live Transfer screen:
 * Displays Sender Phone icon <---> Animated Crimson Sine Wave & [ DIRECT LINK ] pill <---> Receiver Phone icon.
 */
@Composable
fun PeerToPeerWaveBridge(
    peerName: String,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "directWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PhoneEndpointBadge(
                label = if (isSending) "Sender" else "Receiver",
                tint = CrimsonRed
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    val w = size.width
                    val midY = size.height / 2f
                    val amp = size.height * 0.26f

                    val wavePath1 = Path()
                    val wavePath2 = Path()
                    val steps = 48
                    for (i in 0..steps) {
                        val x = (w * i) / steps
                        val y1 = midY + sin((i * 0.32f) - phase) * amp
                        val y2 = midY + cos((i * 0.32f) - phase) * (amp * 0.7f)
                        if (i == 0) {
                            wavePath1.moveTo(x, y1)
                            wavePath2.moveTo(x, y2)
                        } else {
                            wavePath1.lineTo(x, y1)
                            wavePath2.lineTo(x, y2)
                        }
                    }

                    drawPath(
                        path = wavePath2,
                        brush = Brush.horizontalGradient(
                            colors = listOf(CrimsonRed.copy(alpha = 0.38f), DeepRuby.copy(alpha = 0.38f))
                        ),
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = wavePath1,
                        brush = Brush.horizontalGradient(
                            colors = listOf(CrimsonRed, ScarletRed, DeepRuby)
                        ),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Surface(
                    color = DeepBlack.copy(alpha = 0.94f),
                    shape = PillShape,
                    border = BorderStroke(
                        1.2.dp,
                        Brush.horizontalGradient(listOf(CrimsonRed, DeepRuby))
                    )
                ) {
                    Text(
                        text = "DIRECT LINK",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CrimsonRed,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            PhoneEndpointBadge(
                label = peerName.take(12),
                tint = RoseNeon
            )
        }
    }
}

@Composable
private fun PhoneEndpointBadge(
    label: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 64.dp, max = 88.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.16f))
                .border(1.5.dp, tint.copy(alpha = 0.80f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 3D Crimson Circular Progress Gauge with outer red-ruby bloom and high-contrast tabular percentage.
 */
@Composable
fun TransferProgressGauge(
    fraction: Float,
    percentageText: String,
    speedText: String,
    size: Dp = 192.dp
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = 15.dp.toPx()
            val glowStrokePx = 26.dp.toPx()
            val inset = glowStrokePx / 2f
            val arcSize = Size(this.size.width - glowStrokePx, this.size.height - glowStrokePx)
            val topLeft = Offset(inset, inset)

            // Dark recessed carbon track
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Outer crimson bloom halo
            if (clamped > 0.01f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            CrimsonRed.copy(alpha = 0.30f),
                            ScarletRed.copy(alpha = 0.30f),
                            DeepRuby.copy(alpha = 0.34f),
                            RoseNeon.copy(alpha = 0.30f),
                            CrimsonRed.copy(alpha = 0.30f)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = glowStrokePx, cap = StrokeCap.Round)
                )
            }

            // Crisp foreground crimson-to-ruby progress ring
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        CrimsonRed,
                        ScarletRed,
                        DeepRuby,
                        RoseNeon,
                        CrimsonRed
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = percentageText,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = speedText,
                style = MaterialTheme.typography.labelLarge,
                color = CrimsonRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
