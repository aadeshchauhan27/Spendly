package com.spendly.app.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.StatusIncomeGreen

@Composable
fun SpendlyTransactionRow(
    title: String,
    categoryName: String,
    amountText: String,
    timeText: String,
    isIncoming: Boolean,
    icon: ImageVector = Icons.Default.Category,
    iconBackground: Color = Color.Transparent,
    iconName: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = SpendlyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpendlyCategoryBadge(
            categoryName = categoryName,
            iconName = iconName,
            icon = icon,
            backgroundColor = iconBackground,
            size = 42.dp,
            iconSize = 20.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            Text(
                text = categoryName,
                style = SpendlyTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val amountColor = if (isIncoming) StatusIncomeGreen else colors.textPrimary
            val prefix = if (isIncoming) "+ " else "- "
            Text(
                text = "$prefix$amountText",
                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
            Text(
                text = timeText,
                style = SpendlyTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}
