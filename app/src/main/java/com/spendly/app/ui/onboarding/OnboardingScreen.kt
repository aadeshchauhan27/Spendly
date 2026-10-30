package com.spendly.app.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val badge: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val featurePoints: List<String>
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val slides = listOf(
        OnboardingSlide(
            badge = "100% PRIVATE & LOCAL",
            title = "All Your Data Stays On Your Phone",
            description = "Spendly has zero cloud servers, zero telemetry, and never uploads your personal SMS. Everything is processed and stored strictly on-device.",
            icon = Icons.Default.Lock,
            iconColor = ElectricLime,
            featurePoints = listOf(
                "Local-only encrypted Room database",
                "Zero cloud sync & no user logins required",
                "Completely private financial tracking"
            )
        ),
        OnboardingSlide(
            badge = "INTELLIGENT NOISE FILTER",
            title = "No OTPs, No Loan Offers, No Spam",
            description = "Spendly's financial engine strictly filters out OTP passwords, loan advertisements, credit limit offers, and order notifications. Only real money spent is counted.",
            icon = Icons.Default.Shield,
            iconColor = Color(0xFF38BDF8),
            featurePoints = listOf(
                "Strict OTP & 2FA authentication exclusion",
                "Aggressive rejection of pre-approved loan spam",
                "Only real debits & credits recorded"
            )
        ),
        OnboardingSlide(
            badge = "MULTI-BANK & UPI TRACKER",
            title = "Instant UPI & Bank Spend Tracking",
            description = "Auto-detects spends across HDFC, SBI, Axis, ICICI, GPay, PhonePe, Paytm, CRED, and Kiwi. Merchants like Swiggy, Amazon, and Uber are categorized in real-time.",
            icon = Icons.Default.AccountBalance,
            iconColor = Color(0xFFA78BFA),
            featurePoints = listOf(
                "Automated merchant name normalization",
                "Smart category badging with custom icons",
                "Comprehensive UPI, Card & NetBanking support"
            )
        ),
        OnboardingSlide(
            badge = "FINANCIAL INTELLIGENCE",
            title = "Deep Spending Trends & Category Drills",
            description = "Explore interactive daily trend charts, 7D/30D/90D/365D views, clickable day filters, and bank-wise UPI distribution with zero manual effort.",
            icon = Icons.Default.TrendingUp,
            iconColor = Color(0xFF34D399),
            featurePoints = listOf(
                "Interactive clickable daily trend graph",
                "Drill into category spends with instant sorting",
                "Bank-wise UPI breakdown and insights"
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Step Counter and Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Segmented Progress Indicator
                Row(
                    modifier = Modifier.width(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in slides.indices) {
                        val isActive = i <= pagerState.currentPage
                        val widthAnim by animateDpAsState(
                            targetValue = if (i == pagerState.currentPage) 32.dp else 16.dp,
                            label = "dotWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(widthAnim)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isActive) (if (colors.isDark) ElectricLime else Color(0xFF09142A))
                                    else colors.border.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Text(
                    text = "Skip",
                    style = SpendlyTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onFinish()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Horizontal Pager Carousel
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIdx ->
                val slide = slides[pageIdx]
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Badge Chip
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(slide.iconColor.copy(alpha = 0.15f))
                            .border(1.dp, slide.iconColor.copy(alpha = 0.4f), PillShape)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                tint = slide.iconColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = slide.badge,
                                style = SpendlyTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = slide.iconColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = slide.title,
                        style = SpendlyTheme.typography.headlineLarge.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textPrimary,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description
                    Text(
                        text = slide.description,
                        style = SpendlyTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Feature Checklist Cards
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.card)
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        slide.featurePoints.forEach { point ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(slide.iconColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = slide.iconColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = point,
                                    style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions: Next / Get Started and Page Dots
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    for (i in slides.indices) {
                        val isSelected = i == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) (if (colors.isDark) ElectricLime else Color(0xFF09142A))
                                    else colors.textMuted.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                // CTA Button
                val isLastPage = pagerState.currentPage == slides.size - 1
                SpendlyButton(
                    text = if (isLastPage) "Get Started →" else "Next →",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isLastPage) {
                            onFinish()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    style = if (colors.isDark) SpendlyButtonStyle.PRIMARY_LIME else SpendlyButtonStyle.SECONDARY_DARK
                )
            }
        }
    }
}
