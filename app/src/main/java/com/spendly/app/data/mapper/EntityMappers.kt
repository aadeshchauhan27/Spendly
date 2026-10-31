package com.spendly.app.data.mapper

import com.spendly.app.data.local.entity.BudgetEntity
import com.spendly.app.data.local.entity.CategoryEntity
import com.spendly.app.data.local.entity.GoalContributionEntity
import com.spendly.app.data.local.entity.ImportSessionEntity
import com.spendly.app.data.local.entity.SavingsGoalEntity
import com.spendly.app.data.local.entity.TransactionEntity
import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.GoalContribution
import com.spendly.app.domain.model.ImportSession
import com.spendly.app.domain.model.SavingsGoal
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType

fun TransactionEntity.toDomain(
    categoryName: String = "",
    categoryColorHex: String = "#546E7A",
    categoryIconName: String = ""
): Transaction {
    return Transaction(
        id = id,
        amountPaise = amountPaise,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE },
        direction = try { TransactionDirection.valueOf(direction) } catch (e: Exception) { TransactionDirection.OUTGOING },
        status = try { TransactionStatus.valueOf(status) } catch (e: Exception) { TransactionStatus.COMPLETED },
        title = title,
        counterpartyName = counterpartyName,
        counterpartyUpiId = counterpartyUpiId,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryColorHex = categoryColorHex,
        categoryIconName = categoryIconName,
        timestampMs = timestampMs,
        paymentMethod = paymentMethod,
        paymentApp = paymentApp,
        bankReferenceNum = bankReferenceNum,
        notes = notes,
        tag = tag,
        source = try { TransactionSource.valueOf(source) } catch (e: Exception) { TransactionSource.MANUAL },
        importSessionId = importSessionId,
        rawIdentifier = rawIdentifier,
        relatedTransactionId = relatedTransactionId,
        isDemo = isDemo,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amountPaise = amountPaise,
        type = type.name,
        direction = direction.name,
        status = status.name,
        title = title,
        counterpartyName = counterpartyName,
        counterpartyUpiId = counterpartyUpiId,
        categoryId = categoryId,
        timestampMs = timestampMs,
        paymentMethod = paymentMethod,
        paymentApp = paymentApp,
        bankReferenceNum = bankReferenceNum,
        notes = notes,
        tag = tag,
        source = source.name,
        importSessionId = importSessionId,
        rawIdentifier = rawIdentifier,
        relatedTransactionId = relatedTransactionId,
        isDemo = isDemo,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs
    )
}

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        type = type,
        colorHex = colorHex,
        iconName = iconName,
        isSystem = isSystem,
        sortOrder = sortOrder
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        type = type,
        colorHex = colorHex,
        iconName = iconName,
        isSystem = isSystem,
        sortOrder = sortOrder
    )
}

fun BudgetEntity.toDomain(categoryName: String? = null, categoryColorHex: String? = null, spentPaise: Long = 0L): Budget {
    return Budget(
        id = id,
        categoryId = categoryId,
        categoryName = categoryName,
        categoryColorHex = categoryColorHex,
        amountPaise = amountPaise,
        spentPaise = spentPaise,
        periodMonth = periodMonth,
        periodYear = periodYear,
        isActive = isActive,
        thresholdPercent = thresholdPercent,
        alertTriggered = alertTriggered,
        isDemo = isDemo
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        categoryId = categoryId,
        amountPaise = amountPaise,
        periodMonth = periodMonth,
        periodYear = periodYear,
        isActive = isActive,
        thresholdPercent = thresholdPercent,
        alertTriggered = alertTriggered,
        isDemo = isDemo
    )
}

fun SavingsGoalEntity.toDomain(): SavingsGoal {
    return SavingsGoal(
        id = id,
        name = name,
        targetAmountPaise = targetAmountPaise,
        savedAmountPaise = savedAmountPaise,
        deadlineMs = deadlineMs,
        iconName = iconName,
        colorHex = colorHex,
        isCompleted = isCompleted,
        isDemo = isDemo,
        createdAtMs = createdAtMs
    )
}

fun SavingsGoal.toEntity(): SavingsGoalEntity {
    return SavingsGoalEntity(
        id = id,
        name = name,
        targetAmountPaise = targetAmountPaise,
        savedAmountPaise = savedAmountPaise,
        deadlineMs = deadlineMs,
        iconName = iconName,
        colorHex = colorHex,
        isCompleted = isCompleted,
        isDemo = isDemo,
        createdAtMs = createdAtMs
    )
}

fun GoalContributionEntity.toDomain(): GoalContribution {
    return GoalContribution(
        id = id,
        goalId = goalId,
        amountPaise = amountPaise,
        note = note,
        timestampMs = timestampMs
    )
}

fun GoalContribution.toEntity(): GoalContributionEntity {
    return GoalContributionEntity(
        id = id,
        goalId = goalId,
        amountPaise = amountPaise,
        note = note,
        timestampMs = timestampMs
    )
}

fun ImportSessionEntity.toDomain(): ImportSession {
    return ImportSession(
        id = id,
        source = source,
        fileName = fileName,
        totalRows = totalRows,
        importedCount = importedCount,
        duplicateCount = duplicateCount,
        errorCount = errorCount,
        timestampMs = timestampMs
    )
}

fun ImportSession.toEntity(): ImportSessionEntity {
    return ImportSessionEntity(
        id = id,
        source = source,
        fileName = fileName,
        totalRows = totalRows,
        importedCount = importedCount,
        duplicateCount = duplicateCount,
        errorCount = errorCount,
        timestampMs = timestampMs
    )
}

