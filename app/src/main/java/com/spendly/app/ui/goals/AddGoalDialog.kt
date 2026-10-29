package com.spendly.app.ui.goals

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, targetRupees: Double, iconName: String, colorHex: String) -> Unit
) {
    val colors = SpendlyTheme.colors
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("plant") }
    var selectedColor by remember { mutableStateOf("#C8FF5A") }
    var nameError by remember { mutableStateOf(false) }
    var targetError by remember { mutableStateOf(false) }

    val iconOptions = listOf(
        "plant" to Icons.Default.LocalFlorist,
        "flight" to Icons.Default.Flight,
        "laptop" to Icons.Default.Laptop,
        "car" to Icons.Default.DirectionsCar,
        "home" to Icons.Default.Home,
        "savings" to Icons.Default.Savings
    )

    val colorOptions = listOf(
        "#C8FF5A", // Electric Lime
        "#38BDF8", // Sky Blue
        "#818CF8", // Indigo
        "#F472B6", // Pink
        "#FB923C", // Orange
        "#4ADE80"  // Emerald Green
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Savings Goal",
                style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Goal Name
                Text(
                    text = "Goal Title",
                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Emergency Fund, Goa Trip", color = colors.textMuted) },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Goal title cannot be empty", color = Color(0xFFFF5252)) }
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

                // Target Amount
                Text(
                    text = "Target Amount (₹)",
                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetText,
                    onValueChange = {
                        targetText = it
                        targetError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 50000", color = colors.textMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = targetError,
                    supportingText = if (targetError) {
                        { Text("Enter a valid target greater than ₹0", color = Color(0xFFFF5252)) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (colors.isDark) ElectricLime else colors.textPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = if (colors.isDark) ElectricLime else colors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Icon Picker
                Text(
                    text = "Choose Icon",
                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    iconOptions.forEach { (key, iconVector) ->
                        val isSelected = selectedIcon == key
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) (if (colors.isDark) ElectricLime else Color(0xFF09142A)) else colors.surface)
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = key,
                                tint = if (isSelected) (if (colors.isDark) Color(0xFF09142A) else Color.White) else colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Picker
                Text(
                    text = "Accent Color",
                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { hex ->
                        val isSelected = selectedColor == hex
                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            SpendlyButton(
                text = "Create Goal",
                onClick = {
                    val amount = targetText.toDoubleOrNull()
                    if (name.isBlank()) {
                        nameError = true
                    } else if (amount == null || amount <= 0.0) {
                        targetError = true
                    } else {
                        onSave(name.trim(), amount, selectedIcon, selectedColor)
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
