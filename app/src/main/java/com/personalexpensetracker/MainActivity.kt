package com.personalexpensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.lifecycle.ViewModelProvider
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.repository.DataManagementRepositoryImpl
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
import com.personalexpensetracker.data.repository.IncomeRepositoryImpl
import com.personalexpensetracker.data.repository.RecurringExpenseRepositoryImpl
import com.personalexpensetracker.ui.screens.datamanagement.DataManagementScreen
import com.personalexpensetracker.ui.screens.datamanagement.DataManagementViewModel
import com.personalexpensetracker.domain.model.Expense
import com.personalexpensetracker.domain.model.Income
import com.personalexpensetracker.domain.model.RecurringExpense
import com.personalexpensetracker.domain.usecase.AddExpenseUseCase
import com.personalexpensetracker.domain.usecase.AddIncomeUseCase
import com.personalexpensetracker.domain.usecase.CreateRecurringExpenseUseCase
import com.personalexpensetracker.domain.usecase.DeleteExpenseUseCase
import com.personalexpensetracker.domain.usecase.DeleteIncomeUseCase
import com.personalexpensetracker.domain.usecase.GetExpensesUseCase
import com.personalexpensetracker.domain.usecase.GetFinancialBalanceUseCase
import com.personalexpensetracker.domain.usecase.GetIncomeByIdUseCase
import com.personalexpensetracker.domain.usecase.GetIncomesUseCase
import com.personalexpensetracker.domain.usecase.GetRecurringExpenseByIdUseCase
import com.personalexpensetracker.domain.usecase.GetCashFlowReportUseCase
import com.personalexpensetracker.domain.usecase.UpdateExpenseUseCase
import com.personalexpensetracker.domain.usecase.UpdateIncomeUseCase
import com.personalexpensetracker.domain.usecase.UpdateRecurringExpenseUseCase
import com.personalexpensetracker.ui.screens.addexpense.AddExpenseScreen
import com.personalexpensetracker.ui.screens.addexpense.AddExpenseViewModel
import com.personalexpensetracker.ui.screens.analytics.CashFlowAnalyticsScreen
import com.personalexpensetracker.ui.screens.analytics.CashFlowAnalyticsViewModel
import com.personalexpensetracker.ui.screens.dashboard.DashboardScreen
import com.personalexpensetracker.ui.screens.dashboard.DashboardViewModel
import com.personalexpensetracker.ui.screens.expenses.EditExpenseScreen
import com.personalexpensetracker.ui.screens.expenses.EditExpenseViewModel
import com.personalexpensetracker.ui.screens.expenses.ExpenseListScreen
import com.personalexpensetracker.ui.screens.expenses.ExpenseListViewModel
import com.personalexpensetracker.ui.screens.income.AddIncomeScreen
import com.personalexpensetracker.ui.screens.income.AddIncomeViewModel
import com.personalexpensetracker.ui.screens.income.EditIncomeScreen
import com.personalexpensetracker.ui.screens.income.EditIncomeViewModel
import com.personalexpensetracker.ui.screens.income.IncomeListScreen
import com.personalexpensetracker.ui.screens.income.IncomeListViewModel
import com.personalexpensetracker.ui.screens.income.IncomeListViewModelFactory
import com.personalexpensetracker.ui.screens.recurring.AddRecurringExpenseScreen
import com.personalexpensetracker.ui.screens.recurring.AddRecurringExpenseViewModel
import com.personalexpensetracker.ui.screens.recurring.EditRecurringExpenseScreen
import com.personalexpensetracker.ui.screens.recurring.EditRecurringExpenseViewModel
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import com.personalexpensetracker.data.preferences.UserPreferencesImpl
import com.personalexpensetracker.ui.screens.settings.SettingsScreen
import com.personalexpensetracker.ui.screens.settings.SettingsViewModel
import com.personalexpensetracker.ui.util.LocalUserPreferences
import com.personalexpensetracker.ui.components.FinlyBottomNavigation
import com.personalexpensetracker.ui.components.FinlyNavigationDestination
import com.personalexpensetracker.ui.components.FinlySegmentedControl
import com.personalexpensetracker.ui.screens.plan.PlanScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import com.personalexpensetracker.ui.theme.ExpenseTrackerTheme
import com.personalexpensetracker.ui.theme.FinlyPurple
import com.personalexpensetracker.ui.theme.StatusSuccess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val expenseRepository = ExpenseRepositoryImpl(database.expenseDao())
        val addExpenseUseCase = AddExpenseUseCase(expenseRepository)
        val getExpensesUseCase = GetExpensesUseCase(expenseRepository)
        val updateExpenseUseCase = UpdateExpenseUseCase(expenseRepository)
        val deleteExpenseUseCase = DeleteExpenseUseCase(expenseRepository)

        val recurringRepository = RecurringExpenseRepositoryImpl(database.recurringExpenseDao())
        val createRecurringExpenseUseCase = CreateRecurringExpenseUseCase(recurringRepository)
        val getRecurringExpenseByIdUseCase = GetRecurringExpenseByIdUseCase(recurringRepository)
        val updateRecurringExpenseUseCase = UpdateRecurringExpenseUseCase(recurringRepository)

        val incomeRepository = IncomeRepositoryImpl(database.incomeDao())
        val addIncomeUseCase = AddIncomeUseCase(incomeRepository)
        val getIncomeByIdUseCase = GetIncomeByIdUseCase(incomeRepository)
        val updateIncomeUseCase = UpdateIncomeUseCase(incomeRepository)
        val getIncomesUseCase = GetIncomesUseCase(incomeRepository)
        val deleteIncomeUseCase = DeleteIncomeUseCase(incomeRepository)
        val getFinancialBalanceUseCase = GetFinancialBalanceUseCase(expenseRepository, incomeRepository)
        val getCashFlowReportUseCase = GetCashFlowReportUseCase(expenseRepository, incomeRepository)

        val addExpenseViewModel: AddExpenseViewModel = ViewModelProvider(
            this,
            AddExpenseViewModel.Factory(addExpenseUseCase)
        )[AddExpenseViewModel::class.java]

        val expenseListViewModel: ExpenseListViewModel = ViewModelProvider(
            this,
            ExpenseListViewModel.Factory(getExpensesUseCase, deleteExpenseUseCase)
        )[ExpenseListViewModel::class.java]

        val editExpenseViewModel: EditExpenseViewModel = ViewModelProvider(
            this,
            EditExpenseViewModel.Factory(updateExpenseUseCase)
        )[EditExpenseViewModel::class.java]

        val dashboardViewModel: DashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModel.Factory(getExpensesUseCase, getFinancialBalanceUseCase)
        )[DashboardViewModel::class.java]

        val addRecurringExpenseViewModel: AddRecurringExpenseViewModel = ViewModelProvider(
            this,
            AddRecurringExpenseViewModel.Factory(createRecurringExpenseUseCase)
        )[AddRecurringExpenseViewModel::class.java]

        val editRecurringExpenseViewModel: EditRecurringExpenseViewModel = ViewModelProvider(
            this,
            EditRecurringExpenseViewModel.Factory(
                getRecurringExpenseByIdUseCase,
                updateRecurringExpenseUseCase
            )
        )[EditRecurringExpenseViewModel::class.java]

        val addIncomeViewModel: AddIncomeViewModel = ViewModelProvider(
            this,
            AddIncomeViewModel.Factory(addIncomeUseCase)
        )[AddIncomeViewModel::class.java]

        val editIncomeViewModel: EditIncomeViewModel = ViewModelProvider(
            this,
            EditIncomeViewModel.Factory(
                getIncomeByIdUseCase,
                updateIncomeUseCase
            )
        )[EditIncomeViewModel::class.java]

        val incomeListViewModel: IncomeListViewModel = ViewModelProvider(
            this,
            IncomeListViewModelFactory(getIncomesUseCase, deleteIncomeUseCase)
        )[IncomeListViewModel::class.java]

        val cashFlowAnalyticsViewModel: CashFlowAnalyticsViewModel = ViewModelProvider(
            this,
            CashFlowAnalyticsViewModel.Factory(getCashFlowReportUseCase)
        )[CashFlowAnalyticsViewModel::class.java]

        val dataManagementRepository = DataManagementRepositoryImpl(database)
        val dataManagementViewModel: DataManagementViewModel = ViewModelProvider(
            this,
            DataManagementViewModel.Factory(dataManagementRepository)
        )[DataManagementViewModel::class.java]

        val userPreferences = UserPreferencesImpl(applicationContext)
        val settingsViewModel: SettingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.Factory(userPreferences)
        )[SettingsViewModel::class.java]

        setContent {
            val preferences by userPreferences.preferencesFlow.collectAsState(initial = userPreferences.getPreferences())

            CompositionLocalProvider(LocalUserPreferences provides preferences) {
                ExpenseTrackerTheme(themeMode = preferences.themeMode) {
                    ExpenseTrackerAppShell(
                        dashboardViewModel = dashboardViewModel,
                        addExpenseViewModel = addExpenseViewModel,
                        expenseListViewModel = expenseListViewModel,
                        editExpenseViewModel = editExpenseViewModel,
                        addRecurringExpenseViewModel = addRecurringExpenseViewModel,
                        editRecurringExpenseViewModel = editRecurringExpenseViewModel,
                        addIncomeViewModel = addIncomeViewModel,
                        editIncomeViewModel = editIncomeViewModel,
                        incomeListViewModel = incomeListViewModel,
                        cashFlowAnalyticsViewModel = cashFlowAnalyticsViewModel,
                        dataManagementViewModel = dataManagementViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerAppShell(
    dashboardViewModel: DashboardViewModel,
    addExpenseViewModel: AddExpenseViewModel,
    expenseListViewModel: ExpenseListViewModel,
    editExpenseViewModel: EditExpenseViewModel,
    addRecurringExpenseViewModel: AddRecurringExpenseViewModel,
    editRecurringExpenseViewModel: EditRecurringExpenseViewModel,
    addIncomeViewModel: AddIncomeViewModel,
    editIncomeViewModel: EditIncomeViewModel,
    incomeListViewModel: IncomeListViewModel,
    cashFlowAnalyticsViewModel: CashFlowAnalyticsViewModel,
    dataManagementViewModel: DataManagementViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentDestination by remember { mutableStateOf(FinlyNavigationDestination.HOME) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var editingRecurringExpense by remember { mutableStateOf<RecurringExpense?>(null) }
    var editingIncome by remember { mutableStateOf<Income?>(null) }
    var showDataManagement by remember { mutableStateOf(false) }
    var showAddTransaction by remember { mutableStateOf(false) }
    var showAllTransactions by remember { mutableStateOf(false) }
    var addTabType by remember { mutableIntStateOf(0) } // 0 = Expense, 1 = Income
    var recordsTabType by remember { mutableIntStateOf(0) } // 0 = Expense, 1 = Income

    val context = LocalContext.current
    val isNotificationAccessGranted = androidx.core.app.NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)

    var showNotificationOnboardingDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (!isNotificationAccessGranted) {
            showNotificationOnboardingDialog = true
        }
    }

    if (showNotificationOnboardingDialog && !isNotificationAccessGranted) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNotificationOnboardingDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = FinlyPurple,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Automatic Expense Capture",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Finly automatically records your bank and UPI expenses directly from incoming receipts without needing risky SMS permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• 100% Local & Private (data never leaves your phone)\n• Only bank & payment notifications are analyzed\n• Personal chats and OTPs are completely ignored",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showNotificationOnboardingDialog = false
                        settingsViewModel.toggleAutoCaptureExpenses(true)
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val fallback = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                            fallback.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(fallback)
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FinlyPurple)
                ) {
                    Text("Enable Access", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showNotificationOnboardingDialog = false }
                ) {
                    Text("Maybe Later")
                }
            }
        )
    }

    // 1. Full-screen Edit Overlays
    if (editingExpense != null) {
        EditExpenseScreen(
            viewModel = editExpenseViewModel,
            onNavigateBack = { editingExpense = null }
        )
    } else if (editingRecurringExpense != null) {
        EditRecurringExpenseScreen(
            viewModel = editRecurringExpenseViewModel,
            onNavigateBack = { editingRecurringExpense = null }
        )
    } else if (editingIncome != null) {
        EditIncomeScreen(
            viewModel = editIncomeViewModel,
            onNavigateBack = { editingIncome = null }
        )
    } else if (showDataManagement) {
        DataManagementScreen(
            viewModel = dataManagementViewModel,
            onNavigateBack = { showDataManagement = false }
        )
    } else if (showAddTransaction) {
        // Add Transaction Modal / Full View with Segmented Toggle
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showAddTransaction = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (addTabType == 0) "Add Expense" else "Add Income",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FinlySegmentedControl(
                        items = listOf("Expense", "Income"),
                        selectedIndex = addTabType,
                        onItemSelected = { addTabType = it }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (addTabType == 0) {
                    AddExpenseScreen(
                        viewModel = addExpenseViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AddIncomeScreen(
                        viewModel = addIncomeViewModel,
                        onEditIncome = { income ->
                            editIncomeViewModel.setIncome(income)
                            editingIncome = income
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    } else if (showAllTransactions) {
        // View All Transactions (Expenses & Incomes)
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showAllTransactions = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "All Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FinlySegmentedControl(
                        items = listOf("Expenses", "Income"),
                        selectedIndex = recordsTabType,
                        onItemSelected = { recordsTabType = it }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (recordsTabType == 0) {
                    ExpenseListScreen(
                        viewModel = expenseListViewModel,
                        onExpenseClick = { expense ->
                            editExpenseViewModel.setExpense(expense)
                            editingExpense = expense
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    IncomeListScreen(
                        viewModel = incomeListViewModel,
                        onIncomeClick = { income ->
                            editIncomeViewModel.setIncome(income)
                            editingIncome = income
                        },
                        onNavigateToAddIncome = {
                            showAddTransaction = true
                            addTabType = 1
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    } else {
        // Main App Scaffold with Finly Bottom Navigation
        Scaffold(
            bottomBar = {
                FinlyBottomNavigation(
                    selectedDestination = currentDestination,
                    onDestinationSelected = { currentDestination = it },
                    onAddClick = { showAddTransaction = true }
                )
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentDestination) {
                    FinlyNavigationDestination.HOME -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onExpenseClick = { expense ->
                                editExpenseViewModel.setExpense(expense)
                                editingExpense = expense
                            },
                            onNavigateToAddExpense = {
                                showAddTransaction = true
                                addTabType = 0
                            },
                            onCashFlowClick = {
                                currentDestination = FinlyNavigationDestination.REPORTS
                            },
                            onViewAllClick = {
                                showAllTransactions = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    FinlyNavigationDestination.REPORTS -> {
                        CashFlowAnalyticsScreen(
                            viewModel = cashFlowAnalyticsViewModel,
                            onNavigateBack = {
                                currentDestination = FinlyNavigationDestination.HOME
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    FinlyNavigationDestination.PLAN -> {
                        PlanScreen(
                            recurringViewModel = addRecurringExpenseViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    FinlyNavigationDestination.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = {
                                currentDestination = FinlyNavigationDestination.HOME
                            },
                            onNavigateToDataManagement = {
                                showDataManagement = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
