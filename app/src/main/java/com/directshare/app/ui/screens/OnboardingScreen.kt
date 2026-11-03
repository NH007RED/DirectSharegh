package com.directshare.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.directshare.app.ui.components.AnimatedConnectionRing
import com.directshare.app.ui.components.AnimatedGradientButton
import com.directshare.app.ui.components.GlassCard
import com.directshare.app.ui.components.GlassSegmentedSelector
import com.directshare.app.ui.components.StatusPill
import com.directshare.app.ui.theme.CrimsonRed
import com.directshare.app.ui.theme.DeepRuby
import com.directshare.app.ui.theme.NeonEmerald
import com.directshare.app.ui.theme.RoseNeon
import com.directshare.app.ui.theme.ScarletRed
import com.directshare.app.ui.theme.TextPrimary
import com.directshare.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class GuideLanguageFilter(val label: String) {
    ALL("All 3"),
    ENGLISH("English"),
    HINDI("हिंदी"),
    BANGLA("বাংলা")
}

private data class MultilingualGuideCard(
    val languageBadge: String,
    val title: String,
    val worldPointTitle: String,
    val worldPointDesc: String,
    val twoUserPointTitle: String,
    val twoUserPointDesc: String,
    val privacyPointTitle: String,
    val privacyPointDesc: String
)

private val EnglishGuide = MultilingualGuideCard(
    languageBadge = "ENGLISH",
    title = "Anonymous Worldwide File Sharing",
    worldPointTitle = "Share Anywhere in the World Anonymously",
    worldPointDesc = "Send photos, videos, documents, and large files anywhere in the world anonymously with 100% privacy and zero account sign-up.",
    twoUserPointTitle = "2 Users Must Open the App at 1 Time",
    twoUserPointDesc = "Both sender and receiver must keep the application open at the same time on their phones to connect and share files instantly.",
    privacyPointTitle = "Direct Phone-to-Phone Privacy",
    privacyPointDesc = "Connect with a 6-digit code or QR scan. Your files are never saved on any cloud server."
)

private val HindiGuide = MultilingualGuideCard(
    languageBadge = "हिंदी • HINDI",
    title = "दुनिया भर में गुमनाम फ़ाइल शेयरिंग",
    worldPointTitle = "पूरी दुनिया में कहीं पर भी गुमनाम फ़ाइल शेयर करें",
    worldPointDesc = "आप दुनिया में कहीं पर भी बिना किसी अकाउंट या पहचान के (Anonymously) पूरी गोपनीयता के साथ फ़ोटो, वीडियो और बड़ी फ़ाइलें शेयर कर सकते हैं।",
    twoUserPointTitle = "2 यूज़र्स का 1 समय पर ऐप खोलना ज़रूरी है",
    twoUserPointDesc = "फ़ाइल भेजने और पाने के लिए दोनों यूज़र्स (2 Users) का एक ही समय पर अपने फ़ोन में एप्लिकेशन खुला होना चाहिए।",
    privacyPointTitle = "100% सुरक्षित और प्राइवेट कनेक्शन",
    privacyPointDesc = "बस 6-अंकों का कोड या QR स्कैन करें। आपकी फ़ाइलें किसी भी सर्वर पर सेव नहीं होतीं।"
)

