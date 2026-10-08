package com.example.mybillsapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BillCategory
import com.example.mybillsapp.model.BillItem
import com.example.mybillsapp.model.MonthYear
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

    var showAdvanceCycleDialog by remember { mutableStateOf(false) }

    // Computations
    val allItemsForMonth = banks.flatMap { it.itemsForMonth(currentMonthYear.month, currentMonthYear.year) }
    val totalMonthAmount = banks.sumOf { it.subtotalMonth(currentMonthYear.month, currentMonthYear.year) }
    val totalPaidAmount = banks.sumOf { it.subtotalMonthPaid(currentMonthYear.month, currentMonthYear.year) }
    val totalPendingAmount = banks.sumOf { it.subtotalMonthPending(currentMonthYear.month, currentMonthYear.year) }

    val activeBanks = banks.filter { !it.isArchived }
    val totalCreditLimit = activeBanks.sumOf { it.creditLimit }
    val totalAvailableLimit = activeBanks.sumOf { it.availableLimit }

    val isCurrentMonth = currentMonthYear == MonthYear.current()

    val overdueCount = if (isCurrentMonth) allItemsForMonth.count { it.isOverdue() } else 0

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

            // Only show archived banks IF they have bills in this specific month
            val isVisible = if (bank.isArchived) filteredItems.isNotEmpty() else (filteredItems.isNotEmpty() || searchQuery.isBlank())
            
            if (isVisible) {
                bank.copy(items = filteredItems)
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
                    IconButton(onClick = { showAdvanceCycleDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "Virar Mês",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                items(filteredBanks, key = { it.id }) { bank ->
                    BankCard(
                        bank = bank,
                        isPrivacy = isPrivacy,
                        showFuture = false,
                        onAddBill = { selectedBankForAddBill = bank.id },
                        onEditBank = { editingBank = bank },
                        onRemoveBank = {
                            if (bank.items.isEmpty()) {
                                viewModel.removeBank(bank.id)
                            } else {
                                bankToDelete = bank
                            }
                        },
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
                        onMoveBillNextMonth = { billId -> viewModel.moveBillToNextMonth(bank.id, billId) },
                        onPayAllBills = { viewModel.payAllMonthBills(bank.id) }
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

    // Advance Month Cycle Dialog
    if (showAdvanceCycleDialog) {
        AlertDialog(
            onDismissRequest = { showAdvanceCycleDialog = false },
            title = { Text("Virar Mês / Avançar Faturas", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Esta ação irá desmarcar os pagamentos das contas recorrentes e avançar +1 parcela das compras parceladas. Despesas com parcelas concluídas serão arquivadas. Deseja continuar?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.advanceMonthCycle()
                        showAdvanceCycleDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdvanceCycleDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
