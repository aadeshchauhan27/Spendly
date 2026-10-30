package com.spendly.app.ui.analytics

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.SpendlyChip
import com.spendly.app.core.components.SpendlyDateRangeDialog
import com.spendly.app.core.components.SpendlyProgressBar
import com.spendly.app.core.components.SpendlyTransactionRow
import com.spendly.app.core.components.SpendlyTrendLineChart
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.StatusIncomeGreen
import com.spendly.app.domain.engine.InsightType
import com.spendly.app.domain.model.TrendPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onTransactionClick: ((Long) -> Unit)? = null,
    onTrendPointClick: ((TrendPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val periods = listOf("7D", "30D", "90D", "365D")

    var selectedCategoryBreakdown by remember { mutableStateOf<CategorySpendBreakdown?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // Title
        Text(
            text = "Analytics",
            style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 26.sp),
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Time Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            periods.forEach { period ->
                SpendlyChip(
                    text = period,
                    selected = selectedPeriod == period,
                    onClick = { viewModel.setPeriod(period) }
                )
            }

            val isCustomActive = selectedPeriod == "Custom"
            val customText = if (isCustomActive && uiState.customRangeLabel != null) {
                "📅 ${uiState.customRangeLabel}"
            } else {
                "📅 Range"
            }

            SpendlyChip(
                text = customText,
                selected = isCustomActive,
                onClick = { showDateRangePicker = true }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = if (colors.isDark) ElectricLime else colors.textPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Spending Trend Hero Card
                item {
                    SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Spending Trend",
                                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = uiState.formattedTotalSpending,
                                        style = SpendlyTheme.typography.headlineLarge.copy(
                                            fontSize = if (uiState.formattedTotalSpending.length > 9) 28.sp else 32.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colors.textPrimary
                                    )
                                    if (uiState.formattedTotalSpendingLakhs != null) {
                                        Text(
                                            text = "(${uiState.formattedTotalSpendingLakhs})",
                                            style = SpendlyTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = colors.textSecondary
                                        )
                                    }
                                    Text(
                                        text = "Total Spending",
                                        style = SpendlyTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }

                                // Comparison with previous period
                                val isPositiveTrend = uiState.spendChangePercent <= 0
                                val trendColor = if (isPositiveTrend) StatusIncomeGreen else Color(0xFFFF5252)
                                val trendIcon = if (isPositiveTrend) Icons.Default.TrendingDown else Icons.Default.TrendingUp
                                val trendText = "${if (uiState.spendChangePercent > 0) "+" else ""}${uiState.spendChangePercent}% vs prev"

                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(trendColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = trendIcon,
                                            contentDescription = null,
                                            tint = trendColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = trendText,
                                            style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = trendColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.trendPoints.isNotEmpty()) {
                                SpendlyTrendLineChart(
                                    points = uiState.trendPoints,
                                    lineColor = if (colors.isDark) ElectricLime else Color(0xFF09142A),
                                    height = 150.dp,
                                    onPointClick = onTrendPointClick
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // X Axis Dates
                            if (uiState.trendAxisDates.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    uiState.trendAxisDates.forEach { date ->
                                        Text(
                                            text = date,
                                            style = SpendlyTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Breakdown Section
                if (uiState.categoryBreakdowns.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Category Breakdown",
                                    style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Tap to view txns",
                                    style = SpendlyTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    uiState.categoryBreakdowns.forEach { cat ->
                                        val (icon, defaultColor) = getCategoryIconAndColor(cat.categoryName)
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(cat.colorHex))
                                        } catch (e: Exception) {
                                            defaultColor
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { selectedCategoryBreakdown = cat }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    SpendlyCategoryBadge(
                                                        icon = icon,
                                                        backgroundColor = catColor,
                                                        size = 32.dp,
                                                        iconSize = 16.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = cat.categoryName,
                                                        style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                        color = colors.textPrimary
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${cat.formattedAdaptive} (${cat.percentage}%)",
                                                        style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = colors.textPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.ChevronRight,
                                                        contentDescription = "View transactions",
                                                        tint = colors.textSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            SpendlyProgressBar(
                                                progress = (cat.percentage / 100f).coerceIn(0f, 1f),
                                                progressColor = catColor,
                                                height = 6.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Insights Section
                if (uiState.insights.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Insights & Patterns",
                                style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.insights.forEach { insight ->
                                    val (badgeBg, badgeText) = when (insight.type) {
                                        InsightType.POSITIVE -> StatusIncomeGreen.copy(alpha = 0.15f) to StatusIncomeGreen
                                        InsightType.WARNING -> Color(0xFFFF9800).copy(alpha = 0.15f) to Color(0xFFFF9800)
                                        InsightType.TIP -> ElectricLime.copy(alpha = 0.15f) to (if (colors.isDark) ElectricLime else Color(0xFF09142A))
                                        InsightType.NEUTRAL -> (if (colors.isDark) Color(0xFF162544) else Color(0xFFE2E8F0)) to colors.textPrimary
                                    }

                                    SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(badgeBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lightbulb,
                                                    contentDescription = null,
                                                    tint = badgeText,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = insight.title,
                                                    style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = colors.textPrimary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = insight.description,
                                                    style = SpendlyTheme.typography.bodySmall,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Category Transactions Bottom Sheet
    if (selectedCategoryBreakdown != null) {
        val breakdown = selectedCategoryBreakdown!!
        val (icon, defaultColor) = getCategoryIconAndColor(breakdown.categoryName)
        val catColor = try {
            Color(android.graphics.Color.parseColor(breakdown.colorHex))
        } catch (e: Exception) {
            defaultColor
        }
        val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "IN"))
        var sortOption by remember { mutableStateOf("Date") }
        val sortedTransactions = remember(breakdown.transactions, sortOption) {
            when (sortOption) {
                "Largest" -> breakdown.transactions.sortedByDescending { it.amountPaise }
                "Smallest" -> breakdown.transactions.sortedBy { it.amountPaise }
                else -> breakdown.transactions.sortedByDescending { it.timestampMs }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedCategoryBreakdown = null },
            sheetState = sheetState,
            containerColor = colors.surface,
            contentColor = colors.textPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpendlyCategoryBadge(
                        icon = icon,
                        backgroundColor = catColor,
                        size = 46.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = breakdown.categoryName,
                            style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${breakdown.transactions.size} transactions • ${breakdown.formattedFull} (${breakdown.formattedAdaptive})",
                            style = SpendlyTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sort Options (Date, Largest, Smallest)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort:",
                        style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                    listOf("Date", "Largest", "Smallest").forEach { option ->
                        SpendlyChip(
                            text = option,
                            selected = sortOption == option,
                            onClick = { sortOption = option }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (sortedTransactions.isEmpty()) {
                    Text(
                        text = "No recorded transactions for this category in the selected period.",
                        style = SpendlyTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedTransactions) { tx ->
                            SpendlyTransactionRow(
                                title = tx.title,
                                categoryName = tx.paymentMethod ?: tx.categoryName,
                                amountText = tx.formattedRupees.replace("₹", ""),
                                timeText = timeFormat.format(Date(tx.timestampMs)),
                                isIncoming = tx.isIncoming,
                                icon = icon,
                                iconBackground = catColor,
                                onClick = {
                                    selectedCategoryBreakdown = null
                                    onTransactionClick?.invoke(tx.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        SpendlyDateRangeDialog(
            initialStartDateMs = uiState.customRangeStartMs,
            initialEndDateMs = uiState.customRangeEndMs,
            onDismissRequest = { showDateRangePicker = false },
            onDateRangeSelected = { startMs, endMs ->
                viewModel.setCustomDateRange(startMs, endMs)
            }
        )
    }
}
