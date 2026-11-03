package com.directshare.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.directshare.app.ui.theme.CarbonElevated
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepBlack
import com.directshare.app.ui.theme.DeepRuby
import com.directshare.app.ui.theme.GlassBorderCrimson
import com.directshare.app.ui.theme.GlassBorderRuby
import com.directshare.app.ui.theme.GlassBorderSubtle
import com.directshare.app.ui.theme.GlassCardBottom
import com.directshare.app.ui.theme.GlassCardElevatedBottom
import com.directshare.app.ui.theme.GlassCardElevatedTop
import com.directshare.app.ui.theme.GlassCardTop
import com.directshare.app.ui.theme.GlassHighlightTop
import com.directshare.app.ui.theme.ObsidianSurface
import com.directshare.app.ui.theme.RoseNeon
import com.directshare.app.ui.theme.ScarletRed
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary

val LiquidGlassShape = RoundedCornerShape(18.dp)
val PillShape = RoundedCornerShape(50)

/**
 * Full-screen deep carbon black canvas with subtle crimson and ruby ambient light orbs.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambientMesh")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbDrift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepBlack, ObsidianSurface, DeepBlack)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CrimsonRed.copy(alpha = 0.13f),
                        ScarletRed.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.20f + 0.12f * drift), h * 0.11f),
                    radius = w * 0.68f
                ),
                radius = w * 0.68f,
                center = Offset(w * (0.20f + 0.12f * drift), h * 0.11f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DeepRuby.copy(alpha = 0.14f),
                        RoseNeon.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.82f - 0.12f * drift), h * 0.50f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * (0.82f - 0.12f * drift), h * 0.50f)
            )
        }
        content()
    }
}

/**
 * True Obsidian Carbon & Crimson Frosted GlassCard:
 * - Avoids Modifier.shadow() which causes Android hardware layers to render a solid grey box behind translucent fills.
 * - Uses explicit deep carbon-obsidian gradient fills with a subtle top specular rim highlight and glowing crimson border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    accentBorder: Boolean = false,
    glowTint: Color? = null,
    contentPadding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "glassCardScale"
    )

    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = when {
        glowTint != null -> Brush.linearGradient(
            colors = listOf(
                glowTint.copy(alpha = 0.80f),
                glowTint.copy(alpha = 0.30f),
                GlassBorderRuby.copy(alpha = 0.45f)
            )
        )
        accentBorder -> Brush.linearGradient(
            colors = listOf(
                CrimsonRed.copy(alpha = 0.82f),
                ScarletRed.copy(alpha = 0.50f),
                DeepRuby.copy(alpha = 0.75f)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                GlassHighlightTop,
                GlassBorderCrimson.copy(alpha = 0.28f),
                GlassBorderSubtle
            )
        )
    }

    val surfaceBrush = if (accentBorder || glowTint != null) {
        Brush.verticalGradient(
            colors = listOf(
                (glowTint ?: CrimsonRed).copy(alpha = 0.09f),
                GlassCardElevatedTop,
                GlassCardElevatedBottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(GlassCardTop, GlassCardBottom)
        )
    }

    val cardModifier = modifier
        .scale(scale)
        .clip(shape)
        .background(surfaceBrush)
        .drawWithContent {
            drawContent()
            // Subtle top specular glass reflection
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = 20.dp.toPx()
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                size = Size(size.width, 20.dp.toPx())
            )
        }
        .border(
            BorderStroke(if (accentBorder || glowTint != null) 1.25.dp else 1.dp, borderBrush),
            shape
        )
        .let { base ->
            if (onClick != null) {
                base.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else {
                base
            }
        }
        .padding(contentPadding)

    Column(
        modifier = cardModifier,
        content = content
    )
}

/**
 * Uniform-height Hero Action Tile used on Home ("Quick Send", "Receive")
 * and SendFiles 4-column Bento Grid ("Images", "Videos", "Docs", "Music").
 */
