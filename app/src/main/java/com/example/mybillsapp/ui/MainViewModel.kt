package com.example.mybillsapp.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.mybillsapp.data.BillsRepository
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BillCategory
import com.example.mybillsapp.model.BillItem
import com.example.mybillsapp.model.MonthYear
import com.example.mybillsapp.ui.components.StatusFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavTab(val label: String) {
    HOME("Início"),
    FUTURE("Futuro"),
    ANALYTICS("Análises"),
    SETTINGS("Configurações")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BillsRepository(application)
    private val prefs = application.getSharedPreferences("mybills_prefs", Context.MODE_PRIVATE)

    private val _showWelcomeDialog = MutableStateFlow(false)
    val showWelcomeDialog = _showWelcomeDialog.asStateFlow()

    private val _banks = MutableStateFlow<List<BankEntity>>(emptyList())
    val banks = _banks.asStateFlow()

    private val _isPrivacyMode = MutableStateFlow(false)
    val isPrivacyMode = _isPrivacyMode.asStateFlow()

    private val _currentNavTab = MutableStateFlow(NavTab.HOME)
    val currentNavTab = _currentNavTab.asStateFlow()

    private val _currentMonthYear = MutableStateFlow(MonthYear.current().nextMonth())
    val currentMonthYear = _currentMonthYear.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter = _statusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<BillCategory?>(null)
    val categoryFilter = _categoryFilter.asStateFlow()

    init {
        _banks.value = repository.loadData()
        if (prefs.getBoolean("is_first_access_v1", true)) {
            _showWelcomeDialog.value = true
        }
    }

    fun dismissWelcomeDialog() {
        prefs.edit().putBoolean("is_first_access_v1", false).apply()
        _showWelcomeDialog.value = false
    }

    fun navigateTo(tab: NavTab) { _currentNavTab.value = tab }
    fun togglePrivacy() { _isPrivacyMode.value = !_isPrivacyMode.value }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setStatusFilter(filter: StatusFilter) { _statusFilter.value = filter }
    fun setCategoryFilter(category: BillCategory?) { _categoryFilter.value = category }

    fun nextMonth() { _currentMonthYear.value = _currentMonthYear.value.nextMonth() }
    fun previousMonth() { _currentMonthYear.value = _currentMonthYear.value.previousMonth() }

    fun addBank(name: String, hexColor: String, presetKey: String) {
        val newBank = BankEntity(
            name = name,
            colorHex = hexColor,
            presetKey = presetKey
        )
        updateData(_banks.value + newBank)
    }

    fun editBank(bankId: String, name: String, hexColor: String, presetKey: String) {
        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                bank.copy(name = name, colorHex = hexColor, presetKey = presetKey)
            } else bank
        })
    }

    fun removeBank(bankId: String) {
        updateData(_banks.value.filter { it.id != bankId })
    }

    fun addBill(
        bankId: String,
        description: String,
        monthlyAmount: Double,
        totalAmount: Double,
        installments: Int,
        currentInstallment: Int,
        dueDay: Int,
        category: BillCategory,
        notes: String
    ) {
        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                val newItem = BillItem(
                    description = description,
                    monthlyAmount = monthlyAmount,
                    totalAmount = totalAmount,
                    totalInstallments = installments,
                    currentInstallment = currentInstallment,
                    dueDay = dueDay,
                    category = category,
                    notes = notes,
                    isPaid = false
                )
                bank.copy(items = bank.items + newItem)
            } else bank
        })
    }

    fun editBill(
        bankId: String,
        billId: String,
        description: String,
        monthlyAmount: Double,
        totalAmount: Double,
        installments: Int,
        currentInstallment: Int,
        dueDay: Int,
        category: BillCategory,
        notes: String
    ) {
        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                bank.copy(items = bank.items.map { item ->
                    if (item.id == billId) {
                        item.copy(
                            description = description,
                            monthlyAmount = monthlyAmount,
                            totalAmount = totalAmount,
                            totalInstallments = installments,
                            currentInstallment = currentInstallment,
                            dueDay = dueDay,
                            category = category,
                            notes = notes
                        )
                    } else item
                })
            } else bank
        })
    }

    fun removeBill(bankId: String, billId: String) {
        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                bank.copy(items = bank.items.filter { it.id != billId })
            } else bank
        })
    }

    fun toggleBillStatus(bankId: String, billId: String) {
        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                bank.copy(items = bank.items.map { item ->
                    if (item.id == billId) item.copy(isPaid = !item.isPaid) else item
                })
            } else bank
        })
    }

    fun advanceMonthCycle() {
        // Unchecks all paid bills and advances installment counts for parcelled items
        val updated = _banks.value.map { bank ->
            val updatedItems = bank.items.mapNotNull { item ->
                if (item.totalInstallments > 1) {
                    if (item.currentInstallment >= item.totalInstallments) {
                        // Finished paying all installments, remove
                        null
                    } else {
                        item.copy(
                            currentInstallment = item.currentInstallment + 1,
                            isPaid = false
                        )
                    }
                } else {
                    // Regular recurring monthly bill, reset paid status
                    item.copy(isPaid = false)
                }
            }
            bank.copy(items = updatedItems)
        }
        updateData(updated)
    }

    fun reloadData() {
        _banks.value = repository.loadData()
    }

    fun clearAllData() {
        updateData(emptyList())
    }

    fun exportDataJson(): String {
        return repository.exportJson(_banks.value)
    }

    fun importDataJson(jsonString: String): Boolean {
        val imported = repository.importJson(jsonString)
        return if (imported != null) {
            updateData(imported)
            true
        } else false
    }

    private fun updateData(newList: List<BankEntity>) {
        _banks.value = newList
        repository.saveData(newList)
    }
}
