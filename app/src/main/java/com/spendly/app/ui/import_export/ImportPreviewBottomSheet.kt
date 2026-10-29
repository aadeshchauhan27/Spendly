package com.spendly.app.ui.import_export

import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.SpendlyChip
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.domain.model.ImportRowStatus
import com.spendly.app.domain.model.ParsedImportRow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewBottomSheet(
    viewModel: ImportViewModel,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = SpendlyTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    // Android Storage File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    val content = reader.readText()
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "import.csv"
                    viewModel.parseCsvContent(content, fileName)
                }
            } catch (e: Exception) {
                // Handled in VM or display error
            }
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
                Column {
                    Text(
                        text = "Import Transactions",
                        style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Import UPI and bank statement CSV files",
                        style = SpendlyTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
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
                is ImportUiState.Idle -> {
                    IdleImportView(
                        onPickFileClick = { filePickerLauncher.launch("*/*") },
                        onSampleSelected = { sampleType -> viewModel.loadSample(sampleType) }
                    )
                }
                is ImportUiState.Parsing -> {
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
                                text = "Analyzing ${state.fileName}...",
                                style = SpendlyTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Detecting headers, mapping amounts & checking duplicates",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
                is ImportUiState.Preview -> {
                    PreviewContentView(
                        parseResult = state.parseResult,
                        onToggleRow = { viewModel.toggleRowSelection(it) },
                        onSelectAllNew = { viewModel.selectAllMatching(ImportRowStatus.VALID, true) },
                        onDeselectDuplicates = { viewModel.selectAllMatching(ImportRowStatus.DUPLICATE, false) },
                        onConfirmImport = { viewModel.confirmImport() }
                    )
                }
                is ImportUiState.Importing -> {
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
                                text = "Importing ${state.totalCount} transactions...",
                                style = SpendlyTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
                is ImportUiState.Success -> {
                    SuccessImportView(
                        summary = state.summary,
                        onDone = {
                            coroutineScope.launch {
                                sheetState.hide()
                                viewModel.reset()
                                onDismissRequest()
                            }
                        }
                    )
                }
                is ImportUiState.Error -> {
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
                            text = "Import Error",
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
                            text = "Try Another File",
                            onClick = { viewModel.reset() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleImportView(
    onPickFileClick: () -> Unit,
    onSampleSelected: (SampleBankType) -> Unit
) {
    val colors = SpendlyTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            SpendlyCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickFileClick() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (colors.isDark) ElectricLime.copy(alpha = 0.15f) else colors.card),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = if (colors.isDark) ElectricLime else colors.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Choose CSV File",
                        style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Supports HDFC, SBI, ICICI, PhonePe, Paytm, or generic CSV",
                        style = SpendlyTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "OR TRY A TEST STATEMENT",
                style = SpendlyTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            SampleBankType.values().forEach { sample ->
                SpendlyCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSampleSelected(sample) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = sample.displayName,
                                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Pre-formatted real bank statement test data",
                                style = SpendlyTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Text(
                            text = "Test",
                            style = SpendlyTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (colors.isDark) ElectricLime else colors.textPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PreviewContentView(
    parseResult: com.spendly.app.domain.model.ImportParseResult,
    onToggleRow: (Int) -> Unit,
    onSelectAllNew: () -> Unit,
    onDeselectDuplicates: () -> Unit,
    onConfirmImport: () -> Unit
) {
    val colors = SpendlyTheme.colors
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredRows = when (selectedFilter) {
        "NEW" -> parseResult.rows.filter { it.status == ImportRowStatus.VALID }
        "DUPES" -> parseResult.rows.filter { it.status == ImportRowStatus.DUPLICATE }
        "INVALID" -> parseResult.rows.filter { it.status == ImportRowStatus.INVALID }
        else -> parseResult.rows
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryBadge(
                label = "Total",
                count = parseResult.totalRows,
                accentColor = colors.textSecondary,
                isSelected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                modifier = Modifier.weight(1f)
            )
            SummaryBadge(
                label = "New",
                count = parseResult.validCount,
                accentColor = if (colors.isDark) ElectricLime else Color(0xFF2E7D32),
                isSelected = selectedFilter == "NEW",
                onClick = { selectedFilter = "NEW" },
                modifier = Modifier.weight(1f)
            )
            SummaryBadge(
                label = "Dupes",
                count = parseResult.duplicateCount,
                accentColor = Color(0xFFFF9800),
                isSelected = selectedFilter == "DUPES",
                onClick = { selectedFilter = "DUPES" },
                modifier = Modifier.weight(1f)
            )
            if (parseResult.invalidCount > 0) {
                SummaryBadge(
                    label = "Skipped",
                    count = parseResult.invalidCount,
                    accentColor = Color(0xFFFF5252),
                    isSelected = selectedFilter == "INVALID",
                    onClick = { selectedFilter = "INVALID" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick action chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${parseResult.selectedCount} selected for import",
                style = SpendlyTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpendlyChip(
                    text = "Select All New",
                    selected = false,
                    onClick = onSelectAllNew
                )
                if (parseResult.duplicateCount > 0) {
                    SpendlyChip(
                        text = "Skip Dupes",
                        selected = false,
                        onClick = onDeselectDuplicates
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrollable Transactions List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredRows, key = { it.rowIndex }) { row ->
                ImportRowItem(
                    row = row,
                    onToggle = { onToggleRow(row.rowIndex) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom CTA
        SpendlyButton(
            text = if (parseResult.selectedCount > 0) "Import ${parseResult.selectedCount} Transactions" else "Select Transactions to Import",
            onClick = onConfirmImport,
            style = SpendlyButtonStyle.PRIMARY_LIME,
            enabled = parseResult.selectedCount > 0,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryBadge(
    label: String,
    count: Int,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else colors.card)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) accentColor else colors.border,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = count.toString(),
                style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) accentColor else colors.textPrimary
            )
            Text(
                text = label,
                style = SpendlyTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun ImportRowItem(
    row: ParsedImportRow,
    onToggle: () -> Unit
) {
    val colors = SpendlyTheme.colors
    val tx = row.transaction

    SpendlyCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = row.status != ImportRowStatus.INVALID, onClick = onToggle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = row.isSelected,
                onCheckedChange = { onToggle() },
                enabled = row.status != ImportRowStatus.INVALID,
                colors = CheckboxDefaults.colors(
                    checkedColor = if (colors.isDark) ElectricLime else Color(0xFF09142A),
                    checkmarkColor = if (colors.isDark) Color(0xFF09142A) else Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (tx != null) {
                    val formattedDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(tx.timestampMs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tx.counterpartyName,
                            style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "${if (tx.isIncoming) "+₹" else "-₹"}${tx.formattedRupees}",
                            style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (tx.isIncoming) if (colors.isDark) ElectricLime else Color(0xFF2E7D32) else colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedDate,
                            style = SpendlyTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = colors.textSecondary
                        )

                        // Status Badge
                        when (row.status) {
                            ImportRowStatus.VALID -> {
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background((if (colors.isDark) ElectricLime else Color(0xFF2E7D32)).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "NEW",
                                        style = SpendlyTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (colors.isDark) ElectricLime else Color(0xFF2E7D32)
                                        )
                                    )
                                }
                            }
                            ImportRowStatus.DUPLICATE -> {
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
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
                            ImportRowStatus.INVALID -> {}
                        }
                    }

                    if (row.reason != null && row.status == ImportRowStatus.DUPLICATE) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = row.reason,
                            style = SpendlyTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = Color(0xFFFF9800)
                        )
                    }
                } else {
                    // Invalid row display
                    Text(
                        text = row.rawLine,
                        style = SpendlyTheme.typography.bodySmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = row.reason ?: "Unparseable line",
                        style = SpendlyTheme.typography.labelSmall.copy(color = Color(0xFFFF5252)),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessImportView(
    summary: com.spendly.app.domain.model.ImportResultSummary,
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
            text = "Import Successful!",
            style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 24.sp),
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Added ${summary.importedCount} transactions from ${summary.fileName} to your database.",
            style = SpendlyTheme.typography.bodyMedium,
            color = colors.textSecondary
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
