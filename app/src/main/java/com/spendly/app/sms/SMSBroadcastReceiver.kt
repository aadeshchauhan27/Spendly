package com.spendly.app.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.spendly.app.MainActivity
import com.spendly.app.R
import com.spendly.app.data.local.SpendlyDatabase
import com.spendly.app.data.mapper.toDomain
import com.spendly.app.data.mapper.toEntity
import com.spendly.app.domain.engine.SMSTransactionParser
import com.spendly.app.domain.engine.TransactionDeduplicator
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SMSBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSBroadcastReceiver"
        private const val CHANNEL_ID = "spendly_transactions"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = SpendlyDatabase.getInstance(context)
                val categoryDao = database.categoryDao()
                val transactionDao = database.transactionDao()
                val deletedDao = database.deletedTransactionDao()

                val categoryEntities = categoryDao.getAllCategoriesFlow().first()
                val availableCategories = categoryEntities.map { it.toDomain() }
                val existingTransactionEntities = transactionDao.getAllTransactionsFlow().first()
                val existingTransactions = existingTransactionEntities.map { it.toDomain() }
                val deletedTransactions = deletedDao.getAllDeleted()

                for (sms in messages) {
                    val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: ""
                    val content = sms.displayMessageBody ?: sms.messageBody ?: ""
                    val timestamp = sms.timestampMillis

                    val transaction = SMSTransactionParser.parseSms(
                        sender = sender,
                        content = content,
                        messageTimestampMs = timestamp,
                        availableCategories = availableCategories,
                        existingTransactions = existingTransactions
                    )

                    if (transaction != null) {
                        if (TransactionDeduplicator.isDeletedTransaction(transaction, deletedTransactions)) {
                            Log.d(TAG, "Transaction from $sender was previously deleted by user, permanently skipping.")
                            continue
                        }

                        val duplicate = TransactionDeduplicator.findDuplicate(transaction, existingTransactions)

                        if (duplicate != null) {
                            if (TransactionDeduplicator.shouldPreferCandidate(transaction, duplicate)) {
                                val updated = duplicate.copy(
                                    status = transaction.status,
                                    bankReferenceNum = transaction.bankReferenceNum ?: duplicate.bankReferenceNum,
                                    notes = transaction.notes ?: duplicate.notes,
                                    updatedAtMs = System.currentTimeMillis()
                                ).toEntity()
                                transactionDao.update(updated)
                                Log.d(TAG, "Updated transaction ${duplicate.id} with completed confirmation from $sender")
                            } else {
                                Log.d(TAG, "Duplicate transaction from $sender, skipping.")
                            }
                            continue
                        }

                        // If this is a reversal linked to an original transaction, update original transaction status
                        if (transaction.type == TransactionType.REVERSAL && transaction.relatedTransactionId != null) {
                            val orig = transactionDao.getTransactionById(transaction.relatedTransactionId)
                            if (orig != null) {
                                transactionDao.update(orig.copy(status = TransactionStatus.REVERSED.name, updatedAtMs = System.currentTimeMillis()))
                                Log.d(TAG, "Marked original transaction ${orig.id} as REVERSED")
                            }
                        }

                        val entity = transaction.copy(
                            source = TransactionSource.SMS_IMPORT,
                            createdAtMs = System.currentTimeMillis(),
                            updatedAtMs = System.currentTimeMillis()
                        ).toEntity()

                        val newId = transactionDao.insert(entity)
                        Log.d(TAG, "Auto-saved transaction ID $newId from $sender: ${transaction.formattedRupees}")

                        // Post local notification to alert user
                        showTransactionNotification(context, transaction)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showTransactionNotification(context: Context, tx: Transaction) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Transaction Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts for newly detected UPI & Bank SMS transactions"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (tx.isIncoming) {
                "₹${tx.formattedRupees} Received"
            } else {
                "₹${tx.formattedRupees} Spent at ${tx.counterpartyName}"
            }

            val text = "${tx.paymentApp ?: "UPI"} • ${tx.categoryName} • Added to Spendly"

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(tx.id.toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }
}
