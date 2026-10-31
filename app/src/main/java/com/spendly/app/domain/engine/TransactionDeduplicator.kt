package com.spendly.app.domain.engine

import com.spendly.app.data.local.entity.DeletedTransactionEntity
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionStatus

object TransactionDeduplicator {

    private const val FUZZY_TIME_WINDOW_MS = 15 * 60 * 1000L // 15 minutes window

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
        // Same amount in paise + same direction + normalized counterparty match + timestamp within 15 minutes
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

    /**
     * Checks if a candidate transaction matches any transaction that was
     * explicitly deleted by the user, so it should be permanently skipped.
     */
    fun isDeletedTransaction(
        candidate: Transaction,
        deletedList: List<DeletedTransactionEntity>
    ): Boolean {
        if (deletedList.isEmpty()) return false

        // 1. Bank reference match (UTR / Txn ID)
        if (!candidate.bankReferenceNum.isNullOrBlank()) {
            val utr = candidate.bankReferenceNum.trim().lowercase()
            if (deletedList.any { it.bankReferenceNum?.trim()?.lowercase() == utr }) {
                return true
            }
        }

        // 2. Exact match on raw import identifier
        if (!candidate.rawIdentifier.isNullOrBlank()) {
            val rawId = candidate.rawIdentifier.trim().lowercase()
            if (deletedList.any { it.rawIdentifier?.trim()?.lowercase() == rawId }) {
                return true
            }
        }

        // 3. Exact match on original SMS body / tag
        if (!candidate.tag.isNullOrBlank()) {
            val tag = candidate.tag.trim()
            if (deletedList.any { !it.smsBody.isNullOrBlank() && it.smsBody.trim().equals(tag, ignoreCase = true) }) {
                return true
            }
        }

        // 4. Conservative fuzzy match: same amount + counterparty + timestamp within 15 mins
        val candidateMerchant = MerchantNormalizer.normalize(candidate.counterpartyName)
        return deletedList.any { del ->
            val sameAmount = del.amountPaise == candidate.amountPaise
            val timeDiff = Math.abs(del.timestampMs - candidate.timestampMs)
            val withinTimeWindow = timeDiff <= FUZZY_TIME_WINDOW_MS
            val delMerchant = MerchantNormalizer.normalize(del.counterpartyName)
            val sameMerchant = candidateMerchant.equals(delMerchant, ignoreCase = true)

            sameAmount && withinTimeWindow && sameMerchant
        }
    }

    /**
     * Determines if a candidate transaction offers stronger confirmation of completion
     * than an existing transaction (e.g. Bank Debit Confirmation vs Payment Initiated).
     */
    fun shouldPreferCandidate(candidate: Transaction, existing: Transaction): Boolean {
        // 1. Completed status beats Pending or Failed
        if (candidate.status == TransactionStatus.COMPLETED && existing.status != TransactionStatus.COMPLETED) {
            return true
        }
        // 2. Having Bank UTR beats not having one
        if (!candidate.bankReferenceNum.isNullOrBlank() && existing.bankReferenceNum.isNullOrBlank()) {
            return true
        }
        // 3. Having account/card details beats missing notes
        if (!candidate.notes.isNullOrBlank() && existing.notes.isNullOrBlank()) {
            return true
        }
        return false
    }

    /**
     * High-performance deduplication index for bulk scans (e.g. 365 days / thousands of SMS).
     * Avoids O(N^2) list allocations by indexing bank references, raw IDs, and amounts.
     */
    class Index(
        initialTransactions: List<Transaction> = emptyList(),
        private val deletedList: List<DeletedTransactionEntity> = emptyList()
    ) {
        private val refMap = mutableMapOf<String, Transaction>()
        private val rawIdMap = mutableMapOf<String, Transaction>()
        private val amountMap = mutableMapOf<Long, MutableList<Transaction>>()

        init {
            initialTransactions.forEach { add(it) }
        }

        fun isDeleted(candidate: Transaction): Boolean {
            return isDeletedTransaction(candidate, deletedList)
        }

        fun add(tx: Transaction) {
            if (!tx.bankReferenceNum.isNullOrBlank()) {
                refMap[tx.bankReferenceNum.trim().lowercase()] = tx
            }
            if (!tx.rawIdentifier.isNullOrBlank()) {
                rawIdMap[tx.rawIdentifier.trim().lowercase()] = tx
            }
            amountMap.getOrPut(tx.amountPaise) { mutableListOf() }.add(tx)
        }

        fun findDuplicate(candidate: Transaction): Transaction? {
            // 1. Bank reference match
            if (!candidate.bankReferenceNum.isNullOrBlank()) {
                val match = refMap[candidate.bankReferenceNum.trim().lowercase()]
                if (match != null) return match
            }

            // 2. Raw ID match
            if (!candidate.rawIdentifier.isNullOrBlank()) {
                val match = rawIdMap[candidate.rawIdentifier.trim().lowercase()]
                if (match != null) return match
            }

            // 3. Fast candidate lookup on identical amount only
            val sameAmountList = amountMap[candidate.amountPaise] ?: return null
            val candidateMerchant = MerchantNormalizer.normalize(candidate.counterpartyName)

            return sameAmountList.find { existing ->
                val sameDirection = existing.direction == candidate.direction
                val timeDiff = Math.abs(existing.timestampMs - candidate.timestampMs)
                val withinTimeWindow = timeDiff <= FUZZY_TIME_WINDOW_MS
                val existingMerchant = MerchantNormalizer.normalize(existing.counterpartyName)
                val sameMerchant = candidateMerchant.equals(existingMerchant, ignoreCase = true)

                sameDirection && withinTimeWindow && sameMerchant
            }
        }
    }
}
