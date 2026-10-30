package com.spendly.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.SpendlyDetailTopBar
import com.spendly.app.core.components.SpendlyDonutChart
import com.spendly.app.core.components.SpendlyEmptyState
import com.spendly.app.core.components.getCategoryIcon
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import java.util.Locale

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // Title or Top Bar with Add Button
        if (onBackClick != null) {
            SpendlyDetailTopBar(
                title = "Categories",
                onBackClick = onBackClick,
                actions = {
                    SpendlyButton(
                        text = "+ Add",
                        onClick = { showAddDialog = true },
                        style = SpendlyButtonStyle.PRIMARY_LIME
                    )
                }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                    color = colors.textPrimary
                )
                SpendlyButton(
                    text = "+ Add",
                    onClick = { showAddDialog = true },
                    style = SpendlyButtonStyle.PRIMARY_LIME
                )
            }
        }

        when (val state = uiState) {
            is CategoriesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary)
                }
            }
            is CategoriesUiState.Success -> {
                // Spending / Income Segmented Control
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(PillShape)
                        .background(colors.card)
                        .border(1.dp, colors.border, PillShape)
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val tabs = listOf(true to "Spending", false to "Income")
                        tabs.forEach { (isSpendingTab, label) ->
                            val isSelected = state.isSpending == isSpendingTab
                            val tabBg = if (isSelected) {
                                if (colors.isDark) Color(0xFF09142A) else Color(0xFFE2E8F0)
                            } else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PillShape)
                                    .background(tabBg)
                                    .clickable { viewModel.setTab(isSpendingTab) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = SpendlyTheme.typography.labelMedium,
                                    color = if (isSelected) (if (colors.isDark) ElectricLime else Color(0xFF09142A)) else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (state.isEmpty || state.categoriesList.isEmpty()) {
                    SpendlyEmptyState(
                        title = "No Category Data",
                        subtitle = "Recorded transactions will automatically appear categorized here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Interactive Donut Chart
                        item {
                            SpendlyDonutChart(
                                segments = state.donutSegments,
                                centerAmountText = state.totalAmountFormatted,
                                centerLabel = if (state.isSpending) "Total Spent" else "Total Received",
                                sizeDp = 210.dp,
                                strokeWidthDp = 26.dp
                            )

                            Spacer(modifier = Modifier.height(30.dp))
                        }

                        // Category Breakdown Items with unique icons
                        items(state.categoriesList) { cat ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (e: Exception) {
                                Color(0xFF546E7A)
                            }
                            val icon = getCategoryIcon(null, cat.categoryName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SpendlyCategoryBadge(
                                        icon = icon,
                                        backgroundColor = color,
                                        size = 38.dp,
                                        iconSize = 20.dp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = cat.categoryName,
                                        style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${String.format(Locale.US, "%.0f", cat.percentage)}%",
                                        style = SpendlyTheme.typography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Text(
                                        text = cat.formattedAdaptive,
                                        style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismissRequest = { showAddDialog = false },
            onConfirm = { name, type, colorHex, iconName ->
                viewModel.addCustomCategory(
                    name = name,
                    type = type,
                    colorHex = colorHex,
                    iconName = iconName
                )
                showAddDialog = false
            }
        )
    }
}
