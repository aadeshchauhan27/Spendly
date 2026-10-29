package com.spendly.app.ui.transactions

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyChip
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.domain.model.Category

@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (title: String, amountRupees: Double, categoryId: Long, isExpense: Boolean, notes: String?) -> Unit
) {
    val colors = SpendlyTheme.colors
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategoryId by remember {
        mutableStateOf(categories.firstOrNull { it.type == (if (isExpense) "EXPENSE" else "INCOME") }?.id ?: 1L)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Transaction",
                style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Type selector: Expense vs Income
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, PillShape)
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val types = listOf(true to "Expense", false to "Income")
                        types.forEach { (expenseOption, label) ->
                            val isSelected = isExpense == expenseOption
                            val bg = if (isSelected) {
                                if (colors.isDark) Color(0xFF09142A) else Color(0xFFE2E8F0)
                            } else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PillShape)
                                    .background(bg)
                                    .clickable {
                                        isExpense = expenseOption
                                        selectedCategoryId = categories.firstOrNull { it.type == (if (expenseOption) "EXPENSE" else "INCOME") }?.id ?: 1L
                                    }
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

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMessage = null },
                    label = { Text("Title / Merchant (e.g. Swiggy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = if (colors.isDark) ElectricLime else colors.textPrimary,
                        unfocusedBorderColor = colors.border
                    )
                )

                // Amount input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; errorMessage = null },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = if (colors.isDark) ElectricLime else colors.textPrimary,
                        unfocusedBorderColor = colors.border
                    )
                )

                // Category Selector
                Text(
                    text = "Category",
                    style = SpendlyTheme.typography.labelMedium,
                    color = colors.textSecondary
                )

                val filteredCategories = categories.filter { it.type == (if (isExpense) "EXPENSE" else "INCOME") }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredCategories) { cat ->
                        SpendlyChip(
                            text = cat.name,
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id }
                        )
                    }
                }

                // Notes input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = if (colors.isDark) ElectricLime else colors.textPrimary,
                        unfocusedBorderColor = colors.border
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = SpendlyTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }
        },
        confirmButton = {
            SpendlyButton(
                text = "Save",
                fullWidth = false,
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull()
                    if (title.isBlank()) {
                        errorMessage = "Please enter merchant or title"
                    } else if (parsedAmount == null || parsedAmount <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than ₹0"
                    } else {
                        onSave(title, parsedAmount, selectedCategoryId, isExpense, notes.ifBlank { null })
                        onDismiss()
                    }
                },
                style = SpendlyButtonStyle.PRIMARY_LIME
            )
        },
        dismissButton = {
            SpendlyButton(
                text = "Cancel",
                fullWidth = false,
                onClick = onDismiss,
                style = SpendlyButtonStyle.OUTLINE
            )
        },
        containerColor = colors.card
    )
}
