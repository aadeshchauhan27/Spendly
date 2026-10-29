package com.spendly.app.ui.preview

import androidx.compose.ui.graphics.Color
import com.spendly.app.core.components.DonutSegment
import com.spendly.app.core.theme.CategoryBlueTravel
import com.spendly.app.core.theme.CategoryGreenBills
import com.spendly.app.core.theme.CategoryOrangeFood
import com.spendly.app.core.theme.CategoryPinkEntertainment
import com.spendly.app.core.theme.CategoryPurpleShopping
import com.spendly.app.core.theme.CategorySlateOthers

/**
 * Isolated visual preview dataset matching the approved reference design.
 * NOTE: For Phase 1 UI visualization only. In Phase 2 & 3, real Room database Flow pipelines replace this.
 */
object PreviewDataProvider {

    data class PreviewTx(
        val id: String,
        val title: String,
        val category: String,
        val amount: String,
        val time: String,
        val isIncoming: Boolean,
        val dateGroup: String,
        val paymentMethod: String = "UPI (Google Pay)",
        val upiId: String = "aditya@okaxis",
        val refId: String = "512345678901",
        val note: String? = null
    )

    data class PreviewCategory(
        val name: String,
        val percent: Int,
        val amount: String,
        val color: Color
    )

    data class PreviewBudget(
        val category: String,
        val spent: String,
        val total: String,
        val progress: Float,
        val percentText: String,
        val color: Color
    )

    data class PreviewGoal(
        val title: String,
        val saved: String,
        val target: String,
        val progress: Float,
        val percentText: String,
        val iconType: String
    )

    val sampleTransactions = listOf(
        PreviewTx("1", "Swiggy", "Food & Dining", "₹320", "12:45 PM", false, "Today", upiId = "swiggy@axisbank", refId = "512345678901"),
        PreviewTx("2", "Amazon", "Shopping", "₹1,299", "10:20 AM", false, "Today", upiId = "amazonpay@apl", refId = "512345678902"),
        PreviewTx("3", "Received from Rohan", "Transfer", "₹2,000", "09:15 AM", true, "Today", upiId = "rohan@okhdfcbank", refId = "512345678903"),
        PreviewTx("4", "Uber", "Travel", "₹450", "08:30 PM", false, "Yesterday", upiId = "uber@icici", refId = "512345678904"),
        PreviewTx("5", "Jio", "Bills", "₹799", "06:12 PM", false, "Yesterday", upiId = "jio@axisbank", refId = "512345678905")
    )

    val categoryBreakdown = listOf(
        PreviewCategory("Food & Dining", 32, "₹7,860", CategoryOrangeFood),
        PreviewCategory("Shopping", 18, "₹4,420", CategoryPurpleShopping),
        PreviewCategory("Travel", 12, "₹2,950", CategoryBlueTravel),
        PreviewCategory("Bills & Utilities", 9, "₹2,210", CategoryGreenBills),
        PreviewCategory("Entertainment", 8, "₹1,970", CategoryPinkEntertainment),
        PreviewCategory("Others", 21, "₹5,150", CategorySlateOthers)
    )

    val donutSegments = listOf(
        DonutSegment("Food & Dining", 7860f, CategoryOrangeFood),
        DonutSegment("Shopping", 4420f, CategoryPurpleShopping),
        DonutSegment("Travel", 2950f, CategoryBlueTravel),
        DonutSegment("Bills & Utilities", 2210f, CategoryGreenBills),
        DonutSegment("Entertainment", 1970f, CategoryPinkEntertainment),
        DonutSegment("Others", 5150f, CategorySlateOthers)
    )

    val categoryBudgets = listOf(
        PreviewBudget("Food & Dining", "₹7,860", "₹10,000", 0.786f, "80%", CategoryOrangeFood),
        PreviewBudget("Shopping", "₹3,100", "₹5,000", 0.62f, "62%", CategoryPurpleShopping),
        PreviewBudget("Travel", "₹1,500", "₹5,000", 0.30f, "60%", CategoryBlueTravel)
    )

    val savingsGoals = listOf(
        PreviewGoal("Emergency Fund", "₹15,000", "₹50,000", 0.30f, "30%", "plant"),
        PreviewGoal("Vacation", "₹22,000", "₹1,00,000", 0.22f, "22%", "flight"),
        PreviewGoal("New Laptop", "₹12,000", "₹80,000", 0.15f, "15%", "laptop")
    )

    val trendPoints = listOf(1200f, 1800f, 1400f, 2200f, 1900f, 2800f, 2400f, 3100f, 2900f, 3500f)
}
