package com.spendly.app.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.auth.SpendlyAuthManager
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.preferences.UserPreferences
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.StatusExpenseRed
import com.spendly.app.core.theme.StatusIncomeGreen
import com.spendly.app.core.theme.ThemeMode
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.data.sync.CloudSyncStatus
import com.spendly.app.data.sync.SpendlyCloudSyncManager
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
    userPreferences: UserPreferences,
    authManager: SpendlyAuthManager,
    cloudSyncManager: SpendlyCloudSyncManager,
    importViewModel: ImportViewModel,
    smsSyncViewModel: SmsSyncViewModel,
    onThemeChanged: (ThemeMode) -> Unit,
    onAppTourClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showDataDialog by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showSmsSheet by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val importHistory by importViewModel.importHistory.collectAsState()
    val googleEmail by userPreferences.googleAccountEmailFlow.collectAsState(initial = null)
    val googleName by userPreferences.googleAccountNameFlow.collectAsState(initial = null)
    val isCloudSyncEnabled by userPreferences.isCloudSyncEnabledFlow.collectAsState(initial = false)
    val lastSyncTime by userPreferences.lastCloudSyncTimeFlow.collectAsState(initial = 0L)
    val syncStatus by cloudSyncManager.syncStatus.collectAsState()

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                val authResult = authManager.handleAccountPickerResult(result.data)
                if (authResult.isSuccess) {
                    val info = authResult.getOrNull()
                    Toast.makeText(context, "Signed in as ${info?.email}", Toast.LENGTH_SHORT).show()
                    if (isCloudSyncEnabled) {
                        cloudSyncManager.syncNow()
                    }
                } else {
                    Toast.makeText(
                        context,
                        authResult.exceptionOrNull()?.message ?: "Sign-in failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

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
            // 1. Google Account / Profile Card
            item {
                SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                if (!googleEmail.isNullOrBlank()) {
                                    Text(
                                        text = (googleName ?: googleEmail ?: "G").take(1).uppercase(Locale.ENGLISH),
                                        style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (colors.isDark) ElectricLime else colors.textPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = googleName ?: "Google Account",
                                    style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = googleEmail ?: "Sign in to backup & restore your data",
                                    style = SpendlyTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (googleEmail.isNullOrBlank()) {
                            SpendlyButton(
                                text = "Sign in with Google",
                                onClick = {
                                    try {
                                        accountPickerLauncher.launch(authManager.getGoogleSignInIntent())
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open Google sign-in: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                style = SpendlyButtonStyle.PRIMARY_LIME,
                                modifier = Modifier.fillMaxWidth(),
                                icon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SpendlyButton(
                                    text = "Switch Account",
                                    onClick = {
                                        try {
                                            accountPickerLauncher.launch(authManager.getGoogleSignInIntent())
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    style = SpendlyButtonStyle.OUTLINE,
                                    modifier = Modifier.weight(1f)
                                )
                                SpendlyButton(
                                    text = "Sign Out",
                                    onClick = {
                                        coroutineScope.launch {
                                            userPreferences.signOutGoogle()
                                            Toast.makeText(context, "Signed out from Google", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    style = SpendlyButtonStyle.OUTLINE,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Cloud Backup & Sync Card (Firestore)
            item {
                SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (colors.isDark) ElectricLime else Color(0xFF0284C7),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Cloud Backup & Sync",
                                        style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Private encrypted Firestore backup",
                                        style = SpendlyTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }

                            Switch(
                                checked = isCloudSyncEnabled,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        if (enabled && googleEmail.isNullOrBlank()) {
                                            Toast.makeText(context, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
                                            accountPickerLauncher.launch(authManager.getGoogleSignInIntent())
                                        } else {
                                            userPreferences.setCloudSyncEnabled(enabled)
                                            if (enabled) {
                                                cloudSyncManager.syncNow()
                                            }
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (colors.isDark) ElectricLime else Color(0xFF09142A),
                                    checkedTrackColor = if (colors.isDark) ElectricLime.copy(alpha = 0.35f) else Color(0xFF09142A).copy(alpha = 0.35f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val lastSyncLabel = if (lastSyncTime > 0L) {
                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            "Last backed up: ${sdf.format(Date(lastSyncTime))}"
                        } else {
                            "Status: Not backed up yet"
                        }

                        Text(
                            text = lastSyncLabel,
                            style = SpendlyTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpendlyButton(
                                text = if (syncStatus == CloudSyncStatus.SYNCING) "Syncing..." else "Sync Now",
                                onClick = {
                                    coroutineScope.launch {
                                        if (googleEmail.isNullOrBlank()) {
                                            Toast.makeText(context, "Please sign in with Google to backup", Toast.LENGTH_SHORT).show()
                                            accountPickerLauncher.launch(authManager.getGoogleSignInIntent())
                                            return@launch
                                        }
                                        userPreferences.setCloudSyncEnabled(true)
                                        val result = cloudSyncManager.syncNow()
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Backup uploaded to Firestore successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Sync error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                style = SpendlyButtonStyle.PRIMARY_LIME,
                                modifier = Modifier.weight(1f),
                                icon = {
                                    if (syncStatus == CloudSyncStatus.SYNCING) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = colors.textPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            )

                            SpendlyButton(
                                text = "Restore",
                                onClick = {
                                    if (googleEmail.isNullOrBlank()) {
                                        Toast.makeText(context, "Please sign in with Google to restore", Toast.LENGTH_SHORT).show()
                                        accountPickerLauncher.launch(authManager.getGoogleSignInIntent())
                                    } else {
                                        showRestoreConfirmDialog = true
                                    }
                                },
                                style = SpendlyButtonStyle.OUTLINE,
                                modifier = Modifier.weight(1f),
                                icon = {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            )
                        }
                    }
                }
            }

            // 3. Settings Group: Features, Security, Notifications, Appearance
            item {
                SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        SettingsOptionRow(
                            icon = Icons.Default.Explore,
                            title = "App Features Tour",
                            value = "View",
                            onClick = onAppTourClick
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

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

            // 4. Data & Support Group
            item {
                SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        SettingsOptionRow(
                            icon = Icons.Default.Storage,
                            title = "Data & Statements",
                            onClick = { showDataDialog = true }
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsOptionRow(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "Help & Support",
                            onClick = {}
                        )
                        HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsOptionRow(
                            icon = Icons.Default.Info,
                            title = "About Spendly",
                            value = "v1.2.0",
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

    // Restore Backup Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = {
                Text(
                    text = "Restore Cloud Backup?",
                    style = SpendlyTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will restore your transactions, categories, budgets and savings goals from Firestore account (${googleEmail}). Existing items will be merged safely.",
                    style = SpendlyTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        coroutineScope.launch {
                            val res = cloudSyncManager.restoreFromCloud()
                            if (res.isSuccess) {
                                Toast.makeText(context, "Data restored successfully from cloud!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Restore failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Restore Now", color = if (colors.isDark) ElectricLime else colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            },
            containerColor = colors.card
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
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

    // Data & Statements Action Dialog
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Manage your financial transactions, statements and data import/export.",
                        style = SpendlyTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsDialogActionRow(
                        icon = Icons.Default.FileUpload,
                        title = "Import Bank Statement (CSV)",
                        subtitle = "HDFC, SBI, ICICI, Axis and more",
                        onClick = {
                            showDataDialog = false
                            showImportSheet = true
                        }
                    )

                    SettingsDialogActionRow(
                        icon = Icons.Default.History,
                        title = "Statement Import History",
                        subtitle = "${importHistory.size} statement(s) imported",
                        onClick = {
                            showDataDialog = false
                            showHistoryDialog = true
                        }
                    )

                    SettingsDialogActionRow(
                        icon = Icons.Default.Message,
                        title = "Sync SMS Messages",
                        subtitle = "Re-scan device inbox for UPI transactions",
                        onClick = {
                            showDataDialog = false
                            showSmsSheet = true
                        }
                    )

                    SettingsDialogActionRow(
                        icon = Icons.Default.Delete,
                        title = "Clear All Transactions",
                        subtitle = "Permanently wipe stored transactions",
                        isDestructive = true,
                        onClick = {
                            showDataDialog = false
                            coroutineScope.launch {
                                repository.clearAllTransactions()
                                Toast.makeText(context, "Transactions cleared", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            },
            confirmButton = {
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
                        text = "No statements have been imported yet.",
                        style = SpendlyTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(importHistory.size) { index ->
                            val item = importHistory[index]
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("en", "IN"))
                            SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.fileName,
                                            style = SpendlyTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = dateFormat.format(Date(item.timestampMs)),
                                            style = SpendlyTheme.typography.bodySmall,
                                            color = colors.textMuted
                                        )
                                        Text(
                                            text = "${item.importedCount} added, ${item.duplicateCount} duplicates",
                                            style = SpendlyTheme.typography.bodySmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Text(
                                        text = item.source,
                                        style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = StatusIncomeGreen
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Done", color = colors.textPrimary)
                }
            },
            containerColor = colors.card
        )
    }

    // Import BottomSheet
    if (showImportSheet) {
        ImportPreviewBottomSheet(
            viewModel = importViewModel,
            onDismissRequest = { showImportSheet = false }
        )
    }

    // SMS Sync BottomSheet
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

@Composable
private fun SettingsDialogActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = SpendlyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) StatusExpenseRed else colors.textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SpendlyTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = if (isDestructive) StatusExpenseRed else colors.textPrimary
            )
            Text(
                text = subtitle,
                style = SpendlyTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
    }
}
