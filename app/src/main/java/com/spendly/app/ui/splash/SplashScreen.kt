package com.spendly.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

            // Playful Cute Wallet Illustration (Canvas drawing matching reference)
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Floating Gold ₹ Coin
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 20.dp.toPx(),
                        center = Offset(w * 0.25f, h * 0.25f)
                    )

                    // Floating Silver % Coin
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = 16.dp.toPx(),
                        center = Offset(w * 0.78f, h * 0.22f)
                    )

                    // Floating Stars / Sparks
                    drawCircle(
                        color = ElectricLime,
                        radius = 4.dp.toPx(),
                        center = Offset(w * 0.15f, h * 0.45f)
                    )
                    drawCircle(
                        color = ElectricLime,
                        radius = 3.dp.toPx(),
                        center = Offset(w * 0.85f, h * 0.42f)
                    )

                    // Back dark navy card sticking out
                    drawRoundRect(
                        color = Color(0xFF142B52),
                        topLeft = Offset(w * 0.28f, h * 0.32f),
                        size = Size(w * 0.44f, h * 0.28f),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                    )

                    // Mint/Teal Friendly Wallet
                    val walletWidth = w * 0.72f
                    val walletHeight = h * 0.48f
                    val walletTopLeft = Offset(w * 0.14f, h * 0.42f)

                    drawRoundRect(
                        color = Color(0xFF4ADE80),
                        topLeft = walletTopLeft,
                        size = Size(walletWidth, walletHeight),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )

                    // Wallet button / lock
                    drawCircle(
                        color = ElectricLime,
                        radius = 7.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.82f, walletTopLeft.y + walletHeight * 0.45f)
                    )

                    // Blushing smiling face: eyes
                    val eyeY = walletTopLeft.y + walletHeight * 0.55f
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = 3.5.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.38f, eyeY)
                    )
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = 3.5.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.62f, eyeY)
                    )

                    // Blushing cheeks
                    drawCircle(
                        color = Color(0xFFF472B6).copy(alpha = 0.5f),
                        radius = 4.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.32f, eyeY + 6.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFF472B6).copy(alpha = 0.5f),
                        radius = 4.dp.toPx(),
                        center = Offset(walletTopLeft.x + walletWidth * 0.68f, eyeY + 6.dp.toPx())
                    )

                    // Happy mouth curve
                    val mouthPath = Path().apply {
                        moveTo(walletTopLeft.x + walletWidth * 0.44f, eyeY + 4.dp.toPx())
                        cubicTo(
                            walletTopLeft.x + walletWidth * 0.47f, eyeY + 11.dp.toPx(),
                            walletTopLeft.x + walletWidth * 0.53f, eyeY + 11.dp.toPx(),
                            walletTopLeft.x + walletWidth * 0.56f, eyeY + 4.dp.toPx()
                        )
                    }
                    drawPath(
                        path = mouthPath,
                        color = Color(0xFF064E3B),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.5.dp.toPx(),
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
