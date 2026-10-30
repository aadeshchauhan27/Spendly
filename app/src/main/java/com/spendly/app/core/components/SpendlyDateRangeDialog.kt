package com.spendly.app.core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    onDateRangeSelected: (startMs: Long, endMs: Long) -> Unit
) {
    val colors = SpendlyTheme.colors
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartDateMs,
        initialSelectedEndDateMillis = initialEndDateMs
    )

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
                Text(
                    text = "Select Date Range",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 24.dp),
                    style = SpendlyTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
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
