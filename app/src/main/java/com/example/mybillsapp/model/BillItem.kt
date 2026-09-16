package com.example.mybillsapp.model

import java.util.Calendar
import java.util.UUID

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val monthlyAmount: Double, // Parcela mensal
    val totalInstallments: Int = 1,
    val currentInstallment: Int = 1,
    val totalAmount: Double = monthlyAmount * totalInstallments, // Valor total
    val isPaid: Boolean = false,
    val dueDay: Int = 10, // Dia do mês em que vence (1-31)
    val category: BillCategory = BillCategory.OUTROS,
    val notes: String = "",
    val month: Int = Calendar.getInstance().get(Calendar.MONTH), // 0..11
    val year: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    // Quantia pendente para parcelas futuras (excluindo a atual)
    val remainingDebt: Double
        get() = if (totalInstallments > 1 && totalInstallments > currentInstallment) {
            monthlyAmount * (totalInstallments - currentInstallment)
        } else 0.0

    val remainingInstallmentsCount: Int
        get() = if (totalInstallments > 1) (totalInstallments - currentInstallment).coerceAtLeast(0) else 0

    fun isOverdue(): Boolean {
        if (isPaid) return false
        val today = Calendar.getInstance()
        val due = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dueDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return today.after(due)
    }

    fun isDueToday(): Boolean {
        if (isPaid) return false
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == year && 
               today.get(Calendar.MONTH) == month && 
               today.get(Calendar.DAY_OF_MONTH) == dueDay
    }
}
