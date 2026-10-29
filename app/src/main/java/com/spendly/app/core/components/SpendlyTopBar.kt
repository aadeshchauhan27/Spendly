package com.spendly.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme

@Composable
fun SpendlyHeader(
    userName: String = "Aadesh",
    greetingSubtitle: String = "Small steps. Big savings.",
    onToggleTheme: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hey $userName 👋",
                style = SpendlyTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Text(
                text = greetingSubtitle,
                style = SpendlyTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        // Theme Toggle Button
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.dp, colors.border, CircleShape)
        ) {
            Icon(
                imageVector = if (colors.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                tint = if (colors.isDark) ElectricLime else colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Profile Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.5.dp, if (colors.isDark) ElectricLime else colors.border, CircleShape)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SpendlyDetailTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null
) {
    val colors = SpendlyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.dp, colors.border, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        actions?.invoke()
    }
}
