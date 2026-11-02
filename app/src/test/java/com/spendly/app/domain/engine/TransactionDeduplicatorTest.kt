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

    @Test
    fun findDuplicate_preservesSeparateIncomingCreditsWithDifferentUtrAndVpa() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, java.util.Calendar.NOVEMBER, 1, 11, 13, 19)
        val morningTime = cal.timeInMillis

        cal.set(2026, java.util.Calendar.NOVEMBER, 1, 14, 28, 33)
        val afternoonTime = cal.timeInMillis

        val txMorning = Transaction(
            id = 7,
            amountPaise = 206000L,
            type = TransactionType.INCOME,
            direction = TransactionDirection.INCOMING,
            status = TransactionStatus.COMPLETED,
            title = "garg@ybl",
            counterpartyName = "garg@ybl",
            counterpartyUpiId = "garg@ybl",
            categoryId = 7,
            timestampMs = morningTime,
            bankReferenceNum = "301975700816",
            notes = "A/c ending **7862",
            tag = "Credit Alert! Rs.2060.00 credited to HDFC Bank A/c XX7862 on 01-11-26 from VPA *garg@ybl (UPI 301975700816)"
        )

        val txAfternoon = Transaction(
            id = 4,
            amountPaise = 206000L,
            type = TransactionType.INCOME,
            direction = TransactionDirection.INCOMING,
            status = TransactionStatus.COMPLETED,
            title = "6912@ybl",
            counterpartyName = "6912@ybl",
            counterpartyUpiId = "6912@ybl",
            categoryId = 7,
            timestampMs = afternoonTime,
            bankReferenceNum = "393096780809",
            notes = "A/c ending **7862",
            tag = "Credit Alert! Rs.2060.00 credited to HDFC Bank A/c XX7862 on 01-11-26 from VPA *6912@ybl (UPI 393096780809)"
        )

        // Must NOT be detected as duplicates despite identical amount and account!
        assertNull(
            "Distinct incoming credits with different UTRs and VPAs must never be merged",
            TransactionDeduplicator.findDuplicate(txAfternoon, listOf(txMorning))
        )
        assertNull(
            "Distinct incoming credits with different UTRs and VPAs must never be merged (reverse)",
            TransactionDeduplicator.findDuplicate(txMorning, listOf(txAfternoon))
        )
    }

    @Test
    fun findDuplicate_rejectsDifferentUtrsEvenIfSameMerchantAndAccount() {
        val now = System.currentTimeMillis()
        val tx1 = Transaction(
            id = 101,
            amountPaise = 50000L,
            type = TransactionType.EXPENSE,
            direction = TransactionDirection.OUTGOING,
            status = TransactionStatus.COMPLETED,
            title = "Swiggy",
            counterpartyName = "Swiggy",
            categoryId = 1,
            timestampMs = now,
            bankReferenceNum = "111122223333",
            notes = "A/c ending **1234"
        )
        val tx2 = tx1.copy(
            id = 102,
            bankReferenceNum = "999988887777", // Different UTR
            timestampMs = now + 60_000L
        )

        assertNull(
            "Different UTRs must definitively mean two distinct transactions",
            TransactionDeduplicator.findDuplicate(tx2, listOf(tx1))
        )
    }

    @Test
    fun findDuplicate_matchesIdenticalSmsTagOnSameDayAndPrefersRealUtr() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, java.util.Calendar.OCTOBER, 28, 1, 34, 27)
        val ts = cal.timeInMillis

        val smsText = "INR 318815.00 credited to Axis Bank A/c XX3721 on 28-10-26 at 01:32:37 IST. Ref: NEFT/CHASH00063843585/FQ I.View bal: https://ccm.axis.bank.in/AXISBK/m49XJ3KM"

        // Existing transaction inserted without UTR
        val existing = Transaction(
            id = 23,
            amountPaise = 31881500L,
            type = TransactionType.INCOME,
            direction = TransactionDirection.INCOMING,
            status = TransactionStatus.COMPLETED,
            title = "Money Received",
            counterpartyName = "Money Received",
            categoryId = 7,
            timestampMs = ts,
            bankReferenceNum = null,
            notes = "A/c ending **3721",
            tag = smsText
        )

        // Candidate re-parsed with extracted NEFT UTR
        val candidate = Transaction(
            id = 2830,
            amountPaise = 31881500L,
            type = TransactionType.INCOME,
            direction = TransactionDirection.INCOMING,
            status = TransactionStatus.COMPLETED,
            title = "FQ I",
            counterpartyName = "FQ I",
            categoryId = 7,
            timestampMs = ts,
            bankReferenceNum = "CHASH00063843585",
            notes = "A/c ending **3721",
            tag = smsText
        )

        val duplicate = TransactionDeduplicator.findDuplicate(candidate, listOf(existing))
        assertNotNull("Identical SMS on same day must be detected as duplicate", duplicate)
        assertEquals(23L, duplicate?.id)

        // Must prefer candidate with real UTR and specific merchant over old generic row
        assertTrue(TransactionDeduplicator.shouldPreferCandidate(candidate, existing))
    }
}
