package com.spendly.app.domain.engine

import com.spendly.app.data.local.entity.DeletedTransactionEntity
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDeduplicatorTest {

    @Test
    fun findDuplicate_matchesExactBankRef() {
        val existing = listOf(
            Transaction(
                id = 1,
                amountPaise = 32000L,
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Swiggy",
                counterpartyName = "Swiggy",
                categoryId = 1,
                timestampMs = 1000000L,
                bankReferenceNum = "512345678901"
            )
        )

        val candidate = Transaction(
            id = 2,
            amountPaise = 32000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Swiggy Food",
            counterpartyName = "Swiggy Order",
            categoryId = 1,
            timestampMs = 1000000L,
            bankReferenceNum = "512345678901" // Exact UTR match
        )

        val duplicate = TransactionDeduplicator.findDuplicate(candidate, existing)
        assertNotNull(duplicate)
        assertEquals(1L, duplicate?.id)
    }

    @Test
    fun findDuplicate_fuzzyMatchesWithin10Minutes() {
        val now = System.currentTimeMillis()
        val existing = listOf(
            Transaction(
                id = 10,
                amountPaise = 45000L, // ₹450
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Uber",
                counterpartyName = "uber@icici",
                categoryId = 3,
                timestampMs = now
            )
        )

        // Same merchant, amount, within 5 minutes (300,000 ms)
        val candidate = Transaction(
            id = 11,
            amountPaise = 45000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Uber Ride",
            counterpartyName = "Uber India",
            categoryId = 3,
            timestampMs = now + 300_000L
        )

        val duplicate = TransactionDeduplicator.findDuplicate(candidate, existing)
        assertNotNull(duplicate)
        assertEquals(10L, duplicate?.id)
    }

    @Test
    fun findDuplicate_rejectsIfOutsideTimeWindow() {
        val now = System.currentTimeMillis()
        val existing = listOf(
            Transaction(
                id = 20,
                amountPaise = 45000L,
                type = TransactionType.EXPENSE,
                direction = TransactionDirection.OUTGOING,
                status = TransactionStatus.COMPLETED,
                title = "Uber",
                counterpartyName = "Uber",
                categoryId = 3,
                timestampMs = now
            )
        )

        // Same merchant, amount, but 2 hours later (7,200,000 ms > 600,000 ms)
        val candidate = Transaction(
            id = 21,
            amountPaise = 45000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Uber",
            counterpartyName = "Uber",
            categoryId = 3,
            timestampMs = now + 7_200_000L
        )

        val duplicate = TransactionDeduplicator.findDuplicate(candidate, existing)
        assertNull(duplicate)
    }

    @Test
    fun isDeletedTransaction_matchesByBankReferenceNum() {
        val deleted = listOf(
            DeletedTransactionEntity(
                id = 1,
                bankReferenceNum = "429812345678",
                amountPaise = 25000L,
                timestampMs = 1000000L,
                counterpartyName = "Zomato"
            )
        )

        val candidate = Transaction(
            id = 0,
            amountPaise = 25000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Zomato Order",
            counterpartyName = "Zomato",
            categoryId = 1,
            timestampMs = 1000000L,
            bankReferenceNum = "429812345678"
        )

        assertTrue(TransactionDeduplicator.isDeletedTransaction(candidate, deleted))
    }

    @Test
    fun isDeletedTransaction_matchesByFuzzyAmountAndCounterparty() {
        val now = System.currentTimeMillis()
        val deleted = listOf(
            DeletedTransactionEntity(
                id = 2,
                amountPaise = 39900L,
                timestampMs = now,
                counterpartyName = "Netflix"
            )
        )

        val candidate = Transaction(
            id = 0,
            amountPaise = 39900L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Netflix",
            counterpartyName = "Netflix Entertainment",
            categoryId = 5,
            timestampMs = now + 120_000L // 2 mins later
        )

        assertTrue(TransactionDeduplicator.isDeletedTransaction(candidate, deleted))

        // Different amount or outside time window should return false
        val differentAmountCandidate = candidate.copy(amountPaise = 49900L)
        assertFalse(TransactionDeduplicator.isDeletedTransaction(differentAmountCandidate, deleted))
    }

    @Test
    fun findDuplicate_matchesOct27NachAndAchDebitPair() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, java.util.Calendar.OCTOBER, 27, 8, 13, 42)
        val debitTime = cal.timeInMillis

        cal.set(2026, java.util.Calendar.OCTOBER, 27, 8, 57, 27)
        val nachTime = cal.timeInMillis

        val achDebit = Transaction(
            id = 26,
            amountPaise = 3301500L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Upi Payment",
            counterpartyName = "Upi Payment",
            categoryId = 1,
            timestampMs = debitTime,
            notes = "A/c ending **3721",
            tag = "Debit INR 33015.00 Axis Bank A/c XX3721 27-10-26 08:13:42 ACH-DR-HDFC BANK LTD-48116 WhatsApp BAL to 917036165000"
        )

        val nachDebit = Transaction(
            id = 25,
            amountPaise = 3301500L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "HDFC Bank",
            counterpartyName = "HDFC Bank",
            categoryId = 1,
            timestampMs = nachTime,
            notes = "A/c ending **3721",
            tag = "NACH debit towards HDFC BANK LTD for INR 33,015.00 with UMRN UTIB0000000009110862 has been successfully processed in A/c no. XX3721 today - Axis Bank"
        )

        val duplicate = TransactionDeduplicator.findDuplicate(nachDebit, listOf(achDebit))
        assertNotNull("Should detect NACH debit and ACH debit as duplicates on the same day", duplicate)
        assertEquals(26L, duplicate?.id)

        // Verify shouldPreferCandidate prefers "HDFC Bank" over generic "Upi Payment"
        assertTrue(TransactionDeduplicator.shouldPreferCandidate(nachDebit, achDebit))
    }
}
