package com.spendly.app.ui.transactions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyChip
import com.spendly.app.core.components.SpendlyDateRangeDialog
import com.spendly.app.core.components.SpendlyEmptyState
import com.spendly.app.core.components.SpendlyTransactionRow
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.theme.DarkNavyBackground
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.ui.import_export.ImportPreviewBottomSheet
import com.spendly.app.ui.import_export.ImportViewModel
import com.spendly.app.ui.sms.SmsSyncBottomSheet
import com.spendly.app.ui.sms.SmsSyncViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    importViewModel: ImportViewModel,
    smsSyncViewModel: SmsSyncViewModel,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showSmsSheet by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row with Title and Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = SpendlyTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                    color = colors.textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // SMS Sync Button
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.card)
                            .border(1.dp, if (colors.isDark) ElectricLime else colors.border, PillShape)
                            .clickable { showSmsSheet = true }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "SMS Sync",
                                tint = if (colors.isDark) ElectricLime else colors.textPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "SMS",
                                style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (colors.isDark) ElectricLime else colors.textPrimary
                            )
                        }
                    }

                    // Import Button
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.card)
                            .border(1.dp, colors.border, PillShape)
                            .clickable { showImportSheet = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Import",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Import",
                                style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                    }

                    // Add Button
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (colors.isDark) ElectricLime else Color(0xFF09142A))
                            .clickable { showAddDialog = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = if (colors.isDark) DarkNavyBackground else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Add",
                                style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (colors.isDark) DarkNavyBackground else Color.White
                            )
                        }
                    }
                }
            }

            when (val state = uiState) {
                is TransactionsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = if (colors.isDark) ElectricLime else colors.textPrimary)
                    }
                }
                is TransactionsUiState.Error -> {
                    SpendlyEmptyState(
                        title = "Unable to load transactions",
                        subtitle = state.message
                    )
                }
                is TransactionsUiState.Success -> {
                    // Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(PillShape)
                            .background(colors.card)
                            .border(1.dp, colors.border, PillShape)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                textStyle = SpendlyTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                                cursorBrush = SolidColor(colors.textPrimary),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search transactions...",
                                            style = SpendlyTheme.typography.bodyMedium,
                                            color = colors.textMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Range & Category Filter Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isRangeActive = state.dateRange != null
                        val rangeLabel = if (isRangeActive) "📅 ${state.dateRange!!.label}" else "📅 Date Range"

                        SpendlyChip(
                            text = rangeLabel,
                            selected = isRangeActive,
                            onClick = { showDateRangePicker = true }
                        )

                        if (isRangeActive) {
                            Text(
                                text = "Clear ✕",
                                style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (colors.isDark) ElectricLime else Color(0xFFDC2626),
                                modifier = Modifier
                                    .clip(PillShape)
                                    .clickable { viewModel.clearDateRange() }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Chips
                    val categoryNames = listOf("All") + state.availableCategories.map { it.name }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)
                    ) {
                        items(categoryNames) { cat ->
                            SpendlyChip(
                                text = cat,
                                selected = state.selectedCategory == cat,
                                onClick = { viewModel.onCategorySelect(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.groupedTransactions.isEmpty()) {
                        SpendlyEmptyState(
                            title = if (state.searchQuery.isNotBlank()) "No matching transactions" else "No transactions yet",
                            subtitle = if (state.searchQuery.isNotBlank()) "Try searching with a different merchant or note" else "Add your first transaction using the + button."
                        )
                    } else {
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale("en", "IN"))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        ) {
                            state.groupedTransactions.forEach { group ->
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = group.dateLabel,
                                            style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )

                                        Text(
                                            text = group.totalAmountFormatted,
                                            style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                items(group.transactions) { tx ->
                                    val (icon, defaultColor) = getCategoryIconAndColor(tx.categoryName)
                                    val catColor = try {
                                        Color(android.graphics.Color.parseColor(tx.categoryColorHex))
                                    } catch (e: Exception) {
                                        defaultColor
                                    }

                                    SpendlyTransactionRow(
                                        title = tx.title,
                                        categoryName = tx.categoryName,
                                        amountText = tx.formattedRupees.replace("₹", ""),
                                        timeText = timeFormat.format(Date(tx.timestampMs)),
                                        isIncoming = tx.isIncoming,
                                        icon = icon,
                                        iconBackground = catColor,
                                        onClick = { onTransactionClick(tx.id) }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }

                    // Add Transaction Dialog
                    if (showAddDialog) {
                        AddTransactionDialog(
                            categories = state.availableCategories,
                            onDismiss = { showAddDialog = false },
                            onSave = { title, amountRupees, categoryId, isExpense, notes ->
                                viewModel.addManualTransaction(title, amountRupees, categoryId, isExpense, notes)
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ElectricLime,
            contentColor = DarkNavyBackground,
            shape = PillShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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

        // Date Range Picker Dialog
        if (showDateRangePicker) {
            val stateSuccess = uiState as? TransactionsUiState.Success
            SpendlyDateRangeDialog(
                initialStartDateMs = stateSuccess?.dateRange?.startMs,
                initialEndDateMs = stateSuccess?.dateRange?.endMs,
                onDismissRequest = { showDateRangePicker = false },
                onDateRangeSelected = { startMs, endMs ->
                    viewModel.setDateRange(startMs, endMs)
                }
            )
        }
    }
}
