package com.example.mybillsapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybillsapp.ui.MainViewModel
import com.example.mybillsapp.ui.NavTab
import com.example.mybillsapp.ui.components.AddEditBankDialog
import com.example.mybillsapp.ui.components.HelpWelcomeDialog
import com.example.mybillsapp.ui.screens.AnalyticsScreen
import com.example.mybillsapp.ui.screens.FutureScreen
import com.example.mybillsapp.ui.screens.HomeScreen
import com.example.mybillsapp.ui.screens.SettingsScreen
import com.example.mybillsapp.ui.theme.MyBillsAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyBillsAppTheme {
                AppMainContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainContent(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentTab by viewModel.currentNavTab.collectAsState()
    val banks by viewModel.banks.collectAsState()
    val isPrivacy by viewModel.isPrivacyMode.collectAsState()
    val currentMonthYear by viewModel.currentMonthYear.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val showWelcomeDialog by viewModel.showWelcomeDialog.collectAsState()

    var showAddBankDialog by remember { mutableStateOf(false) }
    var isNotificationEnabled by remember { mutableStateOf(false) }

    // Observa o ciclo de vida para recarregar dados e checar permissões ao voltar pro App
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Checa se o listener de notificação está ativado nas configs do sistema
                isNotificationEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                // Recarrega o JSON para caso o serviço em background tenha inserido dados novos
                viewModel.reloadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle Back Button: Return to HOME tab if on another screen
    BackHandler(enabled = currentTab != NavTab.HOME) {
        viewModel.navigateTo(NavTab.HOME)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentTab) {
                                NavTab.HOME -> "Contas a Pagar"
                                NavTab.FUTURE -> "Parcelado Futuro"
                                NavTab.ANALYTICS -> "Análises e Gráficos"
                                NavTab.SETTINGS -> "Configurações"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (!isNotificationEnabled) {
                        IconButton(onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "Habilitar Leitura de Notificações",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.togglePrivacy() }) {
                        Icon(
                            imageVector = if (isPrivacy) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ocultar Valores",
                            tint = if (isPrivacy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.HOME,
                    onClick = { viewModel.navigateTo(NavTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.HOME) Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Início"
                        )
                    },
                    label = { Text("Início") }
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.FUTURE,
                    onClick = { viewModel.navigateTo(NavTab.FUTURE) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.FUTURE) Icons.Default.DateRange else Icons.Outlined.DateRange,
                            contentDescription = "Futuro"
                        )
                    },
                    label = { Text("Futuro") }
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.ANALYTICS,
                    onClick = { viewModel.navigateTo(NavTab.ANALYTICS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.ANALYTICS) Icons.Default.PieChart else Icons.Outlined.PieChart,
                            contentDescription = "Análises"
                        )
                    },
                    label = { Text("Análises") }
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.SETTINGS,
                    onClick = { viewModel.navigateTo(NavTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == NavTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Configurações"
                        )
                    },
                    label = { Text("Configurações") }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == NavTab.HOME) {
                ExtendedFloatingActionButton(
                    onClick = { showAddBankDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar Banco") },
                    text = { Text("Novo Banco", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                label = "tabTransition"
            ) { targetTab ->
                when (targetTab) {
                    NavTab.HOME -> HomeScreen(
                        viewModel = viewModel,
                        banks = banks,
                        isPrivacy = isPrivacy,
                        currentMonthYear = currentMonthYear,
                        searchQuery = searchQuery,
                        statusFilter = statusFilter,
                        categoryFilter = categoryFilter
                    )
                    NavTab.FUTURE -> FutureScreen(
                        banks = banks,
                        isPrivacy = isPrivacy
                    )
                    NavTab.ANALYTICS -> AnalyticsScreen(
                        viewModel = viewModel,
                        banks = banks,
                        isPrivacy = isPrivacy,
                        currentMonthYear = currentMonthYear
                    )
                    NavTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        isPrivacy = isPrivacy
                    )
                }
            }
        }
    }

    if (showAddBankDialog) {
        AddEditBankDialog(
            onDismiss = { showAddBankDialog = false },
            onConfirm = { name, hexColor, presetKey ->
                viewModel.addBank(name, hexColor, presetKey)
                showAddBankDialog = false
            }
        )
    }

    if (showWelcomeDialog) {
        HelpWelcomeDialog(
            isFirstLaunch = true,
            onDismiss = { viewModel.dismissWelcomeDialog() }
        )
    }
}
