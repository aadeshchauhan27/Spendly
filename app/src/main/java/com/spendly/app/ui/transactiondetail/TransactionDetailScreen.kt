package com.spendly.app.ui.transactiondetail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.spendly.app.core.components.SpendlyButton
import com.spendly.app.core.components.SpendlyButtonStyle
import com.spendly.app.core.components.SpendlyCard
import com.spendly.app.core.components.SpendlyCategoryBadge
import com.spendly.app.core.components.SpendlyChip
import com.spendly.app.core.components.SpendlyDetailTopBar
import com.spendly.app.core.components.SpendlyEmptyState
import com.spendly.app.core.components.getCategoryIconAndColor
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.core.theme.ElectricLime
import com.spendly.app.core.theme.PillShape
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.StatusIncomeGreen
import com.spendly.app.ui.categories.AddCategoryDialog
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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val recategorizePrompt by viewModel.recategorizePrompt.collectAsState()

    var showEditNotesDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var rawSmsContent by remember { mutableStateOf<String?>(null) }

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
                val (icon, defaultColor) = getCategoryIconAndColor(tx.categoryName, tx.categoryIconName)
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
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        SpendlyCategoryBadge(
                            categoryName = tx.categoryName,
                            iconName = tx.categoryIconName,
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

                        val isFailed = tx.status == TransactionStatus.FAILED
                        val prefix = when {
                            isFailed -> ""
                            tx.isIncoming -> "+ "
                            else -> "- "
                        }
                        val amountColor = when {
                            isFailed -> colors.textMuted
                            tx.isIncoming -> StatusIncomeGreen
                            else -> colors.textPrimary
                        }

                        Text(
                            text = if (isFailed) "${tx.formattedRupees} (Failed)" else "$prefix${tx.formattedRupees}",
                            style = SpendlyTheme.typography.displayLarge.copy(
                                fontSize = 36.sp,
                                textDecoration = if (isFailed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            ),
                            color = amountColor
                        )

                        Text(
                            text = dateFormat.format(Date(tx.timestampMs)),
                            style = SpendlyTheme.typography.bodySmall,
                            color = colors.textMuted
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4 Action Buttons: Note, Category, Share, View SMS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ActionPillButton(
                                icon = Icons.AutoMirrored.Filled.NoteAdd,
                                label = if (tx.notes.isNullOrBlank()) "Note" else "Edit",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showEditNotesDialog = true
                                }
                            )
                            ActionPillButton(
                                icon = Icons.Default.Edit,
                                label = "Category",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showCategoryDialog = true
                                }
                            )
                            ActionPillButton(
                                icon = Icons.Default.Share,
                                label = "Share",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Paid ${tx.formattedRupees} to ${tx.title} via Spendly"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Transaction"))
                                }
                            )
                            ActionPillButton(
                                icon = Icons.Default.Message,
                                label = "View SMS",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    coroutineScope.launch {
                                        rawSmsContent = viewModel.getExactSmsBody(context, tx)
                                        showSmsDialog = true
                                    }
                                }
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
                                if (tx.status != com.spendly.app.domain.model.TransactionStatus.COMPLETED) {
                                    DetailInfoRow(
                                        label = "Status",
                                        value = if (tx.status == com.spendly.app.domain.model.TransactionStatus.FAILED) "Failed (Not debited)" else tx.status.name
                                    )
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                                }
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
                            title = { 
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Select Category", 
                                        style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary 
                                    )
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // "+ Create New Category" Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (colors.isDark) ElectricLime.copy(alpha = 0.15f) else Color(0xFFF1F5F9))
                                            .border(1.dp, if (colors.isDark) ElectricLime.copy(alpha = 0.5f) else colors.border, RoundedCornerShape(12.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                showCreateCategoryDialog = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "New Category",
                                                tint = if (colors.isDark) ElectricLime else colors.textPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Create New Category",
                                                style = SpendlyTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (colors.isDark) ElectricLime else colors.textPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    state.categories.forEach { cat ->
                                        val isCurrent = cat.id == tx.categoryId
                                        val (catIcon, defaultCatColor) = getCategoryIconAndColor(cat.name, cat.iconName)
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(cat.colorHex))
                                        } catch (e: Exception) {
                                            defaultCatColor
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isCurrent) (if (colors.isDark) Color(0xFF192A4D) else Color(0xFFE2E8F0)) else Color.Transparent)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.requestCategoryChange(cat)
                                                    showCategoryDialog = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                SpendlyCategoryBadge(
                                                    categoryName = cat.name,
                                                    iconName = cat.iconName,
                                                    icon = catIcon,
                                                    backgroundColor = catColor,
                                                    size = 36.dp,
                                                    iconSize = 18.dp
                                                )
                                                Text(
                                                    text = cat.name,
                                                    style = SpendlyTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = colors.textPrimary
                                                )
                                            }

                                            if (isCurrent) {
                                                Text(
                                                    text = "Current",
                                                    style = SpendlyTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (colors.isDark) ElectricLime else Color(0xFF1E88E5)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                SpendlyButton(
                                    text = "Cancel",
                                    fullWidth = false,
                                    style = SpendlyButtonStyle.OUTLINE,
                                    onClick = { showCategoryDialog = false }
                                )
                            },
                            containerColor = colors.card
                        )
                    }

                    // Create New Category Dialog right from assignment page
                    if (showCreateCategoryDialog) {
                        AddCategoryDialog(
                            onDismissRequest = { showCreateCategoryDialog = false },
                            onConfirm = { name, _, colorHex, iconName ->
                                viewModel.createAndAssignCategory(name, colorHex, iconName)
                                showCreateCategoryDialog = false
                                showCategoryDialog = false
                            }
                        )
                    }

                    // Batch Recategorization Confirmation Prompt
                    recategorizePrompt?.let { prompt ->
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissRecategorizePrompt() },
                            title = {
                                Text(
                                    text = "Update Similar Transactions?",
                                    style = SpendlyTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary
                                )
                            },
                            text = {
                                Text(
                                    text = "We found ${prompt.similarCount} other transaction(s) from \"${prompt.merchantName}\". Would you like to categorize all of them as \"${prompt.newCategoryName}\"?",
                                    style = SpendlyTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            },
                            confirmButton = {
                                SpendlyButton(
                                    text = "Apply to All (${prompt.similarCount + 1})",
                                    fullWidth = false,
                                    style = SpendlyButtonStyle.PRIMARY_LIME,
                                    onClick = { viewModel.confirmRecategorization(applyToAll = true) }
                                )
                            },
                            dismissButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpendlyButton(
                                        text = "Cancel",
                                        fullWidth = false,
                                        style = SpendlyButtonStyle.OUTLINE,
                                        onClick = { viewModel.dismissRecategorizePrompt() }
                                    )
                                    SpendlyButton(
                                        text = "Only This One",
                                        fullWidth = false,
                                        style = SpendlyButtonStyle.OUTLINE,
                                        onClick = { viewModel.confirmRecategorization(applyToAll = false) }
                                    )
                                }
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

                    // View SMS Modal Dialog
                    if (showSmsDialog) {
                        val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("en", "IN"))
                        AlertDialog(
                            onDismissRequest = { showSmsDialog = false },
                            containerColor = colors.card,
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (colors.isDark) ElectricLime.copy(alpha = 0.18f) else Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Message,
                                            contentDescription = null,
                                            tint = if (colors.isDark) ElectricLime else Color(0xFF09142A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Original SMS",
                                            style = SpendlyTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = dateFormat.format(Date(tx.timestampMs)),
                                            style = SpendlyTheme.typography.labelSmall,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.background)
                                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = rawSmsContent ?: "Reading SMS message...",
                                            style = SpendlyTheme.typography.bodyMedium.copy(
                                                fontSize = 13.sp,
                                                lineHeight = 20.sp
                                            ),
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SpendlyButton(
                                        text = "Copy SMS",
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            rawSmsContent?.let { text ->
                                                clipboardManager.setText(AnnotatedString(text))
                                                Toast.makeText(context, "SMS copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        style = if (colors.isDark) SpendlyButtonStyle.PRIMARY_LIME else SpendlyButtonStyle.SECONDARY_DARK
                                    )
                                    SpendlyButton(
                                        text = "Close",
                                        onClick = { showSmsDialog = false },
                                        modifier = Modifier.weight(1f),
                                        style = SpendlyButtonStyle.OUTLINE
                                    )
                                }
                            },
                            dismissButton = {}
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
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(colors.card)
            .border(1.dp, colors.border, PillShape)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
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
