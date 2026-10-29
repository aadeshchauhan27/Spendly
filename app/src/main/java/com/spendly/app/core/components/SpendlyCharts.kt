package com.spendly.app.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme

@Composable
fun SpendlyTrendLineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = ElectricLime,
    height: Dp = 140.dp
) {
    if (dataPoints.isEmpty()) return

    val minVal = dataPoints.minOrNull() ?: 0f
    val maxVal = (dataPoints.maxOrNull() ?: 100f).coerceAtLeast(minVal + 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val padding = 16.dp.toPx()
        val graphHeight = canvasHeight - padding * 2
        val stepX = (width - padding * 2) / (dataPoints.size - 1).coerceAtLeast(1)

        val points = dataPoints.mapIndexed { index, value ->
            val normY = (value - minVal) / (maxVal - minVal)
            val x = padding + index * stepX
            val y = canvasHeight - padding - (normY * graphHeight)
            Offset(x, y)
        }

        // Create smooth cubic bezier path
        val strokePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
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
            lineTo(points.last().x, canvasHeight)
            lineTo(points.first().x, canvasHeight)
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

        // Draw last point indicator
        val lastPoint = points.last()
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
