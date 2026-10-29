package com.spendly.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyProgressBar
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.domain.model.SavingsGoal

@Composable
fun RecordContributionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onSaveContribution: (amountRupees: Double, isDeposit: Boolean, note: String?) -> Unit,
    onDeleteGoal: () -> Unit
) {
    val colors = SpendlyTheme.colors
    var isDeposit by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
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
                    text = goal.name,
                    style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                IconButton(onClick = {
                    onDeleteGoal()
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Goal",
                        tint = Color(0xFFFF5252)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Current progress
                Text(
                    text = "Saved: ${goal.formattedSaved} of ${goal.formattedTarget} (${goal.progressPercent}%)",
                    style = SpendlyTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SpendlyProgressBar(
                    progress = goal.progressFraction,
                    progressColor = if (colors.isDark) ElectricLime else Color(0xFF2E7D32),
                    height = 6.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Deposit / Withdraw Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .background(if (isDeposit) (if (colors.isDark) ElectricLime else Color(0xFF09142A)) else colors.surface)
                            .clickable { isDeposit = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Deposit",
                            style = SpendlyTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isDeposit) (if (colors.isDark) Color(0xFF09142A) else Color.White) else colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .background(if (!isDeposit) (if (colors.isDark) ElectricLime else Color(0xFF09142A)) else colors.surface)
                            .clickable { isDeposit = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "- Withdraw",
                            style = SpendlyTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (!isDeposit) (if (colors.isDark) Color(0xFF09142A) else Color.White) else colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                Text(
                    text = "Amount (₹)",
                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2000", color = colors.textMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Enter an amount greater than ₹0", color = Color(0xFFFF5252)) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (colors.isDark) ElectricLime else colors.textPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = if (colors.isDark) ElectricLime else colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note
                Text(
                    text = "Note (Optional)",
                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. October monthly savings", color = colors.textMuted) },
                    singleLine = true,
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
                text = if (isDeposit) "Record Deposit" else "Record Withdrawal",
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        amountError = true
                    } else {
                        onSaveContribution(amount, isDeposit, noteText.trim().takeIf { it.isNotBlank() })
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
