package com.spendly.app.ui.goals

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyProgressBar
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.domain.model.SavingsGoal

@Composable
fun SavingsGoalsScreen(
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoalForContribution by remember { mutableStateOf<SavingsGoal?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Text(
                text = "Savings Goals",
                style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                color = colors.textPrimary,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = if (colors.isDark) ElectricLime else colors.textPrimary)
                }
            } else if (uiState.goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No savings goals yet",
                            style = SpendlyTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set targets for emergency fund, vacation, or big purchases.",
                            style = SpendlyTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Spotlight Goal Card (First goal)
                    val spotlightGoal = uiState.goals.first()
                    item {
                        SpendlyCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedGoalForContribution = spotlightGoal }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spotlightGoal.name,
                                        style = SpendlyTheme.typography.titleMedium,
                                        color = colors.textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${spotlightGoal.formattedSaved} / ${spotlightGoal.formattedTarget}",
                                        style = SpendlyTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    SpendlyProgressBar(
                                        progress = spotlightGoal.progressFraction,
                                        progressColor = ElectricLime,
                                        height = 8.dp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "${spotlightGoal.progressPercent}%",
                                        style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Potted plant vector illustration
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(40.dp)) {
                                        val w = size.width
                                        val h = size.height

                                        drawRoundRect(
                                            color = Color(0xFFD97706),
                                            topLeft = Offset(w * 0.25f, h * 0.55f),
                                            size = Size(w * 0.5f, h * 0.35f),
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )

                                        drawLine(
                                            color = Color(0xFF16A34A),
                                            start = Offset(w * 0.5f, h * 0.55f),
                                            end = Offset(w * 0.5f, h * 0.25f),
                                            strokeWidth = 3.dp.toPx()
                                        )

                                        drawCircle(
                                            color = ElectricLime,
                                            radius = 6.dp.toPx(),
                                            center = Offset(w * 0.38f, h * 0.32f)
                                        )
                                        drawCircle(
                                            color = ElectricLime,
                                            radius = 6.dp.toPx(),
                                            center = Offset(w * 0.62f, h * 0.25f)
                                        )
                                        drawCircle(
                                            color = Color(0xFF22C55E),
                                            radius = 7.dp.toPx(),
                                            center = Offset(w * 0.5f, h * 0.18f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Remaining Goals
                    if (uiState.goals.size > 1) {
                        item {
                            Text(
                                text = "All Goals",
                                style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(uiState.goals.drop(1), key = { it.id }) { goal ->
                            val icon = getGoalIcon(goal.iconName)
                            val accentColor = try {
                                Color(android.graphics.Color.parseColor(goal.colorHex))
                            } catch (e: Exception) {
                                Color(0xFF38BDF8)
                            }

                            SpendlyCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGoalForContribution = goal }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = goal.name,
                                                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = colors.textPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = colors.textMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "${goal.formattedSaved} / ${goal.formattedTarget}",
                                            style = SpendlyTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        SpendlyProgressBar(
                                            progress = goal.progressFraction,
                                            progressColor = accentColor,
                                            height = 5.dp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = "${goal.progressPercent}%",
                                        style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textSecondary
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

        // Bottom CTA: "+ Add Goal"
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            SpendlyButton(
                text = "+ Add Goal",
                onClick = { showAddDialog = true },
                style = SpendlyButtonStyle.PRIMARY_LIME
            )
        }

        // Add Goal Dialog
        if (showAddDialog) {
            AddGoalDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, targetRupees, iconName, colorHex ->
                    viewModel.createGoal(name, targetRupees, iconName, colorHex)
                }
            )
        }

        // Record Contribution Dialog
        selectedGoalForContribution?.let { goal ->
            RecordContributionDialog(
                goal = goal,
                onDismiss = { selectedGoalForContribution = null },
                onSaveContribution = { amountRupees, isDeposit, note ->
                    viewModel.addContribution(goal.id, amountRupees, isDeposit, note)
                },
                onDeleteGoal = {
                    viewModel.deleteGoal(goal.id)
                }
            )
        }
    }
}

private fun getGoalIcon(iconName: String): ImageVector {
    return when (iconName) {
        "flight" -> Icons.Default.Flight
        "laptop" -> Icons.Default.Laptop
        "car" -> Icons.Default.DirectionsCar
        "home" -> Icons.Default.Home
        "savings" -> Icons.Default.Savings
        else -> Icons.Default.LocalFlorist
    }
}
