package com.spendly.app.core.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendlyDateRangeDialog(
    initialStartDateMs: Long? = null,
    initialEndDateMs: Long? = null,
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (startMs: Long, endMs: Long) -> Unit,
    onClearDateRange: (() -> Unit)? = null
) {
    val colors = SpendlyTheme.colors
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDateMs,
        initialSelectedEndDateMillis = initialEndDateMs
    )

    var activeShortcut by remember {
        mutableStateOf<String?>(
            if (initialStartDateMs == null && initialEndDateMs == null) "All Time" else null
        )
    }

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            SpendlyButton(
                text = "Apply",
                fullWidth = false,
                style = SpendlyButtonStyle.PRIMARY_LIME,
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis ?: start
                    if (start != null && end != null) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = end
                        cal.set(Calendar.HOUR_OF_DAY, 23)
                        cal.set(Calendar.MINUTE, 59)
                        cal.set(Calendar.SECOND, 59)
                        cal.set(Calendar.MILLISECOND, 999)
                        onDateRangeSelected(start, cal.timeInMillis)
                    } else {
                        onClearDateRange?.invoke()
                    }
                    onDismissRequest()
                }
            )
        },
        dismissButton = {
            SpendlyButton(
                text = "Cancel",
                fullWidth = false,
                style = SpendlyButtonStyle.OUTLINE,
                onClick = onDismissRequest
            )
        },
        colors = DatePickerDefaults.colors(
            containerColor = colors.card
        )
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "Select Date Range",
                        style = SpendlyTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shortcut Period Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All Time
                        SpendlyChip(
                            text = "All Time",
                            selected = activeShortcut == "All Time" ||
                                    (dateRangePickerState.selectedStartDateMillis == null && dateRangePickerState.selectedEndDateMillis == null),
                            onClick = {
                                activeShortcut = "All Time"
                                dateRangePickerState.setSelection(null, null)
                            }
                        )

                        // 7D
                        SpendlyChip(
                            text = "7D",
                            selected = activeShortcut == "7D",
                            onClick = {
                                activeShortcut = "7D"
                                val now = System.currentTimeMillis()
                                val start = now - (7 * 86400000L)
                                dateRangePickerState.setSelection(start, now)
                            }
                        )

                        // 30D
                        SpendlyChip(
                            text = "30D",
                            selected = activeShortcut == "30D",
                            onClick = {
                                activeShortcut = "30D"
                                val now = System.currentTimeMillis()
                                val start = now - (30 * 86400000L)
                                dateRangePickerState.setSelection(start, now)
                            }
                        )

                        // 60D
                        SpendlyChip(
                            text = "60D",
                            selected = activeShortcut == "60D",
                            onClick = {
                                activeShortcut = "60D"
                                val now = System.currentTimeMillis()
                                val start = now - (60 * 86400000L)
                                dateRangePickerState.setSelection(start, now)
                            }
                        )

                        // 90D
                        SpendlyChip(
                            text = "90D",
                            selected = activeShortcut == "90D",
                            onClick = {
                                activeShortcut = "90D"
                                val now = System.currentTimeMillis()
                                val start = now - (90 * 86400000L)
                                dateRangePickerState.setSelection(start, now)
                            }
                        )

                        // This Year
                        SpendlyChip(
                            text = "This Year",
                            selected = activeShortcut == "This Year",
                            onClick = {
                                activeShortcut = "This Year"
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.MONTH, Calendar.JANUARY)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val start = cal.timeInMillis
                                val now = System.currentTimeMillis()
                                dateRangePickerState.setSelection(start, now)
                            }
                        )
                    }
                }
            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = colors.card,
                titleContentColor = colors.textPrimary,
                headlineContentColor = colors.textPrimary,
                weekdayContentColor = colors.textSecondary,
                subheadContentColor = colors.textSecondary,
                dayContentColor = colors.textPrimary,
                selectedDayContainerColor = ElectricLime,
                selectedDayContentColor = Color(0xFF09142A),
                dayInSelectionRangeContainerColor = ElectricLime.copy(alpha = 0.25f),
                dayInSelectionRangeContentColor = colors.textPrimary
            )
        )
    }
}
