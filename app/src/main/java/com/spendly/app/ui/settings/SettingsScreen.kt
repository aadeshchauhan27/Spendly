package com.spendly.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.ThemeMode
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.ui.import_export.ImportPreviewBottomSheet
import com.spendly.app.ui.import_export.ImportViewModel
import com.spendly.app.ui.sms.SmsSyncBottomSheet
import com.spendly.app.ui.sms.SmsSyncViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    repository: SpendlyRepository,
    importViewModel: ImportViewModel,
    smsSyncViewModel: SmsSyncViewModel,
    onThemeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val coroutineScope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDataDialog by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showSmsSheet by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val importHistory by importViewModel.importHistory.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // Title
        Text(
            text = "Settings",
            style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 26.sp),
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .border(1.5.dp, if (colors.isDark) ElectricLime else colors.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Aadesh Chauhan",
                                style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "aadesh@gmail.com",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            // Settings Group
            item {
                SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        SettingsOptionRow(
                            icon = Icons.Default.Security,
                            title = "Account & Privacy",
                            onClick = {}
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsOptionRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications & Alerts",
                            onClick = {}
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsOptionRow(
                            icon = Icons.Default.Lock,
                            title = "App Lock",
                            value = "On",
                            onClick = {}
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsOptionRow(
                            icon = Icons.Default.ColorLens,
                            title = "Appearance",
                            value = when (currentTheme) {
                                ThemeMode.SYSTEM -> "System"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            onClick = { showThemeDialog = true }
                        )
                    }
                }
            }

            // Data & Support Group
            item {
                SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        SettingsOptionRow(
                            icon = Icons.Default.Storage,
                            title = "Data & Backup",
                            onClick = { showDataDialog = true }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsOptionRow(
                            icon = Icons.Default.HelpOutline,
                            title = "Help & Support",
                            onClick = {}
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsOptionRow(
                            icon = Icons.Default.Info,
                            title = "About Spendly",
                            value = "v1.0.0",
                            onClick = {}
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = "Choose Appearance",
                    style = SpendlyTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChanged(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == mode,
                                onClick = {
                                    onThemeChanged(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (colors.isDark) ElectricLime else Color(0xFF09142A)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "System Default"
                                    ThemeMode.LIGHT -> "Light Theme"
                                    ThemeMode.DARK -> "Dark Theme"
                                },
                                style = SpendlyTheme.typography.bodyLarge,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = colors.card
        )
    }

    // Data & Backup Action Dialog
    if (showDataDialog) {
        AlertDialog(
            onDismissRequest = { showDataDialog = false },
            title = {
                Text(
                    text = "Data & Statements",
                    style = SpendlyTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // SMS Auto-Track Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDataDialog = false
                                showSmsSheet = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = if (colors.isDark) ElectricLime else colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Scan SMS & UPI Alerts",
                                style = SpendlyTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Auto-detects amounts spent via UPI & Banks",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Import Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDataDialog = false
                                showImportSheet = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = if (colors.isDark) ElectricLime else colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import CSV Statement",
                                style = SpendlyTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "HDFC, SBI, ICICI, PhonePe, Paytm",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Import History Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDataDialog = false
                                showHistoryDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import History",
                                style = SpendlyTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${importHistory.size} past import sessions",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Sync SMS Messages Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDataDialog = false
                                showSmsSheet = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sync SMS Transactions",
                                style = SpendlyTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Scan bank SMS inbox for UPI spends and income",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                    // Clear Demo Data Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDataDialog = false
                                coroutineScope.launch {
                                    repository.clearDemoData()
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Clear Demo Data",
                                style = SpendlyTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFFFF5252)
                            )
                            Text(
                                text = "Removes all demo transactions",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDataDialog = false }) {
                    Text("Close", color = colors.textPrimary)
                }
            },
            containerColor = colors.card
        )
    }

    // Import History Dialog
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Text(
                    text = "Import History",
                    style = SpendlyTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                if (importHistory.isEmpty()) {
                    Text(
                        text = "No import sessions yet. Import a statement CSV to see logs here.",
                        style = SpendlyTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(importHistory.size) { idx ->
                            val session = importHistory[idx]
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date(session.timestampMs))
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = session.fileName,
                                    style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$dateStr • ${session.importedCount} imported, ${session.duplicateCount} dupes",
                                    style = SpendlyTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                if (idx < importHistory.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.3f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Done", color = if (colors.isDark) ElectricLime else colors.textPrimary)
                }
            },
            containerColor = colors.card
        )
    }

    // Import Preview Bottom Sheet
    if (showImportSheet) {
        ImportPreviewBottomSheet(
            viewModel = importViewModel,
            onDismissRequest = { showImportSheet = false }
        )
    }

    // SMS Sync Bottom Sheet
    if (showSmsSheet) {
        SmsSyncBottomSheet(
            viewModel = smsSyncViewModel,
            onDismissRequest = { showSmsSheet = false }
        )
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    val colors = SpendlyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    style = SpendlyTheme.typography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
