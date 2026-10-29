package com.spendly.app.domain.engine

import com.spendly.app.domain.model.Budget
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionDirection
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType

enum class InsightType {
    POSITIVE,
    WARNING,
    NEUTRAL,
    TIP
}

data class FinancialInsight(
    val id: String,
    val title: String,
    val description: String,
    val type: InsightType,
    val actionLabel: String? = null
)

object InsightsEngine {

    fun generateInsights(
        currentTransactions: List<Transaction>,
        previousTransactions: List<Transaction>,
        budgets: List<Budget>,
        daysInPeriod: Int
    ): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()

        val currentExpenses = currentTransactions.filter {
            it.type == TransactionType.EXPENSE && it.status == TransactionStatus.COMPLETED
        }
        val currentIncome = currentTransactions.filter {
            it.type == TransactionType.INCOME && it.status == TransactionStatus.COMPLETED
        }

        val totalSpendPaise = currentExpenses.sumOf { it.amountPaise }
        val totalIncomePaise = currentIncome.sumOf { it.amountPaise }

        // 1. Top Spending Category Insight
        if (currentExpenses.isNotEmpty() && totalSpendPaise > 0L) {
            val categoryGroup = currentExpenses.groupBy { it.categoryName.ifBlank { "Others" } }
                .mapValues { (_, txList) -> txList.sumOf { it.amountPaise } }
                .toList()
                .sortedByDescending { it.second }

            if (categoryGroup.isNotEmpty()) {
                val (topCategory, topSpendPaise) = categoryGroup.first()
                val percent = ((topSpendPaise.toDouble() / totalSpendPaise) * 100).toInt()
                val topRupees = (topSpendPaise / 100).toString()

                insights.add(
                    FinancialInsight(
                        id = "top_category",
                        title = "Top Category: $topCategory",
                        description = "$topCategory was your highest expense at ₹$topRupees ($percent% of total spending).",
                        type = if (percent > 40) InsightType.WARNING else InsightType.NEUTRAL
                    )
                )
            }
        }

        // 2. Period vs Period Comparison Insight
        val prevSpendPaise = previousTransactions.filter {
            it.type == TransactionType.EXPENSE && it.status == TransactionStatus.COMPLETED
        }.sumOf { it.amountPaise }

        if (prevSpendPaise > 0L && totalSpendPaise > 0L) {
            val changePercent = (((totalSpendPaise - prevSpendPaise).toDouble() / prevSpendPaise) * 100).toInt()
            if (changePercent < 0) {
                insights.add(
                    FinancialInsight(
                        id = "trend_down",
                        title = "Spending Decreased",
                        description = "Awesome discipline! Your spending decreased by ${Math.abs(changePercent)}% compared to the previous period.",
                        type = InsightType.POSITIVE
                    )
                )
            } else if (changePercent > 10) {
                insights.add(
                    FinancialInsight(
                        id = "trend_up",
                        title = "Spending Increased",
                        description = "Outgoing spending is up by $changePercent% vs last period. Keep an eye on dining and impulse purchases.",
                        type = InsightType.WARNING
                    )
                )
            }
        }

        // 3. Overall Budget Status Insight
        val overallBudget = budgets.find { it.categoryId == null }
        if (overallBudget != null && overallBudget.amountPaise > 0L) {
            val percentUsed = overallBudget.usedPercent
            if (percentUsed >= 100) {
                insights.add(
                    FinancialInsight(
                        id = "budget_exceeded",
                        title = "Monthly Budget Exceeded",
                        description = "You have exceeded your ₹${overallBudget.formattedTarget} monthly budget by ₹${overallBudget.formattedRemaining}.",
                        type = InsightType.WARNING,
                        actionLabel = "Adjust Budget"
                    )
                )
            } else if (percentUsed >= 80) {
                insights.add(
                    FinancialInsight(
                        id = "budget_warning",
                        title = "Approaching Budget Limit",
                        description = "You've used $percentUsed% of your monthly budget. Only ₹${overallBudget.formattedRemaining} remaining.",
                        type = InsightType.WARNING
                    )
                )
            }
        }

        // 4. Daily Spending Average
        if (daysInPeriod > 0 && totalSpendPaise > 0L) {
            val avgDailyRupees = (totalSpendPaise / 100) / daysInPeriod
            insights.add(
                FinancialInsight(
                    id = "daily_average",
                    title = "Daily Average Spend",
                    description = "You spent an average of ₹$avgDailyRupees per day during this $daysInPeriod-day period.",
                    type = InsightType.NEUTRAL
                )
            )
        }

        // 5. Savings Rate Insight
        if (totalIncomePaise > 0L && totalSpendPaise > 0L) {
            val savingsPaise = totalIncomePaise - totalSpendPaise
            if (savingsPaise > 0L) {
                val savingsRate = ((savingsPaise.toDouble() / totalIncomePaise) * 100).toInt()
                insights.add(
                    FinancialInsight(
                        id = "savings_rate",
                        title = "Positive Cash Flow",
                        description = "You saved $savingsRate% of your income this period (net cash flow: +₹${savingsPaise / 100}).",
                        type = InsightType.POSITIVE
                    )
                )
            }
        }

        return insights
    }
}
