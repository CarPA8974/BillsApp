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
import java.util.Locale
import java.util.UUID

enum class NavTab(val label: String) {
    HOME("Início"),
    FUTURE("Futuro"),
    ANALYTICS("Análises"),
    SETTINGS("Ajustes")
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

    private val _currentMonthYear = MutableStateFlow(MonthYear.current())
    val currentMonthYear = _currentMonthYear.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter = _statusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<BillCategory?>(null)
    val categoryFilter = _categoryFilter.asStateFlow()

    init {
        _banks.value = repository.loadData()
        repository.setOnDataChangeListener {
            reloadData()
        }

        val savedMonth = prefs.getInt("active_month", -1)
        val savedYear = prefs.getInt("active_year", -1)
        
        if (savedMonth != -1 && savedYear != -1) {
            _currentMonthYear.value = MonthYear(savedMonth, savedYear)
        } else {
            val defaultMonth = MonthYear.current().nextMonth()
            _currentMonthYear.value = defaultMonth
            saveActiveMonth(defaultMonth)
        }

        if (prefs.getBoolean("is_first_access_v1", true)) {
            _showWelcomeDialog.value = true
        }
    }

    private fun saveActiveMonth(my: MonthYear) {
        prefs.edit().putInt("active_month", my.month).putInt("active_year", my.year).apply()
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

    fun nextMonth() { 
        val next = _currentMonthYear.value.nextMonth()
        _currentMonthYear.value = next
        saveActiveMonth(next)
    }
    fun previousMonth() { 
        val prev = _currentMonthYear.value.previousMonth()
        _currentMonthYear.value = prev
        saveActiveMonth(prev)
    }

    fun addBank(name: String, hexColor: String, presetKey: String, defaultDueDay: Int, creditLimit: Double = 0.0) {
        val capName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val newBank = BankEntity(
            name = capName,
            colorHex = hexColor,
            presetKey = presetKey,
            defaultDueDay = defaultDueDay,
            creditLimit = creditLimit
        )
        updateData(_banks.value + newBank)
    }

    fun editBank(bankId: String, name: String, hexColor: String, presetKey: String, defaultDueDay: Int, creditLimit: Double = 0.0) {
        val capName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                bank.copy(name = capName, colorHex = hexColor, presetKey = presetKey, defaultDueDay = defaultDueDay, creditLimit = creditLimit)
            } else bank
        })
    }

    fun removeBank(bankId: String) {
        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                bank.copy(isArchived = true)
            } else bank
        })
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
        val activeMonth = _currentMonthYear.value
        val capDesc = description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                val newItem = BillItem(
                    description = capDesc,
                    monthlyAmount = monthlyAmount,
                    totalAmount = totalAmount,
                    totalInstallments = installments,
                    currentInstallment = currentInstallment,
                    dueDay = dueDay,
                    category = category,
                    notes = notes,
                    isPaid = false,
                    month = activeMonth.month,
                    year = activeMonth.year
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
        val capDesc = description.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        updateData(_banks.value.map { bank ->
            if (bank.id == bankId) {
                bank.copy(items = bank.items.map { item ->
                    if (item.id == billId) {
                        item.copy(
                            description = capDesc,
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
        val currentMonth = _currentMonthYear.value
        val nextMonth = currentMonth.nextMonth()

        val updatedBanks = _banks.value.map { bank ->
            val activeItems = bank.itemsForMonth(currentMonth.month, currentMonth.year)
            val newNextMonthItems = activeItems.mapNotNull { item ->
                if (item.totalInstallments > 1) {
                    if (item.currentInstallment >= item.totalInstallments) {
                        null // Finished paying all installments
                    } else {
                        item.copy(
                            id = UUID.randomUUID().toString(),
                            currentInstallment = item.currentInstallment + 1,
                            isPaid = false,
                            month = nextMonth.month,
                            year = nextMonth.year
                        )
                    }
                } else {
                    // Regular recurring monthly bill
                    item.copy(
                        id = UUID.randomUUID().toString(),
                        isPaid = false,
                        month = nextMonth.month,
                        year = nextMonth.year
                    )
                }
            }
            // Preserve all existing items (for history) and append new items for next month
            bank.copy(items = bank.items + newNextMonthItems)
        }

        updateData(updatedBanks)
        _currentMonthYear.value = nextMonth
        saveActiveMonth(nextMonth)
    }

    fun reloadData() {
        val loaded = repository.loadData()
        // Trava de Segurança: Se os dados em memória já têm conteúdo e a leitura temporária do disco retornou vazia
        // (por exemplo, leitura concorrente ou I/O interrompido), NUNCA sobrescrevemos a memória com lista vazia!
        if (loaded.isNotEmpty() || _banks.value.isEmpty()) {
            _banks.value = loaded
        }
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
