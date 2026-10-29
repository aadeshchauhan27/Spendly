package com.spendly.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spendly.app.core.theme.DarkNavyBackground
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.StatusExpenseRed

enum class SpendlyButtonStyle {
    PRIMARY_LIME,
    SECONDARY_DARK,
    OUTLINE,
    DESTRUCTIVE
}

@Composable
fun SpendlyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SpendlyButtonStyle = SpendlyButtonStyle.PRIMARY_LIME,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    val buttonModifier = if (fullWidth) modifier.fillMaxWidth().height(52.dp) else modifier.height(52.dp)

    when (style) {
        SpendlyButtonStyle.PRIMARY_LIME -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricLime,
                    contentColor = DarkNavyBackground,
                    disabledContainerColor = ElectricLime.copy(alpha = 0.4f),
                    disabledContentColor = DarkNavyBackground.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                icon?.invoke()
                Text(
                    text = text,
                    style = SpendlyTheme.typography.labelLarge
                )
            }
        }
        SpendlyButtonStyle.SECONDARY_DARK -> {
            val colors = SpendlyTheme.colors
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (colors.isDark) Color(0xFF142B52) else Color(0xFF09142A),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                icon?.invoke()
                Text(
                    text = text,
                    style = SpendlyTheme.typography.labelLarge
                )
            }
        }
        SpendlyButtonStyle.OUTLINE -> {
            val colors = SpendlyTheme.colors
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = PillShape,
                border = BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.textPrimary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                icon?.invoke()
                Text(
                    text = text,
                    style = SpendlyTheme.typography.labelLarge
                )
            }
        }
        SpendlyButtonStyle.DESTRUCTIVE -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled,
                shape = PillShape,
                border = BorderStroke(1.dp, StatusExpenseRed.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = StatusExpenseRed.copy(alpha = 0.08f),
                    contentColor = StatusExpenseRed
                ),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                icon?.invoke()
                Text(
                    text = text,
                    style = SpendlyTheme.typography.labelLarge
                )
            }
        }
    }
}
