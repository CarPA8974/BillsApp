package com.example.mybillsapp.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

val DefaultBankColors = listOf(
    Color(0xFF8A05BE), // Purple
    Color(0xFFFF7A00), // Orange
    Color(0xFFEC7000), // Dark Orange
    Color(0xFF00E676), // Green
    Color(0xFF2196F3), // Blue
    Color(0xFFE91E63), // Pink
    Color(0xFFFFD740)  // Yellow
)

data class BankEntity(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val items: List<BillItem> = emptyList(),
    val colorHex: String = "#7C4DFF",
    val presetKey: String = "OUTRO",
    val colorIndex: Int = 0, // For backwards compatibility
    val defaultDueDay: Int = 10,
    val isArchived: Boolean = false,
    val creditLimit: Double = 0.0 // Limite estático de crédito
) {
    // Retorna as faturas do mês específico.
    // PROJEÇÃO AUTOMÁTICA DE PARCELAS: Se for um parcelamento (ex: 12x), o app calcula e exibe automaticamente
    // a parcela correspondente no mês seguinte sem precisar de duplicações manuais!
    fun itemsForMonth(targetMonth: Int, targetYear: Int): List<BillItem> {
        return items.mapNotNull { item ->
            if (item.month == targetMonth && item.year == targetYear) {
                item
            } else if (item.totalInstallments > 1) {
                val monthsDiff = (targetYear - item.year) * 12 + (targetMonth - item.month)
                val projectedInstallment = item.currentInstallment + monthsDiff
                if (projectedInstallment in 1..item.totalInstallments) {
                    item.copy(
                        currentInstallment = projectedInstallment,
                        month = targetMonth,
                        year = targetYear,
                        isPaid = if (monthsDiff > 0) false else item.isPaid
                    )
                } else null
            } else null
        }
    }

    fun subtotalMonth(month: Int, year: Int): Double = itemsForMonth(month, year).sumOf { it.monthlyAmount }
    fun subtotalMonthPending(month: Int, year: Int): Double = itemsForMonth(month, year).filter { !it.isPaid }.sumOf { it.monthlyAmount }
    fun subtotalMonthPaid(month: Int, year: Int): Double = itemsForMonth(month, year).filter { it.isPaid }.sumOf { it.monthlyAmount }

    val subtotalCurrentMonth: Double get() = items.sumOf { it.monthlyAmount }
    val subtotalCurrentMonthPending: Double get() = items.filter { !it.isPaid }.sumOf { it.monthlyAmount }
    val subtotalCurrentMonthPaid: Double get() = items.filter { it.isPaid }.sumOf { it.monthlyAmount }
    val subtotalFutureDebt: Double get() = items.sumOf { it.remainingDebt }

    // Limite disponível variável (Limite Total minus dívidas acumuladas)
    fun getAvailableLimit(month: Int, year: Int): Double {
        if (creditLimit <= 0.0) return 0.0
        val committedDebt = subtotalMonthPending(month, year) + subtotalFutureDebt
        return (creditLimit - committedDebt).coerceAtLeast(0.0)
    }

    fun getColor(): Color {
        return try {
            if (colorHex.isNotBlank() && colorHex.startsWith("#")) {
                Color(android.graphics.Color.parseColor(colorHex))
            } else if (colorIndex in DefaultBankColors.indices) {
                DefaultBankColors[colorIndex]
            } else {
                DefaultBankColors[0]
            }
        } catch (e: Exception) {
            DefaultBankColors[0]
        }
    }
}
