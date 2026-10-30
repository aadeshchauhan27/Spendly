package com.spendly.app.ui.upi

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyChip
import com.spendly.app.core.components.SpendlyEmptyState
import com.spendly.app.core.components.SpendlyProgressBar
import com.spendly.app.core.components.SpendlyTransactionRow
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.theme.DarkNavySurface
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpiTransactionsScreen(
    viewModel: UpiTransactionsViewModel,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val periods = listOf("7D", "30D", "90D", "365D")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "UPI Transactions",
                    style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                    color = colors.textPrimary
                )
                Text(
                    text = "Bank & App Spending Breakdown",
                    style = SpendlyTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            Surface(
                color = ElectricLime.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
        }

        // Time Period Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            periods.forEach { period ->
                SpendlyChip(
                    text = period,
                    selected = uiState.selectedPeriod == period,
                    onClick = { viewModel.setPeriod(period) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = if (colors.isDark) ElectricLime else colors.textPrimary)
            }
        } else if (uiState.bankBreakdowns.isEmpty()) {
            SpendlyEmptyState(
                title = "No UPI Transactions",
                subtitle = "UPI spends from your SMS messages will automatically appear here."
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Total Volume Hero Card
                item {
                    SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total UPI Outflow",
                                    style = SpendlyTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.formattedTotalSpend,
                                    style = SpendlyTheme.typography.headlineMedium.copy(
                                        fontSize = if (uiState.formattedTotalSpend.length > 9) 24.sp else 28.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.textPrimary
                                )
                                if (uiState.formattedTotalSpendLakhs != null) {
                                    Text(
                                        text = "(${uiState.formattedTotalSpendLakhs})",
                                        style = SpendlyTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${uiState.totalTxCount} transactions",
                                    style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (colors.isDark) ElectricLime else colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Across ${uiState.bankBreakdowns.size} sources",
                                    style = SpendlyTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // 2. Interactive Bank Bar Graph Card
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
                                    text = "Spending by Bank / App",
                                    style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )

                                if (uiState.selectedBankName != null) {
                                    Text(
                                        text = "Reset filter",
                                        style = SpendlyTheme.typography.labelSmall,
                                        color = ElectricLime,
                                        modifier = Modifier.clickable { viewModel.selectBank(null) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Custom Bar Chart
                            val maxAmount = uiState.bankBreakdowns.maxOfOrNull { it.totalAmountPaise } ?: 1L
                            val chartScrollState = rememberScrollState()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(chartScrollState)
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                uiState.bankBreakdowns.forEach { breakdown ->
                                    val isSelected = uiState.selectedBankName == breakdown.bankName
                                    val heightFraction = (breakdown.totalAmountPaise.toFloat() / maxAmount.toFloat()).coerceIn(0.12f, 1f)
                                    val barHeight = (heightFraction * 130).dp

                                    val bankColor = try {
                                        Color(android.graphics.Color.parseColor(breakdown.colorHex))
                                    } catch (e: Exception) {
                                        ElectricLime
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(56.dp)
                                            .clickable { viewModel.selectBank(breakdown.bankName) }
                                    ) {
                                        // Amount label on top of bar
                                        val amountLabel = breakdown.formattedAdaptive

                                        Text(
                                            text = amountLabel,
                                            style = SpendlyTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = if (isSelected) ElectricLime else colors.textSecondary,
                                            textAlign = TextAlign.Center
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // The Bar
                                        Box(
                                            modifier = Modifier
                                                .width(34.dp)
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                                .background(
                                                    if (isSelected) {
                                                        Brush.verticalGradient(listOf(ElectricLime, bankColor))
                                                    } else {
                                                        Brush.verticalGradient(listOf(bankColor.copy(alpha = 0.9f), bankColor.copy(alpha = 0.6f)))
                                                    }
                                                )
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.dp,
                                                    color = if (isSelected) ElectricLime else Color.Transparent,
                                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                                )
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Bank Name Label
                                        Text(
                                            text = breakdown.shortLabel,
                                            style = SpendlyTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) ElectricLime else colors.textPrimary,
                                            maxLines = 1
                                        )

                                        // Percentage label
                                        Text(
                                            text = "${breakdown.percentage}%",
                                            style = SpendlyTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Bank Filter Pills
                item {
                    Text(
                        text = "Sources & Accounts",
                        style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            SpendlyChip(
                                text = "All (${uiState.totalTxCount})",
                                selected = uiState.selectedBankName == null,
                                onClick = { viewModel.selectBank(null) }
                            )
                        }

                        items(uiState.bankBreakdowns) { b ->
                            SpendlyChip(
                                text = "${b.bankName} (${b.count})",
                                selected = uiState.selectedBankName == b.bankName,
                                onClick = { viewModel.selectBank(b.bankName) }
                            )
                        }
                    }
                }

                // 4. Detailed Transaction List
                item {
                    val filterTitle = if (uiState.selectedBankName != null) {
                        "${uiState.selectedBankName} Transactions (${uiState.displayedTransactions.size})"
                    } else {
                        "All Transactions (${uiState.displayedTransactions.size})"
                    }

                    Text(
                        text = filterTitle,
                        style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale("en", "IN"))

                items(uiState.displayedTransactions) { tx ->
                    val (icon, defaultColor) = getCategoryIconAndColor(tx.categoryName)
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(tx.categoryColorHex))
                    } catch (e: Exception) {
                        defaultColor
                    }

                    SpendlyTransactionRow(
                        title = tx.title,
                        categoryName = tx.paymentApp ?: tx.categoryName,
                        amountText = tx.formattedRupees.replace("₹", ""),
                        timeText = timeFormat.format(Date(tx.timestampMs)),
                        isIncoming = tx.isIncoming,
                        icon = icon,
                        iconBackground = catColor,
                        onClick = { onTransactionClick(tx.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
