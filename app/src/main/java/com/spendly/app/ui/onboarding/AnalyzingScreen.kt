package com.spendly.app.ui.onboarding

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.spendly.app.R
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.sms.SmsReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnalyzingScreen(
    repository: SpendlyRepository,
    onAnalysisComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    val progress = remember { Animatable(0f) }
    var statusIndex by remember { mutableIntStateOf(0) }
    val statusMessages = listOf(
        "Reading your transactions...",
        "Scanning UPI and bank messages...",
        "Classifying categories & merchants...",
        "Filtering noise, OTPs & reminders...",
        "100% Private & on-device 🔒"
    )

    // Trigger SMS ingestion & smooth progress fill
    LaunchedEffect(Unit) {
        // Run SMS parser in background
        val syncJob = launch {
            try {
                val categories = repository.getAllCategoriesFlow().first()
                val existing = repository.getAllTransactionsFlow().first()
                val deleted = repository.getAllDeletedTransactions()
                val scanResult = SmsReader.scanInbox(
                    context = context,
                    existingTransactions = existing,
                    categories = categories,
                    deletedTransactions = deleted,
                    maxDays = 365
                )
                val newTransactions = scanResult.items
                    .filter { it.isSelected && it.transaction != null }
                    .mapNotNull { it.transaction }
                if (newTransactions.isNotEmpty()) {
                    repository.batchInsertTransactions(
                        transactions = newTransactions,
                        fileName = "SMS_Inbox_Sync",
                        source = "SMS",
                        duplicateCount = scanResult.duplicateCount,
                        errorCount = 0
                    )
                }
            } catch (_: Exception) {}
        }

        // Cycle status messages
        val messageJob = launch {
            while (true) {
                delay(800)
                statusIndex = (statusIndex + 1) % statusMessages.size
            }
        }

        // Smooth progress animation 0% -> 100% over 2.8s
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = FastOutSlowInEasing)
        )

        // Ensure sync finishes before navigating
        syncJob.join()
        messageJob.cancel()
        delay(200)
        onAnalysisComplete()
    }

    // Floating Animations
    val infiniteTransition = rememberInfiniteTransition(label = "AnalyzingFloating")
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

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFCFD))
    ) {
        // Background Subtle Gradient Blobs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

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

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD1FAE5).copy(alpha = 0.45f),
                        Color(0xFFD1FAE5).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.90f, h * 0.88f),
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = Offset(w * 0.90f, h * 0.88f)
            )
        }

        // Full-Screen Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Upper Section: Logo, Brand & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spendly App Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(elevation = 14.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x3300C896))
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF0F1A30)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.spendly_splash_logo),
                        contentDescription = "Spendly Logo",
                        modifier = Modifier.size(76.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brand Name: Spend (Navy) + ly (Mint/Teal)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spend",
                        style = SpendlyTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "ly",
                        style = SpendlyTheme.typography.displayMedium.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color(0xFF00D09C)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline
                Text(
                    text = "Smarter UPI Spending.\nBetter You.",
                    style = SpendlyTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 21.sp
                    ),
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }

            // Middle Section: 3D Wallet & Orbiting Category Bubbles
            Box(
                modifier = Modifier
                    .size(310.dp),
                contentAlignment = Alignment.Center
            ) {
                // Orbit Rings & Floating Indicator Dots
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val rx = 138.dp.toPx()
                    val ry = 78.dp.toPx()

                    drawOval(
                        color = Color(0xFF00D09C).copy(alpha = 0.22f),
                        topLeft = Offset(cx - rx, cy - ry),
                        size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
                        style = Stroke(width = 1.4.dp.toPx())
                    )

                    val rad = Math.toRadians(orbitAngle.toDouble())
                    val dotX = cx + rx * cos(rad).toFloat()
                    val dotY = cy + ry * sin(rad).toFloat()
                    drawCircle(color = Color(0xFF00D09C), radius = 3.5.dp.toPx(), center = Offset(dotX, dotY))
                }

                // Central Wallet Graphic
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.spendly_splash_wallet),
                        contentDescription = "Spendly Wallet",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 1. Shopping (Top-Left)
                Box(
                    modifier = Modifier
                        .offset(x = (-82).dp, y = (-74).dp + floatShoppingY.dp)
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
                        .offset(x = 80.dp + floatFoodX.dp, y = (-72).dp)
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_food_dining),
                        contentDescription = "Food & Dining",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Transport (Mid-Right)
                Box(
                    modifier = Modifier
                        .offset(x = 124.dp, y = (-6).dp + floatTransportY.dp)
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
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_home),
                        contentDescription = "Home",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 5. Healthcare (Bottom-Left)
                Box(
                    modifier = Modifier
                        .offset(x = (-88).dp + floatHealthX.dp, y = 68.dp)
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
                        .size(46.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cat_fuel),
                        contentDescription = "Fuel",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Lower Section: Progress Bar & Animated Status Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 36.dp)
            ) {
                // Progress Bar
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

                // Smoothly animated status message
                AnimatedContent(
                    targetState = statusMessages[statusIndex],
                    transitionSpec = { fadeIn(tween(250)) with fadeOut(tween(200)) },
                    label = "StatusTextAnimation"
                ) { msg ->
                    Text(
                        text = msg,
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
}
