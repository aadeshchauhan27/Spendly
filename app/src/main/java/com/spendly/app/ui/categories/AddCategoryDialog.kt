package com.spendly.app.ui.categories

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spendly.app.core.components.AVAILABLE_CATEGORY_COLORS
import com.spendly.app.core.components.AVAILABLE_CATEGORY_ICONS
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme

@Composable
fun AddCategoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, type: String, colorHex: String, iconName: String) -> Unit
) {
    val colors = SpendlyTheme.colors
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var selectedColor by remember { mutableStateOf(AVAILABLE_CATEGORY_COLORS.first()) }
    var selectedIconName by remember { mutableStateOf(AVAILABLE_CATEGORY_ICONS.first().first) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Create Category",
                    style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Type Segmented Control
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .background(if (colors.isDark) Color(0xFF0D1B36) else Color(0xFFEEF2F6))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val types = listOf("EXPENSE" to "Expense", "INCOME" to "Income")
                        types.forEach { (typeKey, typeLabel) ->
                            val isSelected = selectedType == typeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PillShape)
                                    .background(if (isSelected) (if (colors.isDark) Color(0xFF1A2F5A) else Color.White) else Color.Transparent)
                                    .clickable { selectedType = typeKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    style = SpendlyTheme.typography.labelMedium,
                                    color = if (isSelected) (if (colors.isDark) ElectricLime else Color(0xFF09142A)) else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Subscriptions, Gym, Pet Care") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricLime,
                        unfocusedBorderColor = colors.border,
                        focusedLabelColor = ElectricLime,
                        unfocusedLabelColor = colors.textSecondary,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF5252),
                        style = SpendlyTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Select Icon
                Text(
                    text = "Select Icon",
                    style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AVAILABLE_CATEGORY_ICONS) { (iconKey, iconVector) ->
                        val isSelected = selectedIconName == iconKey
                        val iconBg = if (isSelected) ElectricLime else (if (colors.isDark) Color(0xFF162544) else Color(0xFFE2E8F0))
                        val iconTint = if (isSelected) Color(0xFF08152E) else colors.textPrimary

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(iconBg)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) ElectricLime else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedIconName = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = iconKey,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Select Color
                Text(
                    text = "Select Color",
                    style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AVAILABLE_CATEGORY_COLORS) { hex ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            Color(0xFF546E7A)
                        }
                        val isSelected = selectedColor == hex

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpendlyButton(
                        text = "Cancel",
                        onClick = onDismissRequest,
                        style = SpendlyButtonStyle.OUTLINE,
                        modifier = Modifier.weight(1f)
                    )

                    SpendlyButton(
                        text = "Save",
                        onClick = {
                            if (name.trim().isBlank()) {
                                errorMessage = "Please enter a category name"
                            } else {
                                onConfirm(
                                    name.trim(),
                                    selectedType,
                                    selectedColor,
                                    selectedIconName
                                )
                            }
                        },
                        style = SpendlyButtonStyle.PRIMARY_LIME,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
