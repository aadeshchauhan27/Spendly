package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
}
