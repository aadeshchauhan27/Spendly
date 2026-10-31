package com.spendly.app.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.domain.model.TrendPoint
import com.spendly.app.domain.model.formatIndianNumber

@Composable
fun SpendlyTrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = ElectricLime,
    height: Dp = 140.dp,
    onPointClick: ((TrendPoint) -> Unit)? = null
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points) { mutableStateOf<Int?>(null) }
    val colors = SpendlyTheme.colors
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val rawValues = points.map { it.amountRupees }
    val minVal = rawValues.minOrNull() ?: 0f
    val maxVal = (rawValues.maxOrNull() ?: 100f).coerceAtLeast(minVal + 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val containerMaxWidth = maxWidth
        val containerMaxHeight = maxHeight
        val minStep = 44.dp
        val minTotalWidth = minStep * (points.size - 1).coerceAtLeast(1) + 32.dp
        val isScrollable = minTotalWidth > containerMaxWidth
        val chartWidth = if (isScrollable) minTotalWidth else containerMaxWidth
        val scrollState = rememberScrollState()

        LaunchedEffect(points, isScrollable) {
            if (isScrollable) {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier)
        ) {
            Box(
                modifier = Modifier
                    .width(chartWidth)
                    .height(height)
                    .pointerInput(points) {
                        detectTapGestures(
                            onTap = { offset ->
                                val padding = with(density) { 16.dp.toPx() }
                                val width = size.width.toFloat()
                                val stepX = (width - padding * 2) / (points.size - 1).coerceAtLeast(1)
                                val tappedIdx = ((offset.x - padding + stepX / 2f) / stepX)
                                    .toInt()
                                    .coerceIn(0, points.size - 1)
                                if (selectedIndex != tappedIdx) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                selectedIndex = if (selectedIndex == tappedIdx) null else tappedIdx
                            }
                        )
                    }
            ) {
                val widthPx = with(density) { chartWidth.toPx() }
                val heightPx = with(density) { containerMaxHeight.toPx() }
                val paddingPx = with(density) { 16.dp.toPx() }
                val graphHeightPx = heightPx - paddingPx * 2
                val stepXPx = (widthPx - paddingPx * 2) / (points.size - 1).coerceAtLeast(1)

        val calculatedPoints = points.mapIndexed { index, pt ->
            val normY = (pt.amountRupees - minVal) / (maxVal - minVal)
            val x = paddingPx + index * stepXPx
            val y = heightPx - paddingPx - (normY * graphHeightPx)
            Offset(x, y)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasHeight = size.height

            // Create smooth cubic bezier path
            val strokePath = Path().apply {
                moveTo(calculatedPoints.first().x, calculatedPoints.first().y)
                for (i in 0 until calculatedPoints.size - 1) {
                    val p0 = calculatedPoints[i]
                    val p1 = calculatedPoints[i + 1]
                    val cx1 = (p0.x + p1.x) / 2
                    val cy1 = p0.y
                    val cx2 = (p0.x + p1.x) / 2
                    val cy2 = p1.y
                    cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                }
            }

            // Fill path with gradient
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(calculatedPoints.last().x, canvasHeight)
                lineTo(calculatedPoints.first().x, canvasHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = canvasHeight
                )
            )

            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // If a point is selected, draw guide line and active pulse ring
            if (selectedIndex != null && selectedIndex!! in calculatedPoints.indices) {
                val pt = calculatedPoints[selectedIndex!!]
                // Vertical dashed guide line
                drawLine(
                    color = lineColor.copy(alpha = 0.5f),
                    start = Offset(pt.x, paddingPx / 2),
                    end = Offset(pt.x, canvasHeight - paddingPx / 2),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                // Outer glow
                drawCircle(
                    color = lineColor.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = pt
                )
                // Middle ring
                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = pt
                )
                // White inner dot
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = pt
                )
            } else {
                // Draw default last point indicator
                val lastPoint = calculatedPoints.last()
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = lastPoint
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = lastPoint
                )
            }
        }

        // Floating interactive tooltip pill above or below point
        if (selectedIndex != null && selectedIndex!! in points.indices && selectedIndex!! in calculatedPoints.indices) {
            val selectedPoint = points[selectedIndex!!]
            val pt = calculatedPoints[selectedIndex!!]
            val xDp = with(density) { pt.x.toDp() }
            val yDp = with(density) { pt.y.toDp() }

            val isNearTop = yDp < 46.dp
            val tooltipY = if (isNearTop) yDp + 14.dp else (yDp - 46.dp).coerceAtLeast(2.dp)
            val tooltipX = (xDp - 50.dp).coerceIn(8.dp, (chartWidth - 108.dp).coerceAtLeast(8.dp))

            Box(
                modifier = Modifier
                    .offset(x = tooltipX, y = tooltipY)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (colors.isDark) Color(0xFF09142A) else Color(0xFF1E293B))
                    .border(1.dp, lineColor.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                    .clickable(enabled = onPointClick != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPointClick?.invoke(selectedPoint)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedPoint.dateLabel,
                        style = SpendlyTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "₹${formatIndianNumber(selectedPoint.amountRupees.toLong())}",
                        style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricLime
                    )
                    if (onPointClick != null) {
                        Text(
                            text = "Tap to view txns →",
                            style = SpendlyTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                            color = lineColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
            }
        }
    }
}

@JvmName("SpendlyTrendLineChartFloats")
@Composable
fun SpendlyTrendLineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = ElectricLime,
    height: Dp = 140.dp
) {
    val points = dataPoints.mapIndexed { idx, v -> TrendPoint(v, "Point ${idx + 1}") }
    SpendlyTrendLineChart(points = points, modifier = modifier, lineColor = lineColor, height = height, onPointClick = null)
}

data class DonutSegment(
    val name: String,
    val value: Float,
    val color: Color
)

@Composable
fun SpendlyDonutChart(
    segments: List<DonutSegment>,
    centerAmountText: String,
    centerLabel: String = "Total Spent",
    modifier: Modifier = Modifier,
    sizeDp: Dp = 200.dp,
    strokeWidthDp: Dp = 28.dp
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    val colors = SpendlyTheme.colors

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = strokeWidthDp.toPx()
            val diameter = size.minDimension - strokeWidth
            val arcSize = Size(diameter, diameter)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            var startAngle = -90f
            segments.forEach { segment ->
                val sweepAngle = (segment.value / total) * 360f
                if (sweepAngle > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2f, // subtle gap
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        // Center Text
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = centerAmountText,
                style = SpendlyTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = centerLabel,
                style = SpendlyTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
