package com.spendly.app.ui.sms

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.domain.model.formatPaiseToRupees
import com.spendly.app.sms.DetectedSmsItem
import com.spendly.app.sms.SmsScanResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSyncBottomSheet(
    viewModel: SmsSyncViewModel,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = SpendlyTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanInbox(context)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(PillShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) ElectricLime.copy(alpha = 0.15f) else colors.card),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = if (colors.isDark) ElectricLime else colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "UPI & SMS Expense Tracker",
                            style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Auto-identifies amounts spent via UPI & Banks",
                            style = SpendlyTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            viewModel.reset()
                            onDismissRequest()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is SmsSyncUiState.Idle -> {
                    IdleSmsView(
                        onScanClick = { viewModel.checkAndScan(context) },
                        onSimulate = { sender, msg -> viewModel.simulateSms(sender, msg) }
                    )
                }
                is SmsSyncUiState.PermissionRequired -> {
                    PermissionRequiredView(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                        onTestWithoutPermission = { sender, msg -> viewModel.simulateSms(sender, msg) }
                    )
                }
                is SmsSyncUiState.Scanning -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = if (colors.isDark) ElectricLime else colors.textPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Reading bank & UPI messages...",
                                style = SpendlyTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Extracting amounts, merchants, categories & UTRs",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                is SmsSyncUiState.Preview -> {
                    SmsPreviewView(
                        result = state.result,
                        onToggle = { viewModel.toggleItem(it) },
                        onConfirmSync = { viewModel.syncSelected() }
                    )
                }
                is SmsSyncUiState.Syncing -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = if (colors.isDark) ElectricLime else colors.textPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Saving ${state.totalCount} transactions to Spendly...",
                                style = SpendlyTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
                is SmsSyncUiState.Success -> {
                    SmsSuccessView(
                        count = state.count,
                        totalSpentPaise = state.totalSpentPaise,
                        totalIncomePaise = state.totalIncomePaise,
                        onDone = {
                            coroutineScope.launch {
                                sheetState.hide()
                                viewModel.reset()
                                onDismissRequest()
                            }
                        }
                    )
                }
                is SmsSyncUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Scan Notice",
                            style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = SpendlyTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        SpendlyButton(
                            text = "Try Sample Indian Bank SMS",
                            onClick = {
                                viewModel.simulateSms(
                                    "AD-HDFCBK",
                                    "Sent Rs.450.00 from HDFC Bank A/C **1234 to SWIGGY on 24-10-26 via UPI Ref 429812345678."
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleSmsView(
    onScanClick: () -> Unit,
    onSimulate: (String, String) -> Unit
) {
    val colors = SpendlyTheme.colors
    var customSmsText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SpendlyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onScanClick() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) ElectricLime.copy(alpha = 0.15f) else colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = if (colors.isDark) ElectricLime else colors.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan Device SMS Inbox",
                        style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reads HDFC, SBI, ICICI, Axis, Paytm, PhonePe & Google Pay SMS",
                        style = SpendlyTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        item {
            Text(
                text = "TRY REAL INDIAN BANK SMS SAMPLES",
                style = SpendlyTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textMuted
            )
        }

        val samples = listOf(
            Triple(
                "AD-HDFCBK",
                "Sent Rs.450.00 from HDFC Bank A/C **1234 to SWIGGY on 24-10-26 via UPI Ref 429812345678. Avl bal: Rs.24,500.00",
                "HDFC UPI: ₹450 to Swiggy"
            ),
            Triple(
                "VK-SBIINB",
                "Dear SBI User, your A/c ending with 5678 has been debited by Rs 380.00 on 24/10/26 transfer to ZOMATO UPI Ref No 429812001122.",
                "SBI UPI: ₹380 to Zomato"
            ),
            Triple(
                "JM-ICICIB",
                "Dear Customer, Account 1234 debited with INR 1,299.00 on 25-Oct-26. Info: UPI/CR/429812345679/AMAZON/ICICI. Avl Bal: INR 35,000.00.",
                "ICICI: ₹1,299 at Amazon"
            ),
            Triple(
                "BP-PAYTM",
                "Paid Rs.250.00 to Chai Point on Paytm. UPI Ref: 429812999888.",
                "Paytm: ₹250 to Chai Point"
            ),
            Triple(
                "VM-HDFCBK",
                "UPDATE: Rs.42,000.00 credited to your HDFC Bank A/c **4321 on 26-OCT-26 by TECH CORP (Salary). Avl Bal: INR 65,400.00",
                "Salary: ₹42,000 Credit"
            )
        )

        items(samples) { (sender, sms, title) ->
            SpendlyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSimulate(sender, sms) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sms,
                            style = SpendlyTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Test",
                        style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (colors.isDark) ElectricLime else colors.textPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "PASTE OR TYPE ANY BANK SMS TO TEST",
                style = SpendlyTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = customSmsText,
                    onValueChange = { customSmsText = it },
                    placeholder = { Text("Paste your bank SMS here...", color = colors.textMuted) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (colors.isDark) ElectricLime else colors.textPrimary,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (customSmsText.isNotBlank()) {
                            onSimulate("BANK-SMS", customSmsText)
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (colors.isDark) ElectricLime else Color(0xFF09142A))
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Test",
                        tint = if (colors.isDark) Color(0xFF09142A) else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionRequiredView(
    onRequestPermission: () -> Unit,
    onTestWithoutPermission: (String, String) -> Unit
) {
    val colors = SpendlyTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background((if (colors.isDark) ElectricLime else Color(0xFF2563EB)).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = if (colors.isDark) ElectricLime else Color(0xFF2563EB),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SMS Permission Needed",
            style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Spendly scans incoming bank notifications entirely on your device to automatically track your UPI spending. Your messages never leave your phone.",
            style = SpendlyTheme.typography.bodyMedium,
            color = colors.textSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        SpendlyButton(
            text = "Grant SMS Access",
            onClick = onRequestPermission,
            style = SpendlyButtonStyle.PRIMARY_LIME
        )

        Spacer(modifier = Modifier.height(12.dp))

        SpendlyButton(
            text = "Or Test with Sample Bank SMS",
            onClick = {
                onTestWithoutPermission(
                    "AD-HDFCBK",
                    "Sent Rs.450.00 from HDFC Bank A/C **1234 to SWIGGY on 24-10-26 via UPI Ref 429812345678."
                )
            },
            style = SpendlyButtonStyle.OUTLINE
        )
    }
}

@Composable
private fun SmsPreviewView(
    result: SmsScanResult,
    onToggle: (Long) -> Unit,
    onConfirmSync: () -> Unit
) {
    val colors = SpendlyTheme.colors
    val selectedCount = result.items.count { it.isSelected && it.transaction != null }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Card
        SpendlyCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${result.validNewCount} New Transactions",
                        style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (colors.isDark) ElectricLime else Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Total Spent: ${formatPaiseToRupees(result.totalSpentPaise)}",
                        style = SpendlyTheme.typography.bodySmall,
                        color = colors.textPrimary
                    )
                    if (result.totalIncomePaise > 0L) {
                        Text(
                            text = "Income: +${formatPaiseToRupees(result.totalIncomePaise)}",
                            style = SpendlyTheme.typography.bodySmall,
                            color = Color(0xFF00C853)
                        )
                    }
                }

                if (result.duplicateCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${result.duplicateCount} Dupes Skipped",
                            style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(result.items, key = { it.smsId }) { item ->
                val tx = item.transaction
                if (tx != null) {
                    val (icon, catColor) = getCategoryIconAndColor(tx.categoryName)
                    val formattedDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(tx.timestampMs))

                    SpendlyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !item.isDuplicate) { onToggle(item.smsId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isSelected,
                                onCheckedChange = { onToggle(item.smsId) },
                                enabled = !item.isDuplicate,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (colors.isDark) ElectricLime else Color(0xFF09142A),
                                    checkmarkColor = if (colors.isDark) Color(0xFF09142A) else Color.White
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            SpendlyCategoryBadge(
                                icon = icon,
                                backgroundColor = catColor,
                                size = 38.dp,
                                iconSize = 18.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tx.counterpartyName,
                                        style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = "${if (tx.isIncoming) "+" else "-"}${tx.formattedRupees}",
                                        style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (tx.isIncoming) Color(0xFF00C853) else colors.textPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${tx.paymentApp ?: "UPI"} • $formattedDate",
                                        style = SpendlyTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colors.textSecondary
                                    )

                                    if (item.isDuplicate) {
                                        Text(
                                            text = "DUPLICATE",
                                            style = SpendlyTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF9800)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SpendlyButton(
            text = if (selectedCount > 0) "Sync $selectedCount Transactions to Spendly" else "Select Transactions to Sync",
            onClick = onConfirmSync,
            style = SpendlyButtonStyle.PRIMARY_LIME,
            enabled = selectedCount > 0,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SmsSuccessView(
    count: Int,
    totalSpentPaise: Long,
    totalIncomePaise: Long,
    onDone: () -> Unit
) {
    val colors = SpendlyTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background((if (colors.isDark) ElectricLime else Color(0xFF2E7D32)).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (colors.isDark) ElectricLime else Color(0xFF2E7D32),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Sync Complete!",
            style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 24.sp),
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Successfully imported $count transactions into your Spendly ledger.\nTotal Spent: ${formatPaiseToRupees(totalSpentPaise)}",
            style = SpendlyTheme.typography.bodyMedium,
            color = colors.textSecondary,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        SpendlyButton(
            text = "View in Transactions",
            onClick = onDone,
            style = SpendlyButtonStyle.PRIMARY_LIME,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
