package com.spendly.app.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.theme.DarkNavyBackground
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme

@Composable
fun SplashScreen(
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth infinite floating and breathing animations
    val infiniteTransition = rememberInfiniteTransition(label = "SplashTransitions")

    val walletOffsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "walletOffsetY"
    )

    val coin1OffsetY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coin1OffsetY"
    )

    val coin2OffsetY by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coin2OffsetY"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleScale"
    )

    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkNavyBackground)
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
            // Top Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                // Spendly Ribbon Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(36.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.7f, size.height * 0.15f)
                            cubicTo(
                                size.width * 0.3f, size.height * 0.15f,
                                size.width * 0.2f, size.height * 0.45f,
                                size.width * 0.5f, size.height * 0.55f
                            )
                            cubicTo(
                                size.width * 0.8f, size.height * 0.65f,
                                size.width * 0.7f, size.height * 0.9f,
                                size.width * 0.3f, size.height * 0.85f
                            )
                        }
                        drawPath(
                            path = path,
                            color = ElectricLime,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 7.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Spendly",
                        style = SpendlyTheme.typography.displayLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Smarter UPI Spending. Better You.",
                    style = SpendlyTheme.typography.bodyLarge,
                    color = Color(0xFF8E9EB5)
                )
            }

            // Playful Animated Cute Wallet Illustration
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Pulsating Radial Aura / Halo behind the wallet
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ElectricLime.copy(alpha = glowAlpha),
                                Color(0xFF4ADE80).copy(alpha = glowAlpha * 0.4f),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f, h * 0.56f),
                            radius = w * 0.45f
                        ),
                        radius = w * 0.45f,
                        center = Offset(w * 0.5f, h * 0.56f)
                    )

                    // Floating Gold ₹ Coin
                    val coin1Center = Offset(w * 0.24f, h * 0.24f + coin1OffsetY.dp.toPx())
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFEE58), Color(0xFFFFA000)),
                            start = Offset(coin1Center.x - 20.dp.toPx(), coin1Center.y - 20.dp.toPx()),
                            end = Offset(coin1Center.x + 20.dp.toPx(), coin1Center.y + 20.dp.toPx())
                        ),
                        radius = 21.dp.toPx(),
                        center = coin1Center
                    )
                    // Inner ring on Gold Coin
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 16.dp.toPx(),
                        center = coin1Center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Simple ₹ symbol lines
                    val rLineY = coin1Center.y
                    drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(coin1Center.x - 7.dp.toPx(), rLineY - 6.dp.toPx()),
                        end = Offset(coin1Center.x + 7.dp.toPx(), rLineY - 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(coin1Center.x - 6.dp.toPx(), rLineY - 2.dp.toPx()),
                        end = Offset(coin1Center.x + 5.dp.toPx(), rLineY - 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(coin1Center.x - 2.dp.toPx(), rLineY - 6.dp.toPx()),
                        end = Offset(coin1Center.x - 2.dp.toPx(), rLineY + 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF5D4037),
                        start = Offset(coin1Center.x - 2.dp.toPx(), rLineY + 1.dp.toPx()),
                        end = Offset(coin1Center.x + 5.dp.toPx(), rLineY + 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Floating Silver % Coin
                    val coin2Center = Offset(w * 0.78f, h * 0.22f + coin2OffsetY.dp.toPx())
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFF94A3B8)),
                            start = Offset(coin2Center.x - 17.dp.toPx(), coin2Center.y - 17.dp.toPx()),
                            end = Offset(coin2Center.x + 17.dp.toPx(), coin2Center.y + 17.dp.toPx())
                        ),
                        radius = 17.dp.toPx(),
                        center = coin2Center
                    )
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = 13.dp.toPx(),
                        center = coin2Center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    // Slash for %
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(coin2Center.x + 4.dp.toPx(), coin2Center.y - 5.dp.toPx()),
                        end = Offset(coin2Center.x - 4.dp.toPx(), coin2Center.y + 5.dp.toPx()),
                        strokeWidth = 1.8.dp.toPx()
                    )
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = 1.8.dp.toPx(),
                        center = Offset(coin2Center.x - 3.5.dp.toPx(), coin2Center.y - 3.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = 1.8.dp.toPx(),
                        center = Offset(coin2Center.x + 3.5.dp.toPx(), coin2Center.y + 3.5.dp.toPx())
                    )

                    // Twinkling Animated Stars / Sparks
                    val spark1 = Offset(w * 0.14f, h * 0.44f)
                    val r1 = 4.5.dp.toPx() * sparkleScale
                    drawCircle(
                        color = ElectricLime.copy(alpha = sparkleAlpha),
                        radius = r1,
                        center = spark1
                    )
                    drawLine(
                        color = ElectricLime.copy(alpha = sparkleAlpha),
                        start = Offset(spark1.x - r1 * 1.8f, spark1.y),
                        end = Offset(spark1.x + r1 * 1.8f, spark1.y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = ElectricLime.copy(alpha = sparkleAlpha),
                        start = Offset(spark1.x, spark1.y - r1 * 1.8f),
                        end = Offset(spark1.x, spark1.y + r1 * 1.8f),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    val spark2 = Offset(w * 0.86f, h * 0.40f)
                    val r2 = 3.8.dp.toPx() * (2f - sparkleScale)
                    drawCircle(
                        color = ElectricLime.copy(alpha = 1.35f - sparkleAlpha),
                        radius = r2,
                        center = spark2
                    )

                    // Back dark navy card sticking out (gently floating with wallet)
                    val cardTopLeft = Offset(w * 0.28f, h * 0.32f + walletOffsetY.dp.toPx())
                    drawRoundRect(
                        color = Color(0xFF142B52),
                        topLeft = cardTopLeft,
                        size = Size(w * 0.44f, h * 0.28f),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )
                    // Card chip detail
                    drawRoundRect(
                        color = Color(0xFFFACC15).copy(alpha = 0.8f),
                        topLeft = Offset(cardTopLeft.x + 8.dp.toPx(), cardTopLeft.y + 12.dp.toPx()),
                        size = Size(18.dp.toPx(), 14.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Mint/Teal Friendly Wallet Body
                    val walletWidth = w * 0.72f
                    val walletHeight = h * 0.48f
                    val walletTopLeft = Offset(w * 0.14f, h * 0.42f + walletOffsetY.dp.toPx())

                    // Wallet gradient
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4ADE80), Color(0xFF22C55E)),
                            startY = walletTopLeft.y,
                            endY = walletTopLeft.y + walletHeight
                        ),
                        topLeft = walletTopLeft,
                        size = Size(walletWidth, walletHeight),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )

                    // Wallet Flap / clasp
                    drawRoundRect(
                        color = Color(0xFF16A34A),
                        topLeft = Offset(walletTopLeft.x + walletWidth * 0.74f, walletTopLeft.y + walletHeight * 0.34f),
                        size = Size(walletWidth * 0.26f, walletHeight * 0.32f),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )

                    // Wallet button / lock
                    drawCircle(
                        color = ElectricLime,
                        radius = 7.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.84f, walletTopLeft.y + walletHeight * 0.50f)
                    )

                    // Blushing smiling face: eyes
                    val eyeY = walletTopLeft.y + walletHeight * 0.53f
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = 4.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.37f, eyeY)
                    )
                    // Eye shine
                    drawCircle(
                        color = Color.White,
                        radius = 1.3.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.37f - 1.dp.toPx(), eyeY - 1.dp.toPx())
                    )

                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = 4.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.61f, eyeY)
                    )
                    // Eye shine
                    drawCircle(
                        color = Color.White,
                        radius = 1.3.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.61f - 1.dp.toPx(), eyeY - 1.dp.toPx())
                    )

                    // Blushing cheeks
                    drawCircle(
                        color = Color(0xFFF472B6).copy(alpha = 0.55f),
                        radius = 4.5.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.31f, eyeY + 6.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFF472B6).copy(alpha = 0.55f),
                        radius = 4.5.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.67f, eyeY + 6.dp.toPx())
                    )

                    // Happy mouth curve
                    val mouthPath = Path().apply {
                        moveTo(walletTopLeft.x + walletWidth * 0.43f, eyeY + 4.dp.toPx())
                        cubicTo(
                            walletTopLeft.x + walletWidth * 0.46f, eyeY + 12.dp.toPx(),
                            walletTopLeft.x + walletWidth * 0.52f, eyeY + 12.dp.toPx(),
                            walletTopLeft.x + walletWidth * 0.55f, eyeY + 4.dp.toPx()
                        )
                    }
                    drawPath(
                        path = mouthPath,
                        color = Color(0xFF064E3B),
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }

            // Bottom CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SpendlyButton(
                    text = "Get Started →",
                    onClick = onGetStartedClick,
                    style = SpendlyButtonStyle.PRIMARY_LIME
                )
            }
        }
    }
}
