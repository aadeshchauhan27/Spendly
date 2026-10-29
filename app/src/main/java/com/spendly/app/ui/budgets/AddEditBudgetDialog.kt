package com.spendly.app.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.Category

@Composable
fun AddEditBudgetDialog(
    categories: List<Category>,
    existingBudget: Budget? = null,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long?, amountRupees: Double) -> Unit,
    onDelete: ((budgetId: Long) -> Unit)? = null
) {
    val colors = SpendlyTheme.colors
    var selectedCategoryId by remember { mutableStateOf<Long?>(existingBudget?.categoryId) }
    var isOverall by remember { mutableStateOf(existingBudget?.categoryId == null) }
    var amountText by remember {
        mutableStateOf(
            existingBudget?.let { (it.amountPaise / 100.0).toString() } ?: ""
        )
    }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingBudget != null) "Edit Budget" else "Set Budget",
                    style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                if (existingBudget != null && onDelete != null) {
                    IconButton(onClick = {
                        onDelete(existingBudget.id)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Budget",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Scope selector: Overall vs Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .background(if (isOverall) (if (colors.isDark) ElectricLime else Color(0xFF09142A)) else colors.surface)
                            .clickable {
                                isOverall = true
                                selectedCategoryId = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Overall Monthly",
                            style = SpendlyTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isOverall) (if (colors.isDark) Color(0xFF09142A) else Color.White) else colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .background(if (!isOverall) (if (colors.isDark) ElectricLime else Color(0xFF09142A)) else colors.surface)
                            .clickable {
                                isOverall = false
                                if (selectedCategoryId == null && categories.isNotEmpty()) {
                                    selectedCategoryId = categories.first().id
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Category Budget",
                            style = SpendlyTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (!isOverall) (if (colors.isDark) Color(0xFF09142A) else Color.White) else colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // If Category Budget, show Category Picker
                if (!isOverall) {
                    Text(
                        text = "Select Category",
                        style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            val (icon, color) = getCategoryIconAndColor(cat.name)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) color.copy(alpha = 0.25f) else colors.surface)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) color else colors.border,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCategoryId = cat.id }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SpendlyCategoryBadge(
                                        icon = icon,
                                        backgroundColor = color,
                                        size = 24.dp,
                                        iconSize = 14.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Amount Input
                Text(
                    text = "Budget Limit (₹)",
                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 15000", color = colors.textMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Enter a valid amount greater than ₹0", color = Color(0xFFFF5252)) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (colors.isDark) ElectricLime else colors.textPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = if (colors.isDark) ElectricLime else colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            SpendlyButton(
                text = "Save Budget",
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        amountError = true
                    } else {
                        onSave(if (isOverall) null else selectedCategoryId, amount)
                        onDismiss()
                    }
                },
                style = SpendlyButtonStyle.PRIMARY_LIME,
                fullWidth = false
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        containerColor = colors.card
    )
}
