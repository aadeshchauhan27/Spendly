package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Transaction

object TransactionDeduplicator {

    private const val FUZZY_TIME_WINDOW_MS = 2 * 60 * 60 * 1000L // 2 hours window

    /**
     * Checks if a new transaction is a duplicate of any existing transactions.
     * Returns the duplicate existing transaction if found, or null otherwise.
     */
    fun findDuplicate(
        candidate: Transaction,
        existingTransactions: List<Transaction>
    ): Transaction? {
        // 1. Exact match on bank reference number (UTR / Txn ID) if present and non-blank
        if (!candidate.bankReferenceNum.isNullOrBlank()) {
            val exactMatch = existingTransactions.find {
                it.bankReferenceNum.equals(candidate.bankReferenceNum, ignoreCase = true)
            }
            if (exactMatch != null) return exactMatch
        }

        // 2. Exact match on raw import identifier if present
        if (!candidate.rawIdentifier.isNullOrBlank()) {
            val identifierMatch = existingTransactions.find {
                it.rawIdentifier.equals(candidate.rawIdentifier, ignoreCase = true)
            }
            if (identifierMatch != null) return identifierMatch
        }

        // 3. Conservative fuzzy match:
        // Same amount in paise + normalized counterparty match + timestamp within 10 minutes
        val candidateMerchant = MerchantNormalizer.normalize(candidate.counterpartyName)

        return existingTransactions.find { existing ->
            val sameAmount = existing.amountPaise == candidate.amountPaise
            val sameDirection = existing.direction == candidate.direction
            val timeDiff = Math.abs(existing.timestampMs - candidate.timestampMs)
            val withinTimeWindow = timeDiff <= FUZZY_TIME_WINDOW_MS
            val existingMerchant = MerchantNormalizer.normalize(existing.counterpartyName)
            val sameMerchant = candidateMerchant.equals(existingMerchant, ignoreCase = true)

            sameAmount && sameDirection && withinTimeWindow && sameMerchant
        }
    }
}
