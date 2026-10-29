package com.spendly.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme

data class OnboardingPage(
    val title: String,
    val description: String,
    val step: Int
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(1) }
    val totalPages = 4
    val colors = SpendlyTheme.colors

    val pages = listOf(
        OnboardingPage(
            title = "Track your UPI spending like never before",
            description = "Get clear insights, smart categories and better money decisions.",
            step = 1
        ),
        OnboardingPage(
            title = "Intelligent Auto-Categorization",
            description = "Swiggy, Amazon, Uber, or local chaiwala — neatly classified instantly.",
            step = 2
        ),
        OnboardingPage(
            title = "Budget with Confidence",
            description = "Set category limits and get gentle proactive alerts before overspending.",
            step = 3
        ),
        OnboardingPage(
            title = "100% Private & Local-First",
            description = "Your financial data stays securely on your phone. No logins required.",
            step = 4
        )
    )

    val currentInfo = pages[currentPage - 1]

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Step Progress Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Segmented progress bar
                Row(
                    modifier = Modifier.width(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..totalPages) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i <= currentPage) (if (colors.isDark) ElectricLime else Color(0xFF09142A))
                                    else colors.border
                                )
                        )
                    }
                }

                Text(
                    text = "$currentPage/$totalPages",
                    style = SpendlyTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            // Headline & Description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = currentInfo.title,
                    style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                    color = colors.textPrimary,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentInfo.description,
                    style = SpendlyTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    lineHeight = 22.sp
                )
            }

            // Central Phone Illustration
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Phone body outline
                    val phoneW = w * 0.58f
                    val phoneH = h * 0.88f
                    val phoneTopLeft = Offset((w - phoneW) / 2, (h - phoneH) / 2)

                    drawRoundRect(
                        color = if (colors.isDark) Color(0xFF142B52) else Color(0xFFE2E8F0),
                        topLeft = phoneTopLeft,
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    )

                    // Phone screen
                    val screenInset = 8.dp.toPx()
                    drawRoundRect(
                        color = if (colors.isDark) Color(0xFF0B1936) else Color.White,
                        topLeft = Offset(phoneTopLeft.x + screenInset, phoneTopLeft.y + screenInset),
                        size = Size(phoneW - screenInset * 2, phoneH - screenInset * 2),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )

                    // Bar Chart on phone screen
                    val chartBottom = phoneTopLeft.y + phoneH - 35.dp.toPx()
                    val barWidth = 14.dp.toPx()
                    val barSpacing = 8.dp.toPx()
                    val startX = phoneTopLeft.x + 22.dp.toPx()

                    val barHeights = listOf(25.dp.toPx(), 55.dp.toPx(), 40.dp.toPx(), 70.dp.toPx())
                    val barColors = listOf(Color(0xFF38BDF8), ElectricLime, Color(0xFF818CF8), Color(0xFF34D399))

                    barHeights.forEachIndexed { idx, barH ->
                        drawRoundRect(
                            color = barColors[idx % barColors.size],
                            topLeft = Offset(startX + idx * (barWidth + barSpacing), chartBottom - barH),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Floating Rupee bubble
                    drawCircle(
                        color = ElectricLime,
                        radius = 18.dp.toPx(),
                        center = Offset(phoneTopLeft.x + phoneW - 8.dp.toPx(), phoneTopLeft.y + 40.dp.toPx())
                    )
                }

                // Overlay Rupee symbol text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = "₹",
                        style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF09142A),
                        modifier = Modifier.padding(top = 58.dp, end = 34.dp)
                    )
                }
            }

            // Bottom Buttons: "Next →" and "Skip"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SpendlyButton(
                    text = if (currentPage < totalPages) "Next →" else "Get Started →",
                    onClick = {
                        if (currentPage < totalPages) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    },
                    style = if (colors.isDark) SpendlyButtonStyle.PRIMARY_LIME else SpendlyButtonStyle.SECONDARY_DARK
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Skip",
                    style = SpendlyTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    modifier = Modifier.clickable { onFinish() }
                )
            }
        }
    }
}
