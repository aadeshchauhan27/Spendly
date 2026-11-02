package com.spendly.app.domain.engine

import com.spendly.app.data.local.entity.DeletedTransactionEntity
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionStatus
import java.util.Calendar
import java.util.Locale

object TransactionDeduplicator {

    private const val FUZZY_TIME_WINDOW_MS = 30 * 60 * 1000L // 30 minutes window for general same-merchant
    private const val SAME_ACCOUNT_WINDOW_MS = 4 * 60 * 60 * 1000L // 4 hours window for same account

    private val GENERIC_MERCHANT_NAMES = setOf(
        "upi payment", "money sent", "money received", "bank transfer",
        "payment", "sent", "debited", "transferred", "bill payment system", "the same", ""
    )

    fun isGenericMerchant(name: String?): Boolean {
        if (name.isNullOrBlank()) return true
        return name.trim().lowercase(Locale.ENGLISH) in GENERIC_MERCHANT_NAMES
    }

    fun hasMandateKeywords(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase(Locale.ENGLISH)
        return lower.contains("nach") || lower.contains("ach") ||
                lower.contains("mandate") || lower.contains("standing instruction") ||
                lower.contains("autopay") || lower.contains("auto-pay") ||
                lower.contains("si set") || lower.contains("umrn") || lower.contains("emidr")
    }

