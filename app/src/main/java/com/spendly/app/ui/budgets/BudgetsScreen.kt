package com.spendly.app.ui.budgets

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.SpendlyDetailTopBar
import com.spendly.app.core.components.SpendlyProgressBar
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.domain.model.Budget

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBudgetToEdit by remember { mutableStateOf<Budget?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (onBackClick != null) 0.dp else 20.dp)
        ) {
            // Header
            if (onBackClick != null) {
                SpendlyDetailTopBar(
                    title = "Budgets",
                    onBackClick = onBackClick
                )
            } else {
                Text(
                    text = "Budgets",
                    style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (colors.isDark) ElectricLime else colors.textPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = if (onBackClick != null) 20.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Overall Monthly Budget Card
                    item {
                        val overall = uiState.overallBudget
                        SpendlyCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBudgetToEdit = overall
                                    showAddDialog = true
                                }
                        ) {
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
                                        text = "Monthly Budget",
                                        style = SpendlyTheme.typography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = if (overall != null) "Tap to Edit" else "Set Limit",
                                        style = SpendlyTheme.typography.labelSmall,
                                        color = if (colors.isDark) ElectricLime else colors.textPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val targetAmount = overall?.formattedTarget ?: "₹0"
                                val spentAmount = overall?.formattedSpent ?: "₹0"
                                val remainingAmount = overall?.formattedRemaining ?: "₹0"
                                val progress = overall?.progressFraction ?: 0f
                                val percent = overall?.usedPercent ?: 0

                                Text(
                                    text = targetAmount,
                                    style = SpendlyTheme.typography.displayLarge.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.textPrimary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val barColor = when {
                                    percent >= 100 -> Color(0xFFFF5252)
                                    percent >= 80 -> Color(0xFFFF9800)
                                    else -> if (colors.isDark) ElectricLime else Color(0xFF2E7D32)
                                }

                                SpendlyProgressBar(
                                    progress = progress,
                                    progressColor = barColor,
                                    height = 8.dp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$percent% used ($spentAmount)",
                                        style = SpendlyTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = if (overall != null && overall.remainingPaise < 0) "$remainingAmount over" else "$remainingAmount left",
                                        style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (percent >= 100) Color(0xFFFF5252) else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Section "Category Budgets"
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category Budgets",
                                style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${uiState.categoryBudgets.size} active",
                                style = SpendlyTheme.typography.labelMedium,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Category Budget Items
                    if (uiState.categoryBudgets.isEmpty()) {
                        item {
                            SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No category budgets set",
                                        style = SpendlyTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Set category limits to keep dining, shopping and travel expenses in check.",
                                        style = SpendlyTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(uiState.categoryBudgets, key = { it.id }) { budget ->
                            val catName = budget.categoryName ?: "Category"
                            val (icon, color) = getCategoryIconAndColor(catName)

                            SpendlyCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBudgetToEdit = budget
                                        showAddDialog = true
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SpendlyCategoryBadge(
                                            icon = icon,
                                            backgroundColor = color,
                                            size = 40.dp,
                                            iconSize = 20.dp
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = catName,
                                                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = "${budget.formattedSpent} / ${budget.formattedTarget}",
                                                style = SpendlyTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                        }

                                        Text(
                                            text = "${budget.usedPercent}%",
                                            style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (budget.usedPercent >= 100) Color(0xFFFF5252) else colors.textPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val itemBarColor = when {
                                        budget.usedPercent >= 100 -> Color(0xFFFF5252)
                                        budget.usedPercent >= 80 -> Color(0xFFFF9800)
                                        else -> color
                                    }

                                    SpendlyProgressBar(
                                        progress = budget.progressFraction,
                                        progressColor = itemBarColor,
                                        height = 6.dp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Bottom "+ Add Budget" Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            SpendlyButton(
                text = "+ Add Budget",
                onClick = {
                    selectedBudgetToEdit = null
                    showAddDialog = true
                },
                style = SpendlyButtonStyle.PRIMARY_LIME
            )
        }

        // Add / Edit Dialog
        if (showAddDialog) {
            AddEditBudgetDialog(
                categories = uiState.availableCategories,
                existingBudget = selectedBudgetToEdit,
                onDismiss = {
                    showAddDialog = false
                    selectedBudgetToEdit = null
                },
                onSave = { categoryId, amountRupees ->
                    viewModel.upsertBudget(
                        categoryId = categoryId,
                        amountRupees = amountRupees,
                        existingBudgetId = selectedBudgetToEdit?.id ?: 0L
                    )
                },
                onDelete = { budgetId ->
                    viewModel.deleteBudget(budgetId)
                }
            )
        }
    }
}
