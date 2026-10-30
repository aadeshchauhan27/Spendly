package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType

data class ParsedSmsResult(
    val transaction: Transaction,
    val sender: String,
    val rawBody: String,
    val accountEnding: String? = null,
    val availableBalancePaise: Long? = null
)

object SMSTransactionParser {

    /**
     * Checks if an SMS message is a promotional spam message, loan offer,
     * consent disbursement request, or non-transaction notice.
     */
    fun isSpamOrOffer(content: String): Boolean {
        return SMSClassifier.isPromotionalOrLoanOffer(content.lowercase())
    }

    /**
     * Parses an incoming or inbox SMS message.
     * Returns null if the SMS does not contain a financial transaction.
     */
    fun parseSms(
        sender: String,
        content: String,
        messageTimestampMs: Long = System.currentTimeMillis(),
        availableCategories: List<Category> = emptyList(),
        existingTransactions: List<Transaction> = emptyList(),
        includeSpam: Boolean = false
    ): Transaction? {
        val parsedResult = parseSmsDetailed(
            sender = sender,
            content = content,
            messageTimestampMs = messageTimestampMs,
            availableCategories = availableCategories,
            existingTransactions = existingTransactions
        )
        val tx = parsedResult?.transaction ?: return null
        if (tx.type == TransactionType.SPAM && !includeSpam) {
            return null
        }
        return tx
    }

    /**
     * Detailed SMS parser returning metadata like account ending and raw body.
     */
    fun parseSmsDetailed(
        sender: String,
        content: String,
        messageTimestampMs: Long = System.currentTimeMillis(),
        availableCategories: List<Category> = emptyList(),
        existingTransactions: List<Transaction> = emptyList()
    ): ParsedSmsResult? {
        val result = SMSClassifier.classify(
            sender = sender,
            rawContent = content,
            messageTimestampMs = messageTimestampMs,
            availableCategories = availableCategories,
            existingTransactions = existingTransactions
        )

        if (!result.isFinancialTransaction || result.transaction == null) {
            return null
        }

        return ParsedSmsResult(
            transaction = result.transaction,
            sender = sender.trim(),
            rawBody = content.trim(),
            accountEnding = result.accountEnding,
            availableBalancePaise = result.availableBalancePaise
        )
    }

    fun parseAmountStringToPaise(raw: String): Long? {
        return SMSClassifier.parseAmountStringToPaise(raw)
    }
}