    fun isSameCalendarDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isMidnight(timestampMs: Long): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = timestampMs }
        return c.get(Calendar.HOUR_OF_DAY) == 0 &&
               c.get(Calendar.MINUTE) == 0 &&
               c.get(Calendar.SECOND) == 0
    }

    /**
     * Checks if two transactions are duplicates based on multi-tiered validation rules:
     * 1. Exact Bank Reference (UTR)
     * 2. Exact Raw Import Identifier
     * 3. Same Amount + Direction + Same Calendar Day:
     *    - Both are NACH/ACH/Mandate/AutoPay (same account or within 12 hours)
     *    - Same Bank Account + same merchant or one is generic (within 4 hours or mandate)
     *    - Same Bank Account rapid duplicate (within 30 mins)
     *    - Same Normalized Merchant (within 30 mins)
     *    - Midnight timestamp reconciliation
     */
    fun isDuplicatePair(candidate: Transaction, existing: Transaction): Boolean {
        // 1. Exact match on bank reference number (UTR / Txn ID) if present and non-blank in both
        if (!candidate.bankReferenceNum.isNullOrBlank() && !existing.bankReferenceNum.isNullOrBlank()) {
            if (candidate.bankReferenceNum.equals(existing.bankReferenceNum, ignoreCase = true)) {
                return true
            }
        }

        // 2. Exact match on raw import identifier if present
        if (!candidate.rawIdentifier.isNullOrBlank() && !existing.rawIdentifier.isNullOrBlank()) {
            if (candidate.rawIdentifier.equals(existing.rawIdentifier, ignoreCase = true)) {
                return true
            }
        }

        // 3. Must match amount in paise and direction
        if (existing.amountPaise != candidate.amountPaise || existing.direction != candidate.direction) {
            return false
        }

        // 4. Must be on the same calendar day
        val sameDay = isSameCalendarDay(existing.timestampMs, candidate.timestampMs)
        if (!sameDay) return false

        val timeDiff = Math.abs(existing.timestampMs - candidate.timestampMs)

        // Tier A: NACH / ACH / Mandate deduplication on the same day
        val candidateMandate = hasMandateKeywords(candidate.tag) || hasMandateKeywords(candidate.notes) || hasMandateKeywords(candidate.title)
        val existingMandate = hasMandateKeywords(existing.tag) || hasMandateKeywords(existing.notes) || hasMandateKeywords(existing.title)
        val sameAccount = !candidate.notes.isNullOrBlank() && candidate.notes == existing.notes

        if (candidateMandate && existingMandate) {
            if (sameAccount || timeDiff <= 12 * 60 * 60 * 1000L) {
                return true
            }
        }

        // Tier B: Same Bank Account on the same day
        if (sameAccount) {
            val candidateMerchant = MerchantNormalizer.normalize(candidate.counterpartyName)
            val existingMerchant = MerchantNormalizer.normalize(existing.counterpartyName)

            // Matching merchant on same account within 4 hours (or if either is a recurring mandate)
            if (candidateMerchant.equals(existingMerchant, ignoreCase = true)) {
                if (timeDiff <= SAME_ACCOUNT_WINDOW_MS || candidateMandate || existingMandate) {
                    return true
                }
            }

            // One generic merchant ("UPI Payment", "Money Sent") and one specific ("HDFC Bank")
            if (isGenericMerchant(candidateMerchant) || isGenericMerchant(existingMerchant)) {
                if (timeDiff <= SAME_ACCOUNT_WINDOW_MS || candidateMandate || existingMandate) {
                    return true
                }
            }

            // Rapid duplicate on same account within 30 minutes
            if (timeDiff <= FUZZY_TIME_WINDOW_MS) {
                return true
            }
        }

        // Tier C: Same merchant without account details within 30 minutes
        val candidateMerchant = MerchantNormalizer.normalize(candidate.counterpartyName)
        val existingMerchant = MerchantNormalizer.normalize(existing.counterpartyName)
        if (!isGenericMerchant(candidateMerchant) && candidateMerchant.equals(existingMerchant, ignoreCase = true)) {
            if (timeDiff <= FUZZY_TIME_WINDOW_MS) {
                return true
            }
        }

        // Tier D: Midnight reconciliation (e.g. statement timestamp 00:00:00 vs real SMS time)
        if (isMidnight(existing.timestampMs) || isMidnight(candidate.timestampMs)) {
            if (candidateMerchant.equals(existingMerchant, ignoreCase = true) ||
                isGenericMerchant(candidateMerchant) || isGenericMerchant(existingMerchant)
            ) {
                return true
            }
        }

        return false
    }

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
                !it.bankReferenceNum.isNullOrBlank() && it.bankReferenceNum.equals(candidate.bankReferenceNum, ignoreCase = true)
            }
            if (exactMatch != null) return exactMatch
        }

        // 2. Exact match on raw import identifier if present
        if (!candidate.rawIdentifier.isNullOrBlank()) {
            val identifierMatch = existingTransactions.find {
                !it.rawIdentifier.isNullOrBlank() && it.rawIdentifier.equals(candidate.rawIdentifier, ignoreCase = true)
            }
            if (identifierMatch != null) return identifierMatch
        }

        // 3. Multi-tiered match on candidate
        return existingTransactions.find { existing ->
            isDuplicatePair(candidate, existing)
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
            val utr = candidate.bankReferenceNum.trim().lowercase(Locale.ENGLISH)
            if (deletedList.any { it.bankReferenceNum?.trim()?.lowercase(Locale.ENGLISH) == utr }) {
                return true
            }
        }

        // 2. Exact match on raw import identifier
        if (!candidate.rawIdentifier.isNullOrBlank()) {
            val rawId = candidate.rawIdentifier.trim().lowercase(Locale.ENGLISH)
            if (deletedList.any { it.rawIdentifier?.trim()?.lowercase(Locale.ENGLISH) == rawId }) {
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

        // 4. Conservative fuzzy match: same amount + counterparty + timestamp within 30 mins
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
     * or richer merchant metadata than an existing transaction (e.g. "HDFC Bank" vs "UPI Payment").
     */
    fun shouldPreferCandidate(candidate: Transaction, existing: Transaction): Boolean {
        // 1. Completed status beats Pending or Failed
        if (candidate.status == TransactionStatus.COMPLETED && existing.status != TransactionStatus.COMPLETED) {
            return true
        }
        if (candidate.status != TransactionStatus.COMPLETED && existing.status == TransactionStatus.COMPLETED) {
            return false
        }
        // 2. Specific merchant ALWAYS beats generic merchant ("HDFC Bank" beats "UPI Payment")
        val candidateGeneric = isGenericMerchant(MerchantNormalizer.normalize(candidate.counterpartyName))
        val existingGeneric = isGenericMerchant(MerchantNormalizer.normalize(existing.counterpartyName))
        if (!candidateGeneric && existingGeneric) {
            return true
        }
        if (candidateGeneric && !existingGeneric) {
            return false
        }
        // 3. Having real Bank UTR beats not having one (ignoring false 12-digit phone numbers)
        val candidateHasRealUtr = !candidate.bankReferenceNum.isNullOrBlank() &&
                !(candidate.bankReferenceNum.startsWith("91") && candidate.bankReferenceNum.length == 12)
        val existingHasRealUtr = !existing.bankReferenceNum.isNullOrBlank() &&
                !(existing.bankReferenceNum.startsWith("91") && existing.bankReferenceNum.length == 12)

        if (candidateHasRealUtr && !existingHasRealUtr) {
            return true
        }
        if (!candidateHasRealUtr && existingHasRealUtr) {
            return false
        }
        // 4. Having account/card details beats missing notes
        if (!candidate.notes.isNullOrBlank() && existing.notes.isNullOrBlank()) {
            return true
        }
        // 5. Real timestamp beats artificial 00:00:00 midnight timestamp
        if (!isMidnight(candidate.timestampMs) && isMidnight(existing.timestampMs)) {
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
                refMap[tx.bankReferenceNum.trim().lowercase(Locale.ENGLISH)] = tx
            }
            if (!tx.rawIdentifier.isNullOrBlank()) {
                rawIdMap[tx.rawIdentifier.trim().lowercase(Locale.ENGLISH)] = tx
            }
            amountMap.getOrPut(tx.amountPaise) { mutableListOf() }.add(tx)
        }

        fun findDuplicate(candidate: Transaction): Transaction? {
            // 1. Bank reference match
            if (!candidate.bankReferenceNum.isNullOrBlank()) {
                val match = refMap[candidate.bankReferenceNum.trim().lowercase(Locale.ENGLISH)]
                if (match != null) return match
            }

            // 2. Raw ID match
            if (!candidate.rawIdentifier.isNullOrBlank()) {
                val match = rawIdMap[candidate.rawIdentifier.trim().lowercase(Locale.ENGLISH)]
                if (match != null) return match
            }

            // 3. Fast candidate lookup on identical amount only
            val sameAmountList = amountMap[candidate.amountPaise] ?: return null
            return sameAmountList.find { existing ->
                isDuplicatePair(candidate, existing)
            }
        }
    }
}
