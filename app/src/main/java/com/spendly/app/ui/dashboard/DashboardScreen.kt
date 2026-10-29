package com.spendly.app.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.SpendlyChip
import com.spendly.app.core.components.SpendlyEmptyState
import com.spendly.app.core.components.SpendlyHeader
import com.spendly.app.core.components.SpendlyTransactionRow
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.components.SpendlyTrendLineChart
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.StatusExpenseRed
import com.spendly.app.core.theme.StatusIncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onToggleTheme: () -> Unit,
    onViewAllCategories: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val periods = listOf("7D", "30D", "3M", "1Y")
    val context = LocalContext.current
    var visibleRecentCount by remember { mutableStateOf(5) }

    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasSmsPermission) {
        if (hasSmsPermission) {
            viewModel.syncTransactionsFromSms(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val userName = when (val state = uiState) {
            is DashboardUiState.Success -> state.userName
            else -> "Aadesh"
        }

        // App Header
        SpendlyHeader(
            userName = userName,
            greetingSubtitle = "Small steps. Big savings.",
            onToggleTheme = onToggleTheme,
            onProfileClick = onProfileClick
        )

        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (colors.isDark) ElectricLime else colors.textPrimary)
                }
            }
            is DashboardUiState.Syncing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(bottom = 8.dp),
                                color = ElectricLime,
                                strokeWidth = 3.5.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analyzing UPI Spending...",
                                style = SpendlyTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = SpendlyTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = ElectricLime.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "🔒 100% On-Device & Private",
                                    style = SpendlyTheme.typography.labelSmall,
                                    color = ElectricLime,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            is DashboardUiState.Error -> {
                SpendlyEmptyState(
                    title = "Unable to load dashboard",
                    subtitle = state.message
                )
            }
            is DashboardUiState.Success -> {
                if (state.isEmpty) {
                    // Empty state when no data exists
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SpendlyEmptyState(
                            title = "No transactions found",
                            subtitle = "Scan your UPI SMS messages to analyze your spending automatically."
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        SpendlyButton(
                            text = "Scan SMS Messages",
                            onClick = { viewModel.syncTransactionsFromSms(context, force = true) },
                            style = SpendlyButtonStyle.PRIMARY_LIME,
                            icon = {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.padding(end = 8.dp))
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Hero Total Spending Card
                        item {
                            SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Total Spending",
                                            style = SpendlyTheme.typography.bodyMedium,
                                            color = colors.textSecondary
                                        )

                                        // Trend change badge
                                        if (state.summary.spendingChangePercent != null) {
                                            val pct = state.summary.spendingChangePercent
                                            val isDecrease = pct <= 0f
                                            val badgeColor = if (isDecrease) StatusIncomeGreen else StatusExpenseRed
                                            val arrow = if (isDecrease) "↓" else "↑"
                                            val text = "$arrow ${String.format(Locale.US, "%.0f", Math.abs(pct))}% vs last month"

                                            Box(
                                                modifier = Modifier
                                                    .clip(PillShape)
                                                    .background(badgeColor.copy(alpha = 0.15f))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = text,
                                                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = badgeColor
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = state.summary.formattedNetSpending,
                                        style = SpendlyTheme.typography.displayLarge.copy(
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colors.textPrimary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Sub-metrics: Income, Received, Net Cash Flow
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Income",
                                                style = SpendlyTheme.typography.labelSmall,
                                                color = colors.textMuted
                                            )
                                            Text(
                                                text = state.summary.formattedIncome,
                                                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colors.textPrimary
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Received",
                                                style = SpendlyTheme.typography.labelSmall,
                                                color = colors.textMuted
                                            )
                                            Text(
                                                text = state.summary.formattedReceived,
                                                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colors.textPrimary
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = "Net Cash Flow",
                                                style = SpendlyTheme.typography.labelSmall,
                                                color = colors.textMuted
                                            )
                                            val isPositiveFlow = state.summary.netCashFlowPaise >= 0
                                            Text(
                                                text = state.summary.formattedCashFlow,
                                                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isPositiveFlow) StatusIncomeGreen else StatusExpenseRed
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Period Filter Chips
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                periods.forEach { period ->
                                    SpendlyChip(
                                        text = period,
                                        selected = state.selectedPeriod == period,
                                        onClick = { viewModel.setPeriod(period) }
                                    )
                                }
                            }
                        }

                        // Interactive Trend Chart
                        item {
                            SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Spending Trend",
                                            style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Spending",
                                            style = SpendlyTheme.typography.labelSmall,
                                            color = if (colors.isDark) ElectricLime else colors.textSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val trendData = state.summary.trendDataPoints.ifEmpty { listOf(0f, 0f) }
                                    SpendlyTrendLineChart(
                                        dataPoints = trendData,
                                        lineColor = if (colors.isDark) ElectricLime else Color(0xFF09142A)
                                    )
                                }
                            }
                        }

                        // Top Categories Section
                        if (state.summary.topCategories.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Top Categories",
                                            style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )

                                        Text(
                                            text = "View all",
                                            style = SpendlyTheme.typography.labelMedium,
                                            color = if (colors.isDark) ElectricLime else Color(0xFF2563EB),
                                            modifier = Modifier.clickable { onViewAllCategories() }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val topCats = state.summary.topCategories.take(4)
                                        topCats.forEach { cat ->
                                            val (icon, defaultColor) = getCategoryIconAndColor(cat.categoryName)
                                            val catColor = try {
                                                Color(android.graphics.Color.parseColor(cat.colorHex))
                                            } catch (e: Exception) {
                                                defaultColor
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                SpendlyCategoryBadge(
                                                    icon = icon,
                                                    backgroundColor = catColor,
                                                    size = 48.dp,
                                                    iconSize = 24.dp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = cat.categoryName.split(" ").first(),
                                                    style = SpendlyTheme.typography.labelMedium,
                                                    color = colors.textPrimary
                                                )
                                                Text(
                                                    text = "${String.format(Locale.US, "%.0f", cat.percentage)}%",
                                                    style = SpendlyTheme.typography.labelSmall,
                                                    color = colors.textMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Recent Transactions Section
                        if (state.summary.recentTransactions.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recent Transactions",
                                            style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Showing ${minOf(visibleRecentCount, state.summary.recentTransactions.size)} of ${state.summary.recentTransactions.size}",
                                            style = SpendlyTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }

                                    val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "IN"))

                                    state.summary.recentTransactions.take(visibleRecentCount).forEach { tx ->
                                        val (icon, defaultColor) = getCategoryIconAndColor(tx.categoryName)
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(tx.categoryColorHex))
                                        } catch (e: Exception) {
                                            defaultColor
                                        }

                                        SpendlyTransactionRow(
                                            title = tx.title,
                                            categoryName = tx.categoryName,
                                            amountText = tx.formattedRupees.replace("₹", ""),
                                            timeText = timeFormat.format(Date(tx.timestampMs)),
                                            isIncoming = tx.isIncoming,
                                            icon = icon,
                                            iconBackground = catColor,
                                            onClick = { onTransactionClick(tx.id) }
                                        )
                                    }

                                    if (state.summary.recentTransactions.size > visibleRecentCount) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        SpendlyButton(
                                            text = "Load More (${state.summary.recentTransactions.size - visibleRecentCount} remaining)",
                                            onClick = { visibleRecentCount += 5 },
                                            style = SpendlyButtonStyle.OUTLINE,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
