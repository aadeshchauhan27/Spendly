package com.spendly.app.ui.splash

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.spendly.app.R
import com.spendly.app.core.theme.SpendlyTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit = {},
    onGetStartedClick: () -> Unit = onSplashFinished,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    // Step 1: Logo Entrance
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }

    // Step 2: Brand Entrance
    val brandAlpha = remember { Animatable(0f) }
    val brandOffsetY = remember { Animatable(16f) }

    // Step 3: Tagline Entrance
    val taglineAlpha = remember { Animatable(0f) }

    // Step 4: Central Wallet Entrance
    val walletAlpha = remember { Animatable(0f) }
    val walletScale = remember { Animatable(0.92f) }
    val walletOffsetY = remember { Animatable(16f) }

    // Step 5: Sequential Category Icons Entrance
    val iconAlphas = remember { List(6) { Animatable(0f) } }
    val iconScales = remember { List(6) { Animatable(0.7f) } }

    // Step 11: Progress Bar Fill
    val progress = remember { Animatable(0f) }

    // Launch Animations Sequence
    LaunchedEffect(Unit) {
        // Step 1: Logo
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
        }

        // Step 2: Brand ("Spendly")
        launch {
            delay(200)
            brandAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            delay(200)
            brandOffsetY.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        }

        // Step 3: Tagline
        launch {
            delay(400)
            taglineAlpha.animateTo(1f, animationSpec = tween(350, easing = FastOutSlowInEasing))
        }

        // Step 4: Wallet
        launch {
            delay(300)
            walletAlpha.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }
        launch {
            delay(300)
            walletScale.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }
        launch {
            delay(300)
            walletOffsetY.animateTo(0f, animationSpec = tween(550, easing = FastOutSlowInEasing))
        }

        // Step 5: Staggered Category Icons
        iconAlphas.forEachIndexed { index, anim ->
            launch {
                delay(280L + index * 55L)
                anim.animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
            }
        }
        iconScales.forEachIndexed { index, anim ->
            launch {
                delay(280L + index * 55L)
                anim.animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
            }
        }

        // Step 11: Progress Bar Smooth 0 -> 100% over ~2.4s
        launch {
            progress.animateTo(1f, animationSpec = tween(2400, easing = FastOutSlowInEasing))
            delay(200)
            onSplashFinished()
            onGetStartedClick()
        }
    }

    // Step 6 & 10: Continuous Floating & Orbital Breathing (Subtle 60 FPS)
    val infiniteTransition = rememberInfiniteTransition(label = "SplashFloating")

    val floatShoppingY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shoppingY"
    )

    val floatFoodX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "foodX"
    )

    val floatTransportY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "transportY"
    )

    val floatHomeY by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeY"
    )

    val floatHealthX by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "healthX"
    )

    val floatFuelX by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fuelX"
    )

    val orbitDotAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitDotAngle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFCFD))
    ) {
        // Background Subtle Gradient Blobs (predominantly white with soft blue/mint tint)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left very pale blue blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE).copy(alpha = 0.55f),
                        Color(0xFFE0F2FE).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.12f, h * 0.12f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.12f, h * 0.12f)
            )

            // Bottom-right very pale mint blob
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD1FAE5).copy(alpha = 0.55f),
                        Color(0xFFD1FAE5).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.88f),
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = Offset(w * 0.88f, h * 0.88f)
            )

            // Soft pale peach accent near center-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFEF3C7).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.05f, h * 0.62f),
                    radius = w * 0.40f
                ),
                radius = w * 0.40f,
                center = Offset(w * 0.05f, h * 0.62f)
            )
        }

        // Main Splash Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Section: Logo + Brand + Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                // Spendly App Icon (Rounded-Square, Navy, 3D Mint Wallet, Gold ₹ Coin)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(26.dp),
                            spotColor = Color(0xFF09142A).copy(alpha = 0.22f),
                            ambientColor = Color(0xFF09142A).copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.spendly_splash_logo),
                        contentDescription = "Spendly App Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Brand Name: "Spend" (Deep Navy) + "ly" (Mint/Teal Accent)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .alpha(brandAlpha.value)
                        .offset { IntOffset(0, brandOffsetY.value.roundToInt()) }
                ) {
                    Text(
                        text = "Spend",
                        style = SpendlyTheme.typography.displayLarge.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF09142A)
                    )
                    Text(
                        text = "ly",
                        style = SpendlyTheme.typography.displayLarge.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF00D09C)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tagline: Two lines, center aligned, muted blue-grey
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(taglineAlpha.value)
                ) {
                    Text(
                        text = "Smarter UPI Spending.",
                        style = SpendlyTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Better You.",
                        style = SpendlyTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Central Section: 3D Mint Wallet + 6 Orbiting Category Icons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp),
                contentAlignment = Alignment.Center
            ) {
                // Orbital Connection Rings & Dots Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(walletAlpha.value)
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w * 0.5f
                    val cy = h * 0.52f

                    // Soft Outer Orbit Ellipse
                    drawOval(
                        color = Color(0xFFBAE6FD).copy(alpha = 0.45f),
                        topLeft = Offset(cx - 146.dp.toPx(), cy - 74.dp.toPx()),
                        size = Size(292.dp.toPx(), 148.dp.toPx()),
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Soft Inner Orbit Ellipse
                    drawOval(
                        color = Color(0xFFA7F3D0).copy(alpha = 0.40f),
                        topLeft = Offset(cx - 128.dp.toPx(), cy - 62.dp.toPx()),
                        size = Size(256.dp.toPx(), 124.dp.toPx()),
                        style = Stroke(width = 1.0.dp.toPx())
                    )

                    // Small Glowing Orbiting Accent Dots
                    val rad = Math.toRadians(orbitDotAngle.toDouble())
                    val dot1X = cx + 146.dp.toPx() * cos(rad).toFloat()
                    val dot1Y = cy + 74.dp.toPx() * sin(rad).toFloat()
                    drawCircle(
                        color = Color(0xFF00D09C).copy(alpha = 0.85f),
                        radius = 2.8.dp.toPx(),
                        center = Offset(dot1X, dot1Y)
                    )

                    val rad2 = Math.toRadians((orbitDotAngle + 180.0) % 360.0)
                    val dot2X = cx + 128.dp.toPx() * cos(rad2).toFloat()
                    val dot2Y = cy + 62.dp.toPx() * sin(rad2).toFloat()
                    drawCircle(
                        color = Color(0xFF38B6FF).copy(alpha = 0.85f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(dot2X, dot2Y)
                    )

                    // Fixed subtle accent dots matching reference
                    drawCircle(
                        color = Color(0xFFFBBF24).copy(alpha = 0.75f),
                        radius = 2.4.dp.toPx(),
                        center = Offset(cx - 96.dp.toPx(), cy + 62.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF00D09C).copy(alpha = 0.75f),
                        radius = 2.2.dp.toPx(),
                        center = Offset(cx + 104.dp.toPx(), cy + 50.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF38B6FF).copy(alpha = 0.75f),
                        radius = 2.4.dp.toPx(),
                        center = Offset(cx - 20.dp.toPx(), cy - 68.dp.toPx())
                    )
                }

                // Central 3D Wallet Illustration (Mint Wallet, Gold ₹ Coin, Chart Card)
                Box(
                    modifier = Modifier
                        .size(175.dp)
                        .scale(walletScale.value)
                        .alpha(walletAlpha.value)
                        .offset { IntOffset(0, walletOffsetY.value.roundToInt()) },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.spendly_splash_wallet),
                        contentDescription = "Spendly 3D Wallet",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 6 Orbiting Category Icons (Shopping, Food, Transport, Home, Health, Fuel)

                // 1. Shopping (Top-Left)
                Box(
                    modifier = Modifier
                        .offset(x = (-96).dp, y = (-78).dp + floatShoppingY.dp)
                        .scale(iconScales[0].value)
                        .alpha(iconAlphas[0].value)
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_shopping),
                        contentDescription = "Shopping",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Food & Dining (Top-Right)
                Box(
                    modifier = Modifier
                        .offset(x = 94.dp + floatFoodX.dp, y = (-72).dp)
                        .scale(iconScales[1].value)
                        .alpha(iconAlphas[1].value)
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_food_dining),
                        contentDescription = "Food & Dining",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Transport / Car (Mid-Right)
                Box(
                    modifier = Modifier
                        .offset(x = 124.dp, y = (-6).dp + floatTransportY.dp)
                        .scale(iconScales[2].value)
                        .alpha(iconAlphas[2].value)
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_transport),
                        contentDescription = "Transport",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 4. Home (Bottom-Right)
                Box(
                    modifier = Modifier
                        .offset(x = 92.dp, y = 72.dp + floatHomeY.dp)
                        .scale(iconScales[3].value)
                        .alpha(iconAlphas[3].value)
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_home),
                        contentDescription = "Home",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 5. Healthcare / Heart (Bottom-Left)
                Box(
                    modifier = Modifier
                        .offset(x = (-88).dp + floatHealthX.dp, y = 68.dp)
                        .scale(iconScales[4].value)
                        .alpha(iconAlphas[4].value)
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_healthcare),
                        contentDescription = "Healthcare",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 6. Fuel (Mid-Left)
                Box(
                    modifier = Modifier
                        .offset(x = (-122).dp + floatFuelX.dp, y = (-4).dp)
                        .scale(iconScales[5].value)
                        .alpha(iconAlphas[5].value)
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_fuel),
                        contentDescription = "Fuel",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Lower Section: Loading Progress Bar & Status Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 36.dp)
            ) {
                // Horizontal Rounded Progress Bar (Track: pale grey, Progress: teal/mint)
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.value.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00D09C),
                                        Color(0xFF05D3A3)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Status Text
                Text(
                    text = "Reading your transactions...",
                    style = SpendlyTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    ),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
