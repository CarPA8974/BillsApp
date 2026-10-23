package com.example.mybillsapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BillCategory
import com.example.mybillsapp.model.BillItem
import com.example.mybillsapp.model.MonthYear
import com.example.mybillsapp.toCurrency
import com.example.mybillsapp.ui.MainViewModel
import com.example.mybillsapp.ui.components.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    banks: List<BankEntity>,
    isPrivacy: Boolean,
    currentMonthYear: MonthYear,
    searchQuery: String,
    statusFilter: StatusFilter,
    categoryFilter: BillCategory?
) {
    var showAddBankDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<BankEntity?>(null) }
    var bankToDelete by remember { mutableStateOf<BankEntity?>(null) }
    
    // Add/Edit Bill State
    var selectedBankForAddBill by remember { mutableStateOf<String?>(null) }
    var editingBillPair by remember { mutableStateOf<Pair<String, BillItem>?>(null) }

    // Bill Action States
    var billToTogglePair by remember { mutableStateOf<Pair<String, BillItem>?>(null) }
    var billToDeletePair by remember { mutableStateOf<Pair<String, BillItem>?>(null) } // Pair(bankId, billId)
    var billToMovePair by remember { mutableStateOf<Pair<String, BillItem>?>(null) }
    var selectedBankIdForDetails by remember { mutableStateOf<String?>(null) }

    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    var showBudgetWarningDialog by rememberSaveable { mutableStateOf(false) }
    var hasShownBudgetWarning by rememberSaveable { mutableStateOf(false) }

    // Computations
    val allItemsForMonth = banks.flatMap { it.itemsForMonth(currentMonthYear.month, currentMonthYear.year) }
    val totalMonthAmount = banks.sumOf { it.subtotalMonth(currentMonthYear.month, currentMonthYear.year) }
    val totalPaidAmount = banks.sumOf { it.subtotalMonthPaid(currentMonthYear.month, currentMonthYear.year) }
    val totalPendingAmount = banks.sumOf { it.subtotalMonthPending(currentMonthYear.month, currentMonthYear.year) }

    val activeOrDisplayedBanks = banks.filter { !it.isArchived || it.itemsForMonth(currentMonthYear.month, currentMonthYear.year).isNotEmpty() }
    val totalCreditLimit = activeOrDisplayedBanks.sumOf { it.creditLimit }
    val totalAvailableLimit = activeOrDisplayedBanks.sumOf { it.availableLimit }

    val isCurrentMonth = currentMonthYear == MonthYear.current()

    val overdueCount = if (isCurrentMonth) allItemsForMonth.count { it.isOverdue() } else 0

    LaunchedEffect(totalMonthAmount, monthlyBudget) {
        if (!hasShownBudgetWarning && monthlyBudget > 0.0 && totalMonthAmount > monthlyBudget) {
            showBudgetWarningDialog = true
            hasShownBudgetWarning = true
        }
    }

    // Filter banks & bills for the active month/year (Hide archived banks if they have no active items in the selected month)
    val filteredBanks = remember(banks, currentMonthYear, searchQuery, statusFilter, categoryFilter) {
        banks.mapNotNull { bank ->
            val monthItems = bank.itemsForMonth(currentMonthYear.month, currentMonthYear.year)
            val filteredItems = monthItems.filter { item ->
                val matchesSearch = searchQuery.isBlank() ||
                        item.description.contains(searchQuery, ignoreCase = true) ||
                        bank.name.contains(searchQuery, ignoreCase = true) ||
                        item.notes.contains(searchQuery, ignoreCase = true)

                val isOverdue = item.isOverdue()
                
                val matchesStatus = when (statusFilter) {
                    StatusFilter.ALL -> true
                    StatusFilter.PENDING -> !item.isPaid
                    StatusFilter.PAID -> item.isPaid
                    StatusFilter.OVERDUE -> isOverdue
                }

                val matchesCategory = categoryFilter == null || item.category == categoryFilter

                matchesSearch && matchesStatus && matchesCategory
            }

            val hasActiveFilter = statusFilter != StatusFilter.ALL || categoryFilter != null || searchQuery.isNotBlank()

            // Bancos ativos NUNCA somem sem filtro. Bancos arquivados só aparecem se tiverem despesas no mês.
            val isVisible = if (bank.isArchived) {
                filteredItems.isNotEmpty()
            } else {
                if (hasActiveFilter) {
                    filteredItems.isNotEmpty()
                } else {
                    true
                }
            }
            
            if (isVisible) {
                Pair(bank, filteredItems)
            } else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Month Selector Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
                }

                Text(
                    text = currentMonthYear.formatDisplay(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row {
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Stats Header
            item {
                StatsHeaderCard(
                    title = "Resumo - ${currentMonthYear.formatDisplay()}",
                    totalAmount = totalMonthAmount,
                    paidAmount = totalPaidAmount,
                    pendingAmount = totalPendingAmount,
                    overdueCount = overdueCount,
                    totalCreditLimit = totalCreditLimit,
                    totalAvailableLimit = totalAvailableLimit,
                    isPrivacy = isPrivacy
                )
            }

            // Search & Filter Bar
            item {
                SearchFilterBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedStatus = statusFilter,
                    onStatusSelected = { viewModel.setStatusFilter(it) },
                    selectedCategory = categoryFilter,
                    onCategorySelected = { viewModel.setCategoryFilter(it) }
                )
            }

            // Bank Cards List
            if (filteredBanks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (banks.isEmpty()) "Nenhum banco ou cartão cadastrado!"
                                else "Nenhuma despesa encontrada com estes filtros.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (banks.isEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { showAddBankDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cadastrar Primeiro Banco")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredBanks, key = { it.first.id }) { (bank, displayedItems) ->
                    BankCard(
                        bank = bank,
                        displayedItems = displayedItems,
                        isPrivacy = isPrivacy,
                        showFuture = false,
                        onAddBill = { selectedBankForAddBill = bank.id },
                        onEditBank = { editingBank = bank },
                        onRemoveBank = { bankToDelete = bank },
                        onToggleBill = { billId ->
                            val bill = bank.items.find { it.id == billId }
                            if (bill != null && bill.totalInstallments > 1) {
                                billToTogglePair = Pair(bank.id, bill)
                            } else {
                                viewModel.toggleBillStatus(bank.id, billId)
                            }
                        },
                        onEditBill = { item -> editingBillPair = Pair(bank.id, item) },
                        onRemoveBill = { billId ->
                            val bill = bank.items.find { it.id == billId }
                            if (bill != null && bill.totalInstallments > 1) {
                                billToDeletePair = Pair(bank.id, bill)
                            } else {
                                viewModel.removeBill(bank.id, billId)
                            }
                        },
                        onMoveBillNextMonth = { billId -> 
                            val bill = bank.items.find { it.id == billId }
                            if (bill != null) billToMovePair = Pair(bank.id, bill) 
                        },
                        onPayAllBills = { viewModel.payAllMonthBills(bank.id) },
                        onBankClick = { selectedBankIdForDetails = bank.id }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // Add Bank
    if (showAddBankDialog) {
        AddEditBankDialog(
            onDismiss = { showAddBankDialog = false },
            onConfirm = { name, hexColor, presetKey, defaultDueDay, closingDay, creditLimit ->
                viewModel.addBank(name, hexColor, presetKey, defaultDueDay, closingDay, creditLimit)
                showAddBankDialog = false
            }
        )
    }

    // Edit Bank
    editingBank?.let { bank ->
        AddEditBankDialog(
            initialBank = bank,
            onDismiss = { editingBank = null },
            onConfirm = { name, hexColor, presetKey, defaultDueDay, closingDay, creditLimit ->
                viewModel.editBank(bank.id, name, hexColor, presetKey, defaultDueDay, closingDay, creditLimit)
                editingBank = null
            }
        )
    }

    // Delete Bank
    bankToDelete?.let { bank ->
        ConfirmDeleteDialog(
            title = "Excluir Instituição",
            message = "Tem certeza que deseja excluir ${bank.name} e todas as suas despesas?",
            onDismiss = { bankToDelete = null },
            onConfirm = {
                viewModel.removeBank(bank.id)
                bankToDelete = null
            }
        )
    }

    // Add Bill
    selectedBankForAddBill?.let { bankId ->
        val bank = banks.find { it.id == bankId }
        val defaultDue = bank?.defaultDueDay ?: 10
        AddEditBillDialog(
            defaultDueDay = defaultDue,
            onDismiss = { selectedBankForAddBill = null },
            onConfirm = { desc, monthly, total, installments, current, dueDay, cat, notes ->
                viewModel.addBill(
                    bankId = bankId,
                    description = desc,
                    monthlyAmount = monthly,
                    totalAmount = total,
                    installments = installments,
                    currentInstallment = current,
                    dueDay = dueDay,
                    category = cat,
                    notes = notes
                )
                selectedBankForAddBill = null
            }
        )
    }

    // Edit Bill
    editingBillPair?.let { (bankId, billItem) ->
        AddEditBillDialog(
            initialItem = billItem,
            onDismiss = { editingBillPair = null },
            onConfirm = { desc, monthly, total, installments, current, dueDay, cat, notes ->
                viewModel.editBill(
                    bankId = bankId,
                    billId = billItem.id,
                    description = desc,
                    monthlyAmount = monthly,
                    totalAmount = total,
                    installments = installments,
                    currentInstallment = current,
                    dueDay = dueDay,
                    category = cat,
                    notes = notes
                )
                editingBillPair = null
            }
        )
    }

    // Delete Bill Confirmation (Multi-installment handling)
    billToDeletePair?.let { (bankId, billItem) ->
        ConfirmDeleteDialog(
            title = "Excluir Despesa",
            message = "Esta é uma despesa parcelada (${billItem.currentInstallment}/${billItem.totalInstallments}x). O que deseja excluir?",
            confirmText = "Somente Esta",
            dismissText = "Cancelar",
            onDismiss = { billToDeletePair = null },
            onConfirm = {
                viewModel.removeBill(bankId, billItem.id)
                billToDeletePair = null
            },
            extraButtonText = "Excluir Todas (Série)",
            onExtraAction = {
                viewModel.removeBillSeries(bankId, billItem.groupId)
                billToDeletePair = null
            }
        )
    }

    // Toggle Paid Bill Confirmation (Multi-installment handling)
    billToTogglePair?.let { (bankId, billItem) ->
        val isPaying = !billItem.isPaid
        ConfirmDeleteDialog(
            title = if (isPaying) "Pagar Parcelas" else "Desmarcar Parcelas",
            message = "Você está prestes a ${if (isPaying) "pagar" else "desmarcar"} uma conta parcelada. Deseja aplicar isso apenas a esta parcela ou a todas as seguintes?",
            confirmText = "Somente Esta",
            dismissText = "Cancelar",
            onDismiss = { billToTogglePair = null },
            onConfirm = {
                viewModel.toggleBillStatus(bankId, billItem.id)
                billToTogglePair = null
            },
            extraButtonText = "Aplicar a Todas Futuras",
            onExtraAction = {
                if (isPaying) {
                    viewModel.payBillSeries(bankId, billItem.groupId, billItem.currentInstallment)
                } else {
                    viewModel.toggleBillStatus(bankId, billItem.id) // Simplificando para desmarcar só essa caso precise reverter
                }
                billToTogglePair = null
            }
        )
    }

    // Move Bill Confirmation
    billToMovePair?.let { (bankId, billItem) ->
        AlertDialog(
            onDismissRequest = { billToMovePair = null },
            title = { Text("Mover Despesa", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente mover '${billItem.description}' para o mês seguinte?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveBillToNextMonth(bankId, billItem.id)
                        billToMovePair = null
                    }
                ) {
                    Text("Mover")
                }
            },
            dismissButton = {
                TextButton(onClick = { billToMovePair = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Budget Warning
    if (showBudgetWarningDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetWarningDialog = false },
            title = { Text("Orçamento Excedido!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { 
                Text("Você ultrapassou o seu limite de gastos mensal definido de ${monthlyBudget.toCurrency()}.\nTotal Gasto no Mês: ${totalMonthAmount.toCurrency()}") 
            },
            confirmButton = {
                Button(onClick = { showBudgetWarningDialog = false }) { Text("Entendi") }
            }
        )
    }

    // Bank Details Full Screen Dialog
    selectedBankIdForDetails?.let { bankId ->
        val detailsPair = filteredBanks.find { it.first.id == bankId }
        if (detailsPair != null) {
            BankDetailsDialog(
                bank = detailsPair.first,
                displayedItems = detailsPair.second,
                isPrivacy = isPrivacy,
                showFuture = false,
                onDismiss = { selectedBankIdForDetails = null },
                onAddBill = { selectedBankForAddBill = bankId },
                onEditBank = { editingBank = detailsPair.first },
                onRemoveBank = { bankToDelete = detailsPair.first },
                onToggleBill = { billId ->
                    val bill = detailsPair.first.items.find { it.id == billId }
                    if (bill != null && bill.totalInstallments > 1) {
                        billToTogglePair = Pair(bankId, bill)
                    } else {
                        viewModel.toggleBillStatus(bankId, billId)
                    }
                },
                onEditBill = { item -> editingBillPair = Pair(bankId, item) },
                onRemoveBill = { billId ->
                    val bill = detailsPair.first.items.find { it.id == billId }
                    if (bill != null && bill.totalInstallments > 1) {
                        billToDeletePair = Pair(bankId, bill)
                    } else {
                        viewModel.removeBill(bankId, billId)
                    }
                },
                onMoveBillNextMonth = { billId -> 
                    val bill = detailsPair.first.items.find { it.id == billId }
                    if (bill != null) billToMovePair = Pair(bankId, bill) 
                },
                onPayAllBills = { viewModel.payAllMonthBills(bankId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDetailsDialog(
    bank: BankEntity,
    displayedItems: List<BillItem>,
    isPrivacy: Boolean,
    showFuture: Boolean,
    onDismiss: () -> Unit,
    onAddBill: () -> Unit,
    onEditBank: () -> Unit,
    onRemoveBank: () -> Unit,
    onToggleBill: (String) -> Unit,
    onEditBill: (BillItem) -> Unit,
    onRemoveBill: (String) -> Unit,
    onMoveBillNextMonth: (String) -> Unit,
    onPayAllBills: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(bank.name) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = onEditBank) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                        IconButton(onClick = onRemoveBank) { Icon(Icons.Default.Delete, contentDescription = "Excluir") }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onAddBill,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nova Despesa") }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                // Info Cards
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Vencimento: Dia ${bank.defaultDueDay} | Fechamento: Dia ${bank.closingDay}", style = MaterialTheme.typography.bodyMedium)
                        if (bank.pixKey.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Chave Pix: ${bank.pixKey}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        if (bank.creditLimit > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Limite Livre: ${if (isPrivacy) "****" else bank.availableLimit.toCurrency()} / ${if (isPrivacy) "****" else bank.creditLimit.toCurrency()} total",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Despesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val hasPending = displayedItems.any { !it.isPaid }
                    if (hasPending && !showFuture) {
                        TextButton(onClick = onPayAllBills) {
                            Text("Pagar Tudo", color = Color(0xFF00E676))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (displayedItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Nenhuma despesa para exibir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp), 
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(displayedItems, key = { it.id }) { item ->
                            com.example.mybillsapp.ui.components.BillListItem(
                                item = item,
                                isPrivacy = isPrivacy,
                                showFuture = showFuture,
                                onTogglePaid = { onToggleBill(item.id) },
                                onEdit = { onEditBill(item) },
                                onDelete = { onRemoveBill(item.id) },
                                onMoveToNextMonth = { onMoveBillNextMonth(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
