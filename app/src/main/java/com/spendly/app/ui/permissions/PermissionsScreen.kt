package com.spendly.app.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme

@Composable
fun PermissionsScreen(
    onAllowClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Shield Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(colors.card),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Privacy Shield",
                        tint = if (colors.isDark) ElectricLime else Color(0xFF09142A),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your privacy\nmatters",
                    style = SpendlyTheme.typography.displayLarge.copy(fontSize = 28.sp),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "To show you accurate insights, we need your permission to access:",
                    style = SpendlyTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // 3 Permission Info Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionItemCard(
                    icon = Icons.Default.Message,
                    badgeColor = Color(0xFFE879F9), // Pink badge from reference
                    title = "SMS (optional)",
                    description = "Read UPI transaction alerts to auto-categorize expenses"
                )

                PermissionItemCard(
                    icon = Icons.Default.Notifications,
                    badgeColor = Color(0xFF34D399), // Mint green badge from reference
                    title = "Notifications",
                    description = "Show budget alerts & reminders"
                )

                PermissionItemCard(
                    icon = Icons.Default.Folder,
                    badgeColor = Color(0xFF38BDF8), // Cyan blue badge from reference
                    title = "Storage",
                    description = "Import statements or screenshots"
                )
            }

            // Bottom Actions: "Allow Access" & "Not now"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SpendlyButton(
                    text = "Allow Access",
                    onClick = onAllowClick,
                    style = if (colors.isDark) SpendlyButtonStyle.PRIMARY_LIME else SpendlyButtonStyle.SECONDARY_DARK
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Not now",
                    style = SpendlyTheme.typography.labelLarge,
                    color = colors.textSecondary,
                    modifier = Modifier.clickable { onSkipClick() }
                )
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    SpendlyCard(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = SpendlyTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
