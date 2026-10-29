package com.spendly.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Permissions : Screen("permissions")
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction_detail/$transactionId"
    }
    object Categories : Screen("categories")
    object Budgets : Screen("budgets")
    object Analytics : Screen("analytics")
    object UpiTransactions : Screen("upi_transactions")
    object Settings : Screen("settings")
}
