package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.ImportParseResult
import com.spendly.app.domain.model.ImportRowStatus
import com.spendly.app.domain.model.ParsedImportRow
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object CsvTransactionParser {

    private val DATE_FORMATS = listOf(
        "dd/MM/yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm",
        "dd/MM/yyyy hh:mm:ss a",
        "dd/MM/yyyy hh:mm a",
        "dd/MM/yyyy",
        "dd-MM-yyyy HH:mm:ss",
        "dd-MM-yyyy HH:mm",
        "dd-MM-yyyy hh:mm:ss a",
        "dd-MM-yyyy hh:mm a",
        "dd-MM-yyyy",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd",
        "dd-MMM-yyyy HH:mm:ss",
        "dd-MMM-yyyy HH:mm",
        "dd-MMM-yyyy",
        "dd MMM yyyy HH:mm:ss",
        "dd MMM yyyy",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd",
        "MM/dd/yyyy HH:mm:ss",
        "MM/dd/yyyy"
    )

    private val TIME_FORMATS = listOf(
        "HH:mm:ss",
        "HH:mm",
        "hh:mm:ss a",
        "hh:mm a"
    )

    private val UTR_REGEX = Pattern.compile("(?i)(?:UPI(?:/CR|/DR)?/|UTR[:/\\s-]*|REF[:/\\s-]*|RRN[:/\\s-]*)([A-Za-z0-9]{10,22})")
    private val STANDALONE_12_DIGIT_REGEX = Pattern.compile("\\b(\\d{12})\\b")

    /**
     * Parses a CSV content string into a structured ImportParseResult.
     * Evaluates duplicates against existingTransactions.
     */
    fun parse(
        csvContent: String,
        fileName: String = "statement.csv",
        existingTransactions: List<Transaction> = emptyList(),
        availableCategories: List<Category> = emptyList()
    ): ImportParseResult {
        val lines = splitCsvLines(csvContent)
        if (lines.isEmpty()) {
            return ImportParseResult(
                fileName = fileName,
                totalRows = 0,
                validCount = 0,
                duplicateCount = 0,
                invalidCount = 0,
                rows = emptyList()
            )
        }

        // Find header row and parse columns
        val headerInfo = findHeaderRow(lines)
        val dataLinesWithIndices = if (headerInfo != null) {
            lines.mapIndexed { idx, line -> idx to line }
                .drop(headerInfo.headerIndex + 1)
        } else {
            // Fallback: assume line 0 is header or standard positional
            lines.mapIndexed { idx, line -> idx to line }.drop(1)
        }

        val parsedRows = mutableListOf<ParsedImportRow>()
        var validCount = 0
        var duplicateCount = 0
        var invalidCount = 0

        for ((lineIndex, lineStr) in dataLinesWithIndices) {
            val trimmed = lineStr.trim()
            if (trimmed.isBlank() || isBankFooterOrTotal(trimmed)) {
                continue
            }

            val tokens = parseCsvLine(trimmed)
            if (tokens.isEmpty() || tokens.all { it.isBlank() }) {
                continue
            }

            val parseOutcome = parseRow(
                tokens = tokens,
                headerInfo = headerInfo,
                lineIndex = lineIndex,
                rawLine = trimmed,
                availableCategories = availableCategories
            )

            when (parseOutcome) {
                is RowParseOutcome.Invalid -> {
                    invalidCount++
                    parsedRows.add(
                        ParsedImportRow(
                            rowIndex = lineIndex,
                            transaction = null,
                            rawLine = trimmed,
                            status = ImportRowStatus.INVALID,
                            reason = parseOutcome.reason,
                            isSelected = false
                        )
                    )
                }
                is RowParseOutcome.Success -> {
                    val candidate = parseOutcome.transaction
                    val duplicate = TransactionDeduplicator.findDuplicate(candidate, existingTransactions)

                    if (duplicate != null) {
                        duplicateCount++
                        val reason = "Matches ₹${duplicate.formattedRupees} (${duplicate.counterpartyName})"
                        parsedRows.add(
                            ParsedImportRow(
                                rowIndex = lineIndex,
                                transaction = candidate,
                                rawLine = trimmed,
                                status = ImportRowStatus.DUPLICATE,
                                reason = reason,
                                matchedDuplicate = duplicate,
                                isSelected = false // Unchecked by default for duplicates
                            )
                        )
                    } else {
                        validCount++
                        parsedRows.add(
                            ParsedImportRow(
                                rowIndex = lineIndex,
                                transaction = candidate,
                                rawLine = trimmed,
                                status = ImportRowStatus.VALID,
                                isSelected = true
                            )
                        )
                    }
                }
            }
        }

        return ImportParseResult(
            fileName = fileName,
            totalRows = parsedRows.size,
            validCount = validCount,
            duplicateCount = duplicateCount,
            invalidCount = invalidCount,
            rows = parsedRows
        )
    }

    private sealed class RowParseOutcome {
        data class Success(val transaction: Transaction) : RowParseOutcome()
        data class Invalid(val reason: String) : RowParseOutcome()
    }

    private data class HeaderInfo(
        val headerIndex: Int,
        val dateCol: Int,
        val timeCol: Int? = null,
        val descriptionCol: Int,
        val refCol: Int? = null,
        val amountCol: Int? = null,
        val debitCol: Int? = null,
        val creditCol: Int? = null,
        val typeCol: Int? = null,
        val categoryCol: Int? = null,
        val appCol: Int? = null,
        val notesCol: Int? = null
    )

    private fun findHeaderRow(lines: List<String>): HeaderInfo? {
        for (idx in 0 until minOf(25, lines.size)) {
            val line = lines[idx]
            val tokens = parseCsvLine(line).map { it.trim().lowercase(Locale.ENGLISH) }

            val dateCol = tokens.indexOfFirst {
                it.contains("date") || it == "txn date" || it == "transaction date" || it == "value dt" || it == "value date"
            }
            val descCol = tokens.indexOfFirst {
                it.contains("narration") || it.contains("description") || it.contains("particulars") ||
                        it.contains("merchant") || it.contains("counterparty") || it.contains("remarks") ||
                        it.contains("details") || it.contains("paid to") || it.contains("payee")
            }
            val debitCol = tokens.indexOfFirst {
                it.contains("withdrawal") || it == "debit" || it == "dr" || it == "withdrawal amt." || it == "debit amt"
            }
            val creditCol = tokens.indexOfFirst {
                it.contains("deposit") || it == "credit" || it == "cr" || it == "deposit amt." || it == "credit amt"
            }
            val amountCol = tokens.indexOfFirst {
                it == "amount" || it == "txn amount" || it == "net amount" || it.contains("amount")
            }

            // Check if this line looks like a valid header
            if (dateCol != -1 && (descCol != -1 || amountCol != -1 || debitCol != -1)) {
                val timeCol = tokens.indexOfFirst { it.contains("time") }.takeIf { it != -1 && it != dateCol }
                val refCol = tokens.indexOfFirst {
                    it.contains("ref") || it.contains("utr") || it.contains("chq") || it.contains("cheque") ||
                            it.contains("transaction id") || it.contains("txn id")
                }.takeIf { it != -1 }

                val typeCol = tokens.indexOfFirst {
                    it == "type" || it == "direction" || it == "dr/cr" || it == "cr/dr" || it == "txn type"
                }.takeIf { it != -1 }

                val categoryCol = tokens.indexOfFirst { it.contains("category") || it == "tag" }.takeIf { it != -1 }
                val appCol = tokens.indexOfFirst {
                    it.contains("app") || it.contains("mode") || it.contains("channel")
                }.takeIf { it != -1 }
                val notesCol = tokens.indexOfFirst { it.contains("notes") || it.contains("comments") }.takeIf { it != -1 }

                return HeaderInfo(
                    headerIndex = idx,
                    dateCol = dateCol,
                    timeCol = timeCol,
                    descriptionCol = if (descCol != -1) descCol else 0,
                    refCol = refCol,
                    amountCol = if (amountCol != -1) amountCol else null,
                    debitCol = if (debitCol != -1) debitCol else null,
                    creditCol = if (creditCol != -1) creditCol else null,
                    typeCol = typeCol,
                    categoryCol = categoryCol,
                    appCol = appCol,
                    notesCol = notesCol
                )
            }
        }
        return null
    }

    private fun parseRow(
        tokens: List<String>,
        headerInfo: HeaderInfo?,
        lineIndex: Int,
        rawLine: String,
        availableCategories: List<Category>
    ): RowParseOutcome {
        val dateCol = headerInfo?.dateCol ?: 0
        val descCol = headerInfo?.descriptionCol ?: 1

        if (dateCol >= tokens.size) {
            return RowParseOutcome.Invalid("Row does not contain date column")
        }

        val dateStr = tokens[dateCol].trim()
        val timeStr = headerInfo?.timeCol?.let { if (it < tokens.size) tokens[it].trim() else null }
        val timestampMs = parseDateTimeToTimestamp(dateStr, timeStr)
            ?: return RowParseOutcome.Invalid("Invalid date: '$dateStr'")

        val rawDescription = if (descCol < tokens.size) tokens[descCol].trim() else "Transaction"
        if (rawDescription.isBlank()) {
            return RowParseOutcome.Invalid("Missing transaction description/merchant")
        }

        // Amount and Direction parsing
        val amountAndDirection = determineAmountAndDirection(tokens, headerInfo)
            ?: return RowParseOutcome.Invalid("Unable to determine valid amount")

        val (amountPaise, direction, type) = amountAndDirection

        if (amountPaise <= 0L) {
            return RowParseOutcome.Invalid("Zero or negative transaction amount")
        }

        // Extract reference/UTR
        val refFromCol = headerInfo?.refCol?.let { if (it < tokens.size) tokens[it].trim().takeIf { s -> s.isNotBlank() } else null }
        val extractedUtr = refFromCol ?: extractUtrFromNarration(rawDescription)

        // Payment App
        val paymentApp = headerInfo?.appCol?.let { if (it < tokens.size) tokens[it].trim().takeIf { s -> s.isNotBlank() } else null }
            ?: detectPaymentApp(rawDescription)

        // Normalize merchant
        val normalizedMerchant = MerchantNormalizer.normalize(rawDescription)

        // Category resolution
        val explicitCategory = headerInfo?.categoryCol?.let { if (it < tokens.size) tokens[it].trim().takeIf { s -> s.isNotBlank() } else null }
        val (categoryId, categoryName, categoryColor) = resolveCategory(
            explicitCategory = explicitCategory,
            description = rawDescription,
            direction = direction,
            availableCategories = availableCategories
        )

        val notes = headerInfo?.notesCol?.let { if (it < tokens.size) tokens[it].trim().takeIf { s -> s.isNotBlank() } else null }

        val transaction = Transaction(
            id = 0L,
            amountPaise = amountPaise,
            type = type,
            direction = direction,
            status = TransactionStatus.COMPLETED,
            title = normalizedMerchant,
            counterpartyName = normalizedMerchant,
            counterpartyUpiId = extractUpiId(rawDescription),
            categoryId = categoryId,
            categoryName = categoryName,
            categoryColorHex = categoryColor,
            timestampMs = timestampMs,
            paymentMethod = "UPI",
            paymentApp = paymentApp,
            bankReferenceNum = extractedUtr,
            notes = notes ?: if (rawDescription != normalizedMerchant) rawDescription else null,
            source = TransactionSource.CSV_IMPORT,
            rawIdentifier = "CSV_${lineIndex}_${timestampMs}_${amountPaise}"
        )

        return RowParseOutcome.Success(transaction)
    }

    private fun determineAmountAndDirection(
        tokens: List<String>,
        headerInfo: HeaderInfo?
    ): Triple<Long, TransactionDirection, TransactionType>? {
        if (headerInfo != null && headerInfo.debitCol != null && headerInfo.creditCol != null) {
            val debitStr = if (headerInfo.debitCol < tokens.size) tokens[headerInfo.debitCol] else ""
            val creditStr = if (headerInfo.creditCol < tokens.size) tokens[headerInfo.creditCol] else ""

            val debitPaise = parseAmountToPaise(debitStr)
            val creditPaise = parseAmountToPaise(creditStr)

            if (debitPaise != null && debitPaise > 0L) {
                return Triple(debitPaise, TransactionDirection.OUTGOING, TransactionType.EXPENSE)
            } else if (creditPaise != null && creditPaise > 0L) {
                return Triple(creditPaise, TransactionDirection.INCOMING, TransactionType.INCOME)
            }
        }

        // Check unified amount column or find first number column
        val amountCol = headerInfo?.amountCol ?: tokens.indices.firstOrNull { idx ->
            idx != headerInfo?.dateCol && idx != headerInfo?.descriptionCol && parseAmountToPaise(tokens[idx]) != null
        } ?: return null

        if (amountCol >= tokens.size) return null

        val rawAmountStr = tokens[amountCol].trim()
        val isNegative = rawAmountStr.startsWith("-") || rawAmountStr.startsWith("(") && rawAmountStr.endsWith(")")
        val amountPaise = parseAmountToPaise(rawAmountStr) ?: return null

        // Check type column
        val typeStr = headerInfo?.typeCol?.let { if (it < tokens.size) tokens[it].trim().uppercase(Locale.ENGLISH) else null }

        val (direction, type) = when {
            typeStr != null -> {
                when {
                    typeStr.contains("REFUND") -> TransactionDirection.INCOMING to TransactionType.REFUND
                    typeStr.contains("REVERSAL") -> TransactionDirection.INCOMING to TransactionType.REVERSAL
                    typeStr.contains("CR") || typeStr.contains("CREDIT") || typeStr.contains("INCOME") ->
                        TransactionDirection.INCOMING to TransactionType.INCOME
                    typeStr.contains("DR") || typeStr.contains("DEBIT") || typeStr.contains("EXPENSE") ->
                        TransactionDirection.OUTGOING to TransactionType.EXPENSE
                    isNegative -> TransactionDirection.OUTGOING to TransactionType.EXPENSE
                    else -> TransactionDirection.OUTGOING to TransactionType.EXPENSE
                }
            }
            isNegative -> TransactionDirection.OUTGOING to TransactionType.EXPENSE
            else -> TransactionDirection.OUTGOING to TransactionType.EXPENSE
        }

        return Triple(amountPaise, direction, type)
    }

    /**
     * Parses a string representation of an amount into exact 64-bit integer paise.
     * ₹1 = 100 paise.
     */
    fun parseAmountToPaise(raw: String): Long? {
        val clean = raw.replace("₹", "")
            .replace("Rs.", "")
            .replace("Rs", "")
            .replace("INR", "")
            .replace(",", "")
            .replace("(", "")
            .replace(")", "")
            .trim()

        if (clean.isBlank()) return null

        return try {
            val bigDec = BigDecimal(clean).abs()
            bigDec.multiply(BigDecimal(100)).toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDateTimeToTimestamp(dateStr: String, timeStr: String?): Long? {
        val combined = if (!timeStr.isNullOrBlank()) "$dateStr $timeStr" else dateStr

        for (pattern in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    isLenient = false
                }
                val parsed = sdf.parse(combined)
                if (parsed != null) {
                    return parsed.time
                }
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return null
    }

    private fun extractUtrFromNarration(narration: String): String? {
        val utrMatcher = UTR_REGEX.matcher(narration)
        if (utrMatcher.find()) {
            return utrMatcher.group(1)
        }
        val standaloneMatcher = STANDALONE_12_DIGIT_REGEX.matcher(narration)
        if (standaloneMatcher.find()) {
            return standaloneMatcher.group(1)
        }
        return null
    }

    private fun extractUpiId(narration: String): String? {
        val upiMatcher = Pattern.compile("([a-zA-Z0-9.\\-_]+@[a-zA-Z0-9]+)").matcher(narration)
        return if (upiMatcher.find()) upiMatcher.group(1) else null
    }

    private fun detectPaymentApp(text: String): String {
        val lower = text.lowercase(Locale.ENGLISH)
        return when {
            lower.contains("gpay") || lower.contains("google pay") -> "Google Pay"
            lower.contains("phonepe") -> "PhonePe"
            lower.contains("paytm") -> "Paytm"
            lower.contains("cred") -> "CRED"
            lower.contains("bhim") -> "BHIM UPI"
            lower.contains("amazon") && lower.contains("upi") -> "Amazon Pay"
            else -> "Google Pay"
        }
    }

    private fun resolveCategory(
        explicitCategory: String?,
        description: String,
        direction: TransactionDirection,
        availableCategories: List<Category>
    ): Triple<Long, String, String> {
        // If direction is incoming and contains salary/income words
        if (direction == TransactionDirection.INCOMING) {
            val incomeCat = availableCategories.find { it.type == "INCOME" || it.id == 7L }
            return Triple(
                incomeCat?.id ?: 7L,
                incomeCat?.name ?: "Salary",
                incomeCat?.colorHex ?: "#00E676"
            )
        }

        // If explicit category is provided, match with available
        if (!explicitCategory.isNullOrBlank()) {
            val matched = availableCategories.find {
                it.name.equals(explicitCategory, ignoreCase = true)
            }
            if (matched != null) {
                return Triple(matched.id, matched.name, matched.colorHex)
            }
        }

        // Smart keyword inference
        val lower = description.lowercase(Locale.ENGLISH)

        fun findCat(name: String, fallbackId: Long, fallbackColor: String): Triple<Long, String, String> {
            val found = availableCategories.find { it.name.contains(name, ignoreCase = true) }
            return Triple(
                found?.id ?: fallbackId,
                found?.name ?: name,
                found?.colorHex ?: fallbackColor
            )
        }

        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("starbucks") ||
                    lower.contains("mcdonald") || lower.contains("burger") || lower.contains("pizza") ||
                    lower.contains("cafe") || lower.contains("restaurant") || lower.contains("chai") ||
                    lower.contains("bakery") || lower.contains("dining") || lower.contains("eat") ||
                    lower.contains("kfc") || lower.contains("subway") || lower.contains("haldiram") ->
                findCat("Food & Dining", 1L, "#FF7A00")

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") ||
                    lower.contains("meesho") || lower.contains("zara") || lower.contains("h&m") ||
                    lower.contains("uniqlo") || lower.contains("ajio") || lower.contains("nykaa") ||
                    lower.contains("retail") || lower.contains("shopping") || lower.contains("apparel") ->
                findCat("Shopping", 2L, "#7C5CFC")

            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") ||
                    lower.contains("metro") || lower.contains("irctc") || lower.contains("petrol") ||
                    lower.contains("fuel") || lower.contains("indian oil") || lower.contains("iocl") ||
                    lower.contains("hpcl") || lower.contains("bpcl") || lower.contains("shell") ||
                    lower.contains("flight") || lower.contains("makemytrip") || lower.contains("indigo") ->
                findCat("Travel", 3L, "#2979FF")

            lower.contains("bescom") || lower.contains("electricity") || lower.contains("airtel") ||
                    lower.contains("jio") || lower.contains("vi") || lower.contains("vodafone") ||
                    lower.contains("broadband") || lower.contains("water") || lower.contains("gas") ||
                    lower.contains("bill") || lower.contains("recharge") || lower.contains("tata play") ||
                    lower.contains("dth") ->
                findCat("Bills & Utilities", 4L, "#00C853")

            lower.contains("netflix") || lower.contains("spotify") || lower.contains("bookmyshow") ||
                    lower.contains("cinema") || lower.contains("prime") || lower.contains("youtube") ||
                    lower.contains("pvr") || lower.contains("inox") || lower.contains("hotstar") ||
                    lower.contains("steam") || lower.contains("game") ->
                findCat("Entertainment", 5L, "#E040FB")

            lower.contains("zerodha") || lower.contains("groww") || lower.contains("mutual") ||
                    lower.contains("fund") || lower.contains("sip") || lower.contains("upstox") ||
                    lower.contains("coin") || lower.contains("angel") || lower.contains("share") ->
                findCat("Investment", 6L, "#00B0FF")

            else ->
                findCat("Others", 8L, "#546E7A")
        }
    }

    private fun isBankFooterOrTotal(line: String): Boolean {
        val lower = line.lowercase(Locale.ENGLISH)
        return lower.startsWith("total") ||
                lower.startsWith("opening balance") ||
                lower.startsWith("closing balance") ||
                lower.startsWith("statement generated") ||
                lower.startsWith("page ") ||
                lower.startsWith("note:") ||
                lower.startsWith("* this is a computer")
    }

    /**
     * Splits CSV input into raw lines handling CRLF and LF.
     */
    private fun splitCsvLines(csv: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (char in csv) {
            when (char) {
                '"' -> {
                    inQuotes = !inQuotes
                    sb.append(char)
                }
                '\n' -> {
                    if (inQuotes) {
                        sb.append(char)
                    } else {
                        result.add(sb.toString().trimEnd('\r'))
                        sb.clear()
                    }
                }
                else -> sb.append(char)
            }
        }
        if (sb.isNotEmpty()) {
            result.add(sb.toString().trimEnd('\r'))
        }
        return result
    }

    /**
     * Tokenizes a single CSV line into columns, respecting quoted strings and escaped double quotes.
     */
    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote: "" -> "
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(char)
                }
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