private val BanglaGuide = MultilingualGuideCard(
    languageBadge = "বাংলা • BANGLA",
    title = "সারা বিশ্বে গোপনীয়তার সাথে ফাইল শেয়ারিং",
    worldPointTitle = "পৃথিবীর যেকোনো প্রান্তে গোপনে ফাইল শেয়ার করুন",
    worldPointDesc = "কোনো অ্যাকাউন্ট বা পরিচয় ছাড়াই (Anonymously) সারা বিশ্বের যেকোনো স্থানে সম্পূর্ণ গোপনীয়তার সাথে ছবি, ভিডিও এবং বড় ফাইল শেয়ার করতে পারবেন।",
    twoUserPointTitle = "২ জন ব্যবহারকারীকে একই সময়ে অ্যাপ খোলা রাখতে হবে",
    twoUserPointDesc = "ফাইল আদান-প্রদান করার জন্য প্রেরক এবং প্রাপক উভয়কেই (2 Users) একই সময়ে নিজেদের ফোনে অ্যাপ্লিকেশনটি খোলা রাখতে হবে।",
    privacyPointTitle = "১০০% সুরক্ষিত ও প্রাইভেট ট্রান্সফার",
    privacyPointDesc = "৬-সংখ্যার কোড বা QR স্ক্যান করে মুহূর্তেই যুক্ত হন। আপনার ফাইল কখনোই কোনো সার্ভারে জমা হয় না।"
)

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onOpenPrivacyDetails: () -> Unit
) {
    var targetProgress by remember { mutableFloatStateOf(0.08f) }
    var isReady by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(GuideLanguageFilter.ALL) }

    LaunchedEffect(Unit) {
        targetProgress = 1f
        delay(950)
        isReady = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "startupLoadingProgress"
    )
    val percent = (animatedProgress * 100f).roundToInt().coerceIn(0, 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand Header + Animated Radar
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Black)) {
                    append("Direct")
                }
                withStyle(SpanStyle(color = CrimsonRed, fontWeight = FontWeight.Black)) {
                    append("Share")
                }
            },
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Anonymous Global File Transfer",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedConnectionRing(
            size = 148.dp,
            isConnected = isReady,
            centerLabel = if (isReady) "Ready" else "$percent%",
            subLabel = if (isReady) "Global Share" else "Loading..."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Crimson Loading Bar Card
        GlassCard(
            cornerRadius = 16.dp,
            contentPadding = 12.dp,
            accentBorder = true,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isReady) "System Ready • Select Language or Continue" else "Initializing Anonymous Transfer Engine...",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    text = if (isReady) "READY" else "$percent%",
                    color = if (isReady) NeonEmerald else CrimsonRed
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.05f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(CrimsonRed, ScarletRed, RoseNeon)
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3-Language Switcher Bar (All 3 | English | हिंदी | বাংলা)
        GlassSegmentedSelector(
            options = GuideLanguageFilter.entries.toList(),
            selectedOption = selectedLanguage,
            labelProvider = { it.label },
            onOptionSelected = { selectedLanguage = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        val visibleGuides = when (selectedLanguage) {
            GuideLanguageFilter.ALL -> listOf(EnglishGuide, HindiGuide, BanglaGuide)
            GuideLanguageFilter.ENGLISH -> listOf(EnglishGuide)
            GuideLanguageFilter.HINDI -> listOf(HindiGuide)
            GuideLanguageFilter.BANGLA -> listOf(BanglaGuide)
        }

        visibleGuides.forEach { guide ->
            LanguageInfoCard(guide = guide)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedGradientButton(
            text = "Next to Home",
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(onClick = onOpenPrivacyDetails) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CrimsonRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "How Anonymous File Sharing Works",
                color = CrimsonRed,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun LanguageInfoCard(guide: MultilingualGuideCard) {
    GlassCard(
        accentBorder = true,
        glowTint = CrimsonRed,
        contentPadding = 14.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = guide.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusPill(text = guide.languageBadge, color = CrimsonRed)
        }

        Spacer(modifier = Modifier.height(10.dp))

        GuidePointRow(
            icon = Icons.Default.Public,
            title = guide.worldPointTitle,
            description = guide.worldPointDesc
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuidePointRow(
            icon = Icons.Default.PhonelinkRing,
            title = guide.twoUserPointTitle,
            description = guide.twoUserPointDesc
        )

        Spacer(modifier = Modifier.height(10.dp))

        GuidePointRow(
            icon = Icons.Default.VerifiedUser,
            title = guide.privacyPointTitle,
            description = guide.privacyPointDesc
        )
    }
}

@Composable
private fun GuidePointRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(CrimsonRed.copy(alpha = 0.24f), DeepRuby.copy(alpha = 0.16f))
                    )
                )
                .border(1.dp, CrimsonRed.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CrimsonRed,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
