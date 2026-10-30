package com.spendly.app.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.spendly.app.core.components.BottomTab
import com.spendly.app.core.components.SpendlyBottomBar
import com.spendly.app.core.preferences.UserPreferences
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.ThemeMode
import com.spendly.app.data.repository.SpendlyRepository
import com.spendly.app.domain.model.TrendPoint
import com.spendly.app.ui.analytics.AnalyticsScreen
import com.spendly.app.ui.analytics.AnalyticsViewModel
import com.spendly.app.ui.budgets.BudgetsScreen
import com.spendly.app.ui.budgets.BudgetsViewModel
import com.spendly.app.ui.categories.CategoriesScreen
import com.spendly.app.ui.categories.CategoriesViewModel
import com.spendly.app.ui.dashboard.DashboardScreen
import com.spendly.app.ui.dashboard.DashboardViewModel
import com.spendly.app.ui.goals.GoalsViewModel
import com.spendly.app.ui.goals.SavingsGoalsScreen
import com.spendly.app.ui.import_export.ImportViewModel
import com.spendly.app.ui.onboarding.OnboardingScreen
import com.spendly.app.ui.permissions.PermissionsScreen
import com.spendly.app.ui.settings.SettingsScreen
import com.spendly.app.ui.sms.SmsSyncViewModel
import com.spendly.app.ui.splash.SplashScreen
import com.spendly.app.ui.transactiondetail.TransactionDetailScreen
import com.spendly.app.ui.transactiondetail.TransactionDetailViewModel
import com.spendly.app.ui.transactions.TransactionsScreen
import com.spendly.app.ui.transactions.TransactionsViewModel
import com.spendly.app.ui.upi.UpiTransactionsScreen
import com.spendly.app.ui.upi.UpiTransactionsViewModel
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SpendlyNavGraph(
    navController: NavHostController,
    userPreferences: UserPreferences,
    repository: SpendlyRepository,
    currentTheme: ThemeMode,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Transactions.route,
        Screen.Analytics.route,
        Screen.UpiTransactions.route,
        Screen.Settings.route
    )

    val showBottomBar = currentDestination in bottomBarRoutes
    val onboardingCompleted by userPreferences.onboardingCompletedFlow.collectAsState(initial = false)
    val sharedTransactionsViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.Factory(repository)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpendlyTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar && currentDestination != null) {
                SpendlyBottomBar(
                    currentRoute = currentDestination,
                    onTabSelected = { tab ->
                        if (currentDestination != tab.route) {
                            navController.navigate(tab.route) {
                                popUpTo(Screen.Dashboard.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onGetStartedClick = {
                        if (onboardingCompleted) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Permissions.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Permissions.route) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    coroutineScope.launch {
                        userPreferences.setOnboardingCompleted(true)
                    }
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                }

                PermissionsScreen(
                    onAllowClick = {
                        val perms = buildList {
                            add(Manifest.permission.READ_SMS)
                            add(Manifest.permission.RECEIVE_SMS)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }.toTypedArray()
                        permissionLauncher.launch(perms)
                    },
                    onSkipClick = {
                        coroutineScope.launch {
                            userPreferences.setOnboardingCompleted(true)
                        }
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Permissions.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(repository, userPreferences)
                )
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onToggleTheme = {
                        coroutineScope.launch {
                            val nextTheme = if (currentTheme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                            userPreferences.setThemeMode(nextTheme)
                        }
                    },
                    onViewAllCategories = {
                        navController.navigate(Screen.Categories.route)
                    },
                    onTransactionClick = { txId ->
                        navController.navigate(Screen.TransactionDetail.createRoute(txId.toString()))
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onTrendPointClick = { point ->
                        if (point.timestampMs > 0L) {
                            sharedTransactionsViewModel.filterByDay(point.timestampMs)
                        }
                        navController.navigate(Screen.Transactions.route)
                    }
                )
            }

            composable(Screen.Transactions.route) {
                val importViewModel: ImportViewModel = viewModel(
                    factory = ImportViewModel.Factory(repository)
                )
                val smsSyncViewModel: SmsSyncViewModel = viewModel(
                    factory = SmsSyncViewModel.Factory(repository)
                )
                TransactionsScreen(
                    viewModel = sharedTransactionsViewModel,
                    importViewModel = importViewModel,
                    smsSyncViewModel = smsSyncViewModel,
                    onTransactionClick = { txId ->
                        navController.navigate(Screen.TransactionDetail.createRoute(txId.toString()))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val txId = backStackEntry.arguments?.getLong("transactionId") ?: 1L
                val detailViewModel: TransactionDetailViewModel = viewModel(
                    factory = TransactionDetailViewModel.Factory(txId, repository)
                )
                TransactionDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Categories.route) {
                val categoriesViewModel: CategoriesViewModel = viewModel(
                    factory = CategoriesViewModel.Factory(repository)
                )
                CategoriesScreen(
                    viewModel = categoriesViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Budgets.route) {
                val budgetsViewModel: BudgetsViewModel = viewModel(
                    factory = BudgetsViewModel.Factory(repository)
                )
                BudgetsScreen(
                    viewModel = budgetsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Analytics.route) {
                val analyticsViewModel: AnalyticsViewModel = viewModel(
                    factory = AnalyticsViewModel.Factory(repository)
                )
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    onTransactionClick = { txId ->
                        navController.navigate(Screen.TransactionDetail.createRoute(txId.toString()))
                    }
                )
            }

            composable(Screen.UpiTransactions.route) {
                val upiViewModel: UpiTransactionsViewModel = viewModel(
                    factory = UpiTransactionsViewModel.Factory(repository)
                )
                UpiTransactionsScreen(
                    viewModel = upiViewModel,
                    onTransactionClick = { txId ->
                        navController.navigate(Screen.TransactionDetail.createRoute(txId.toString()))
                    }
                )
            }

            composable(Screen.Settings.route) {
                val importViewModel: ImportViewModel = viewModel(
                    factory = ImportViewModel.Factory(repository)
                )
                val smsSyncViewModel: SmsSyncViewModel = viewModel(
                    factory = SmsSyncViewModel.Factory(repository)
                )
                SettingsScreen(
                    currentTheme = currentTheme,
                    repository = repository,
                    importViewModel = importViewModel,
                    smsSyncViewModel = smsSyncViewModel,
                    onThemeChanged = { newTheme ->
                        coroutineScope.launch {
                            userPreferences.setThemeMode(newTheme)
                        }
                    }
                )
            }
        }
    }
}
