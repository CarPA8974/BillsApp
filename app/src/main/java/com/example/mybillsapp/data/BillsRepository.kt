package com.example.mybillsapp.data

import android.content.Context
import android.util.Log
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BankPresets
import com.google.gson.Gson
import java.io.File
import java.util.Calendar

class BillsRepository(private val context: Context) {
    private val fileName = "mybills_v3.json"
    private val gson = Gson()

    fun loadData(): List<BankEntity> {
        val cal = Calendar.getInstance()
        val defaultMonth = cal.get(Calendar.MONTH)
        val defaultYear = cal.get(Calendar.YEAR)

        return try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val jsonText = file.readText()
                if (jsonText.isBlank()) return emptyList()

                val loadedArray: Array<BankEntity>? = gson.fromJson(jsonText, Array<BankEntity>::class.java)
                val loadedList = loadedArray?.toList() ?: return emptyList()
                
                // Sanitize and migrate loaded items with strict null-safety
                loadedList.map { bank ->
                    val preset = BankPresets.presets.find { it.key == bank.presetKey }
                    val safeHex = if (bank.colorHex.isNotBlank() && bank.colorHex.startsWith("#")) {
                        bank.colorHex
                    } else preset?.hexColor ?: BankPresets.presets[(bank.colorIndex.coerceAtLeast(0)) % BankPresets.presets.size].hexColor

                    bank.copy(
                        colorHex = safeHex,
                        items = bank.items.map { item ->
                            val safeCategory = item.category
                            val safeDueDay = if (item.dueDay in 1..31) item.dueDay else 10
                            val safeTotalAmount = if (item.totalAmount > 0.0) item.totalAmount else item.monthlyAmount * item.totalInstallments
                            val safeYear = if (item.year > 2000) item.year else defaultYear
                            val safeMonth = if (item.month in 0..11) item.month else defaultMonth

                            item.copy(
                                category = safeCategory,
                                dueDay = safeDueDay,
                                totalAmount = safeTotalAmount,
                                month = safeMonth,
                                year = safeYear
                            )
                        }
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BillsRepository", "Erro ao carregar dados: ${e.message}", e)
            emptyList()
        }
    }

    fun saveData(list: List<BankEntity>) {
        try {
            val file = File(context.filesDir, fileName)
            val jsonText = gson.toJson(list)
            file.writeText(jsonText)
            Log.d("BillsRepository", "Dados salvos com sucesso (${list.size} bancos).")
        } catch (e: Exception) {
            Log.e("BillsRepository", "Erro ao salvar dados: ${e.message}", e)
        }
    }

    fun exportJson(list: List<BankEntity>): String {
        return gson.toJson(list)
    }

    fun importJson(jsonString: String): List<BankEntity>? {
        return try {
            val loadedArray = gson.fromJson(jsonString, Array<BankEntity>::class.java)
            loadedArray?.toList()
        } catch (e: Exception) {
            Log.e("BillsRepository", "Erro ao importar JSON: ${e.message}", e)
            null
        }
    }
}
