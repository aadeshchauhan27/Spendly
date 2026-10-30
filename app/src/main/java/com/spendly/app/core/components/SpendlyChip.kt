package com.spendly.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.spendly.app.core.theme.DarkNavyBackground
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme

@Composable
fun SpendlyChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val colors = SpendlyTheme.colors
    val haptic = LocalHapticFeedback.current
    val backgroundColor = if (selected) {
        if (colors.isDark) ElectricLime else Color(0xFF09142A)
    } else {
        if (colors.isDark) Color(0xFF142B52).copy(alpha = 0.5f) else Color(0xFFECEAE2)
    }

    val contentColor = if (selected) {
        if (colors.isDark) DarkNavyBackground else Color.White
    } else {
        colors.textSecondary
    }

    val borderColor = if (selected) Color.Transparent else colors.border

    Box(
        modifier = modifier
            .clip(PillShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, PillShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = SpendlyTheme.typography.labelMedium,
            color = contentColor
        )
    }
}
