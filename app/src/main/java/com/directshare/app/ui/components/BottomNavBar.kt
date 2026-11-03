package com.directshare.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.directshare.app.ui.navigation.ScreenRoute
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepBlack
import com.directshare.app.ui.theme.DeepRuby
import com.directshare.app.ui.theme.ObsidianSurface
import com.directshare.app.ui.theme.ScarletRed
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem(ScreenRoute.Home.route, "Radar", Icons.Default.Dashboard),
    BottomNavItem(ScreenRoute.SendFiles.route, "Send", Icons.Default.Send),
    BottomNavItem(ScreenRoute.DiscoveryPairing.route, "Connect", Icons.Default.QrCodeScanner),
    BottomNavItem(ScreenRoute.TransferHistory.route, "History", Icons.Default.History),
    BottomNavItem(ScreenRoute.Settings.route, "Settings", Icons.Default.Settings)
)

@Composable
fun DirectShareBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val dockShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(dockShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DeepBlack.copy(alpha = 0.97f),
                            ObsidianSurface.copy(alpha = 0.97f),
                            DeepBlack.copy(alpha = 0.97f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.horizontalGradient(
                            colors = listOf(
                                CrimsonRed.copy(alpha = 0.65f),
                                ScarletRed.copy(alpha = 0.35f),
                                DeepRuby.copy(alpha = 0.65f)
                            )
                        )
                    ),
                    dockShape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                val iconTint by animateColorAsState(
                    targetValue = if (selected) CrimsonRed else TextSecondary,
                    label = "navIconTint"
                )
                val labelTint by animateColorAsState(
                    targetValue = if (selected) TextPrimary else TextSecondary,
                    label = "navLabelTint"
                )
                val pillWidth by animateDpAsState(
                    targetValue = if (selected) 22.dp else 0.dp,
                    label = "navIndicatorWidth"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) CrimsonRed.copy(alpha = 0.14f) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(item.route) }
                        .padding(horizontal = 4.dp)
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .width(pillWidth)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(listOf(CrimsonRed, ScarletRed))
                                )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    } else {
                        Spacer(modifier = Modifier.height(5.5.dp))
                    }

                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.5.sp,
                        color = labelTint,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
