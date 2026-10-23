package com.example.mybillsapp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BankPresets
import com.google.gson.Gson
import java.util.Calendar

class BillsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("mybills_database", Context.MODE_PRIVATE)
    private val dataKey = "banks_data_v4"
    private val gson = Gson()

    private var preferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    companion object {
        private val fileLock = Any()
    }

    fun setOnDataChangeListener(listener: () -> Unit) {
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == dataKey) {
                listener()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun loadData(): List<BankEntity> {
        val cal = Calendar.getInstance()
        val defaultMonth = cal.get(Calendar.MONTH)
        val defaultYear = cal.get(Calendar.YEAR)

        synchronized(fileLock) {
            return try {
                val jsonText = prefs.getString(dataKey, null)
                if (jsonText.isNullOrBlank()) return emptyList()

                val loadedArray: Array<BankEntity>? = gson.fromJson(jsonText, Array<BankEntity>::class.java)
                val loadedList = loadedArray?.toList() ?: return emptyList()
                
                // Sanitize and migrate loaded items with strict null-safety
                loadedList.map { bank ->
                    val preset = BankPresets.presets.find { it.key == bank.presetKey }
                    val safeHex = if (bank.colorHex.isNotBlank() && bank.colorHex.startsWith("#")) {
                        bank.colorHex
                    } else preset?.hexColor ?: BankPresets.presets[(bank.colorIndex.coerceAtLeast(0)) % BankPresets.presets.size].hexColor

                    val safeDefaultDueDay = if (bank.defaultDueDay in 1..31) bank.defaultDueDay else 10
                    val safeClosingDay = if (bank.closingDay in 1..31) bank.closingDay else (safeDefaultDueDay - 7).let { if (it < 1) it + 30 else it }
                    val safeIsArchived = bank.isArchived
                    val safeLimit = if (bank.creditLimit >= 0.0) bank.creditLimit else 0.0
                    val safePixKey = bank.pixKey ?: ""

                    bank.copy(
                        colorHex = safeHex,
                        defaultDueDay = safeDefaultDueDay,
                        closingDay = safeClosingDay,
                        isArchived = safeIsArchived,
                        creditLimit = safeLimit,
                        pixKey = safePixKey,
                        items = bank.items.filter { it.currentInstallment <= it.totalInstallments }.map { item ->
                            val safeCategory = item.category
                            val safeDueDay = if (item.dueDay in 1..31) item.dueDay else 10
                            val safeTotalAmount = if (item.totalAmount > 0.0) item.totalAmount else item.monthlyAmount * item.totalInstallments
                            val safeYear = if (item.year > 2000) item.year else defaultYear
                            val safeMonth = if (item.month in 0..11) item.month else defaultMonth
                            val safeTimestamp = if (item.timestamp > 0) item.timestamp else System.currentTimeMillis()
                            val safeGroupId = if (item.groupId.isNullOrBlank()) item.id else item.groupId

                            item.copy(
                                groupId = safeGroupId,
                                category = safeCategory,
                                dueDay = safeDueDay,
                                totalAmount = safeTotalAmount,
                                month = safeMonth,
                                year = safeYear,
                                timestamp = safeTimestamp
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("BillsRepository", "Erro ao carregar dados: ${e.message}", e)
                emptyList()
            }
        }
    }

    fun saveData(list: List<BankEntity>) {
        synchronized(fileLock) {
            try {
                val jsonText = gson.toJson(list)
                prefs.edit().putString(dataKey, jsonText).apply()
                Log.d("BillsRepository", "Dados salvos atomicamente com sucesso (${list.size} bancos).")
            } catch (e: Exception) {
                Log.e("BillsRepository", "Erro ao salvar dados: ${e.message}", e)
            }
        }
    }

    fun exportJson(list: List<BankEntity>): String {
        return gson.toJson(list)
    }

    fun importJson(jsonString: String): List<BankEntity>? {
        synchronized(fileLock) {
            return try {
                val loadedArray = gson.fromJson(jsonString, Array<BankEntity>::class.java)
                val loadedList = loadedArray?.toList()
                if (loadedList != null) {
                    saveData(loadedList)
                }
                loadedList
            } catch (e: Exception) {
                Log.e("BillsRepository", "Erro ao importar JSON: ${e.message}", e)
                null
            }
        }
    }
}
