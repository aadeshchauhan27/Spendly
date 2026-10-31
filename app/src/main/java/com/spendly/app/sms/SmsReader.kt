package com.spendly.app.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.spendly.app.data.local.entity.DeletedTransactionEntity
import com.spendly.app.domain.engine.SMSTransactionParser
import com.spendly.app.domain.engine.TransactionDeduplicator
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetectedSmsItem(
    val smsId: Long,
    val sender: String,
    val body: String,
    val dateMs: Long,
    val transaction: Transaction?,
    val isDuplicate: Boolean,
    val matchedDuplicate: Transaction? = null,
    val isSelected: Boolean = (transaction != null && !isDuplicate)
)

data class SmsScanResult(
    val totalScanned: Int,
    val totalTransactionsFound: Int,
    val validNewCount: Int,
    val duplicateCount: Int,
    val totalSpentPaise: Long,
    val totalIncomePaise: Long,
    val items: List<DetectedSmsItem>
)

object SmsReader {

    private const val TAG = "SmsReader"

    /**
     * Scans SMS inbox and parses financial transactions.
     * Evaluates duplicates against existing DB transactions.
     */
    suspend fun scanInbox(
        context: Context,
        existingTransactions: List<Transaction>,
        categories: List<Category>,
        deletedTransactions: List<DeletedTransactionEntity> = emptyList(),
        maxDays: Int = 365
    ): SmsScanResult = withContext(Dispatchers.IO) {
        val items = mutableListOf<DetectedSmsItem>()
        var totalScanned = 0
        var totalFound = 0
        var validNewCount = 0
        var duplicateCount = 0
        var totalSpentPaise = 0L
        var totalIncomePaise = 0L

        val dedupIndex = TransactionDeduplicator.Index(existingTransactions, deletedTransactions)

        val cutoffTimeMs = System.currentTimeMillis() - (maxDays * 24 * 60 * 60 * 1000L)

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoffTimeMs.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressCol = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyCol = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    totalScanned++
                    val id = c.getLong(idCol)
                    val sender = c.getString(addressCol) ?: ""
                    val body = c.getString(bodyCol) ?: ""
                    val dateMs = c.getLong(dateCol)

                    val transaction = SMSTransactionParser.parseSms(
                        sender = sender,
                        content = body,
                        messageTimestampMs = dateMs,
                        availableCategories = categories,
                        existingTransactions = existingTransactions
                    )

                    if (transaction != null) {
                        if (dedupIndex.isDeleted(transaction)) {
                            // User explicitly deleted this transaction previously. Permanently skip!
                            continue
                        }

                        totalFound++
                        val duplicate = dedupIndex.findDuplicate(transaction)

                        if (duplicate != null) {
                            duplicateCount++
                            items.add(
                                DetectedSmsItem(
                                    smsId = id,
                                    sender = sender,
                                    body = body,
                                    dateMs = dateMs,
                                    transaction = transaction,
                                    isDuplicate = true,
                                    matchedDuplicate = duplicate,
                                    isSelected = false
                                )
                            )
                        } else {
                            dedupIndex.add(transaction)
                            val isSpam = transaction.type == TransactionType.SPAM
                            if (!isSpam) {
                                validNewCount++
                                if (transaction.isIncoming) {
                                    totalIncomePaise += transaction.amountPaise
                                } else {
                                    totalSpentPaise += transaction.amountPaise
                                }
                            }
                            items.add(
                                DetectedSmsItem(
                                    smsId = id,
                                    sender = sender,
                                    body = body,
                                    dateMs = dateMs,
                                    transaction = transaction,
                                    isDuplicate = false,
                                    isSelected = !isSpam
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission READ_SMS not granted", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning SMS inbox", e)
        }

        SmsScanResult(
            totalScanned = totalScanned,
            totalTransactionsFound = totalFound,
            validNewCount = validNewCount,
            duplicateCount = duplicateCount,
            totalSpentPaise = totalSpentPaise,
            totalIncomePaise = totalIncomePaise,
            items = items
        )
    }
}