@Composable
fun HeroGlassActionTile(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    primaryTint: Color,
    secondaryTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    GlassCard(
        modifier = modifier.defaultMinSize(minHeight = if (compact) 76.dp else 68.dp),
        cornerRadius = 16.dp,
        glowTint = primaryTint,
        contentPadding = if (compact) 10.dp else 13.dp,
        onClick = onClick
    ) {
        if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    primaryTint.copy(alpha = 0.28f),
                                    secondaryTint.copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(primaryTint.copy(alpha = 0.80f), secondaryTint.copy(alpha = 0.40f))
                            ),
                            shape = RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    primaryTint.copy(alpha = 0.30f),
                                    secondaryTint.copy(alpha = 0.20f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(primaryTint.copy(alpha = 0.85f), secondaryTint.copy(alpha = 0.45f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Equal-Width Segmented Carbon-Crimson Pill Selector:
 * Ensures every option has equal weight, single-line enforcement, high-contrast white text on active crimson fill,
 * and a subtle specular rim border.
 */
@Composable
fun <T> GlassSegmentedSelector(
    options: List<T>,
    selectedOption: T,
    labelProvider: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(DeepBlack.copy(alpha = 0.85f))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(13.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val itemShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(itemShape)
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                colors = listOf(CrimsonRed, ScarletRed, DeepRuby)
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent)
                            )
                        }
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(Color.White.copy(alpha = 0.45f), RoseNeon.copy(alpha = 0.65f))
                                ),
                                itemShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onOptionSelected(option) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(option),
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else TextSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Flagship Crimson-to-Ruby Pill Button (48.dp height) with responsive horizontal padding
 * so both full-width CTAs and 2-column grid buttons render cleanly without text clipping.
 */
@Composable
fun AnimatedGradientButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondaryStyle: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradientShift")
    val shift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    val colors = if (!enabled) {
        listOf(Color(0xFF1C1619), Color(0xFF110D0F))
    } else if (secondaryStyle) {
        listOf(
            CarbonElevated,
            DeepRuby.copy(alpha = 0.55f),
            ScarletRed.copy(alpha = 0.40f)
        )
    } else {
        listOf(CrimsonRed, ScarletRed, DeepRuby)
    }

    val brush = Brush.linearGradient(
        colors = colors,
        start = Offset(shift, 0f),
        end = Offset(shift + 500f, 220f)
    )

    val borderBrush = if (!enabled) {
        Brush.linearGradient(listOf(GlassBorderSubtle, GlassBorderSubtle))
    } else if (secondaryStyle) {
        Brush.linearGradient(colors = listOf(CrimsonRed.copy(alpha = 0.65f), DeepRuby.copy(alpha = 0.70f)))
    } else {
        Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.55f), RoseNeon.copy(alpha = 0.90f)))
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .height(48.dp)
            .clip(PillShape)
            .background(brush)
            .border(1.dp, borderBrush, PillShape)
            .semantics { contentDescription = text }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) Color.White else TextSecondary,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = PillShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.60f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TransferSparklineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val points = if (dataPoints.size >= 2) dataPoints else listOf(0.24f, 0.50f, 0.36f, 0.78f, 0.60f, 0.94f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxVal = (points.maxOrNull() ?: 1f).coerceAtLeast(0.01f)
        val stepX = w / (points.size - 1).coerceAtLeast(1)

        val coords = points.mapIndexed { idx, value ->
            val normalized = (value / maxVal).coerceIn(0.14f, 0.90f)
            Offset(idx * stepX, h * (1f - normalized))
        }

        val strokePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (i in 0 until coords.size - 1) {
                val p0 = coords[i]
                val p1 = coords[i + 1]
                val controlX1 = (p0.x + p1.x) / 2f
                cubicTo(controlX1, p0.y, controlX1, p1.y, p1.x, p1.y)
            }
        }

        val fillPath = Path().apply {
            addPath(strokePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    CrimsonRed.copy(alpha = 0.36f),
                    DeepRuby.copy(alpha = 0.14f),
                    Color.Transparent
                )
            )
        )

        drawPath(
            path = strokePath,
            brush = Brush.horizontalGradient(
                colors = listOf(DeepRuby, ScarletRed, CrimsonRed)
            ),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )

        coords.lastOrNull()?.let { lastPt ->
            drawCircle(
                color = CrimsonRed,
                radius = 3.8.dp.toPx(),
                center = lastPt
            )
        }
    }
}
