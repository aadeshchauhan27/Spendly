package com.spendly.app.ui.transactiondetail

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.SpendlyChip
import com.spendly.app.core.components.SpendlyDetailTopBar
import com.spendly.app.core.components.SpendlyEmptyState
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.StatusIncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendlyTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    var showEditNotesDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is TransactionDetailUiState.Deleted) {
            onBackClick()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding()
    ) {
        SpendlyDetailTopBar(
            title = "Transaction Details",
            onBackClick = onBackClick
        )

        when (val state = uiState) {
            is TransactionDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.textPrimary)
                }
            }
            is TransactionDetailUiState.Deleted -> {
                // Handled in LaunchedEffect
            }
            is TransactionDetailUiState.Error -> {
                SpendlyEmptyState(
                    title = "Error",
                    subtitle = state.message
                )
            }
            is TransactionDetailUiState.Success -> {
                val tx = state.transaction
                val (icon, defaultColor) = getCategoryIconAndColor(tx.categoryName)
                val badgeColor = try {
                    Color(android.graphics.Color.parseColor(tx.categoryColorHex))
                } catch (e: Exception) {
                    defaultColor
                }

                val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("en", "IN"))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        SpendlyCategoryBadge(
                            icon = icon,
                            backgroundColor = badgeColor,
                            size = 64.dp,
                            iconSize = 32.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = tx.title,
                            style = SpendlyTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )

                        Text(
                            text = tx.categoryName,
                            style = SpendlyTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        val prefix = if (tx.isIncoming) "+ " else "- "
                        val amountColor = if (tx.isIncoming) StatusIncomeGreen else colors.textPrimary

                        Text(
                            text = "$prefix${tx.formattedRupees}",
                            style = SpendlyTheme.typography.displayLarge.copy(fontSize = 36.sp),
                            color = amountColor
                        )

                        Text(
                            text = dateFormat.format(Date(tx.timestampMs)),
                            style = SpendlyTheme.typography.bodySmall,
                            color = colors.textMuted
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 3 Action Buttons: Add Note, Edit Category, Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionPillButton(
                                icon = Icons.AutoMirrored.Filled.NoteAdd,
                                label = if (tx.notes.isNullOrBlank()) "Add Note" else "Edit Note",
                                modifier = Modifier.weight(1f),
                                onClick = { showEditNotesDialog = true }
                            )
                            ActionPillButton(
                                icon = Icons.Default.Edit,
                                label = "Category",
                                modifier = Modifier.weight(1f),
                                onClick = { showCategoryDialog = true }
                            )
                            ActionPillButton(
                                icon = Icons.Default.Share,
                                label = "Share",
                                modifier = Modifier.weight(1f),
                                onClick = { /* Share transaction summary */ }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Details Card
                        SpendlyCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                DetailInfoRow(label = "Payment Method", value = tx.paymentMethod)
                                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                                DetailInfoRow(label = "UPI ID", value = tx.counterpartyUpiId ?: "N/A")
                                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                                DetailInfoRow(label = "Transaction ID", value = tx.bankReferenceNum ?: "TXN-${tx.id}")
                                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                                DetailInfoRow(
                                    label = "Category",
                                    value = tx.categoryName,
                                    hasChevron = true,
                                    onClick = { showCategoryDialog = true }
                                )
                                if (!tx.notes.isNullOrBlank()) {
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                                    DetailInfoRow(label = "Notes", value = tx.notes)
                                }
                            }
                        }
                    }

                    // Destructive Action: Delete Transaction
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        SpendlyButton(
                            text = "Delete Transaction",
                            onClick = { showDeleteDialog = true },
                            style = SpendlyButtonStyle.DESTRUCTIVE,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        )
                    }

                    // Edit Notes Dialog
                    if (showEditNotesDialog) {
                        var editedNote by remember { mutableStateOf(tx.notes ?: "") }
                        AlertDialog(
                            onDismissRequest = { showEditNotesDialog = false },
                            title = { Text("Edit Note", color = colors.textPrimary) },
                            text = {
                                OutlinedTextField(
                                    value = editedNote,
                                    onValueChange = { editedNote = it },
                                    label = { Text("Note") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                SpendlyButton(
                                    text = "Save",
                                    fullWidth = false,
                                    onClick = {
                                        viewModel.updateNotes(editedNote)
                                        showEditNotesDialog = false
                                    }
                                )
                            },
                            dismissButton = {
                                SpendlyButton(
                                    text = "Cancel",
                                    fullWidth = false,
                                    style = SpendlyButtonStyle.OUTLINE,
                                    onClick = { showEditNotesDialog = false }
                                )
                            },
                            containerColor = colors.card
                        )
                    }

                    // Change Category Dialog
                    if (showCategoryDialog) {
                        AlertDialog(
                            onDismissRequest = { showCategoryDialog = false },
                            title = { Text("Select Category", color = colors.textPrimary) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.categories.forEach { cat ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(PillShape)
                                                .background(if (cat.id == tx.categoryId) colors.border else Color.Transparent)
                                                .clickable {
                                                    viewModel.updateCategory(cat.id)
                                                    showCategoryDialog = false
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = cat.name,
                                                style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                SpendlyButton(
                                    text = "Close",
                                    fullWidth = false,
                                    style = SpendlyButtonStyle.OUTLINE,
                                    onClick = { showCategoryDialog = false }
                                )
                            },
                            containerColor = colors.card
                        )
                    }

                    // Delete Confirmation Dialog
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Transaction?", color = colors.textPrimary) },
                            text = {
                                Text(
                                    "Are you sure you want to delete this transaction of ${tx.formattedRupees}? This will immediately update your dashboard spending and category budgets.",
                                    color = colors.textSecondary
                                )
                            },
                            confirmButton = {
                                SpendlyButton(
                                    text = "Delete",
                                    fullWidth = false,
                                    style = SpendlyButtonStyle.DESTRUCTIVE,
                                    onClick = {
                                        viewModel.deleteTransaction()
                                        showDeleteDialog = false
                                    }
                                )
                            },
                            dismissButton = {
                                SpendlyButton(
                                    text = "Cancel",
                                    fullWidth = false,
                                    style = SpendlyButtonStyle.OUTLINE,
                                    onClick = { showDeleteDialog = false }
                                )
                            },
                            containerColor = colors.card
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colors = SpendlyTheme.colors
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(colors.card)
            .border(1.dp, colors.border, PillShape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = SpendlyTheme.typography.labelSmall,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    hasChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = SpendlyTheme.colors
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = SpendlyTheme.typography.bodyMedium,
            color = colors.textSecondary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary
            )
            if (hasChevron) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
