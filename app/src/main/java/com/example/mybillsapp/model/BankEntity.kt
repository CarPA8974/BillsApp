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
    val closingDay: Int = 1, // Dia em que a fatura fecha (joga para o mês seguinte)
    val isArchived: Boolean = false,
    val creditLimit: Double = 0.0 // Limite estático de crédito
) {
    fun itemsForMonth(targetMonth: Int, targetYear: Int): List<BillItem> {
        return items.filter { it.month == targetMonth && it.year == targetYear }
    }

    fun subtotalMonth(month: Int, year: Int): Double = itemsForMonth(month, year).sumOf { it.monthlyAmount }
    fun subtotalMonthPending(month: Int, year: Int): Double = itemsForMonth(month, year).filter { !it.isPaid }.sumOf { it.monthlyAmount }
    fun subtotalMonthPaid(month: Int, year: Int): Double = itemsForMonth(month, year).filter { it.isPaid }.sumOf { it.monthlyAmount }

    val subtotalCurrentMonth: Double get() = items.sumOf { it.monthlyAmount }
    val subtotalCurrentMonthPending: Double get() = items.filter { !it.isPaid }.sumOf { it.monthlyAmount }
    val subtotalCurrentMonthPaid: Double get() = items.filter { it.isPaid }.sumOf { it.monthlyAmount }
    val subtotalFutureDebt: Double get() = 0.0

    // Cálculo exato do total comprometido no cartão (considera compras legadas de parcela única e séries físicas)
    val totalCommittedDebt: Double
        get() {
            val grouped = items.groupBy { if (it.groupId.isNotBlank()) it.groupId else it.id }
            var total = 0.0
            for ((_, groupItems) in grouped) {
                if (groupItems.size > 1) {
                    total += groupItems.filter { !it.isPaid }.sumOf { it.monthlyAmount }
                } else {
                    val item = groupItems.first()
                    if (!item.isPaid) {
                        if (item.totalInstallments > 1 && item.totalAmount > item.monthlyAmount) {
                            val remainingCount = (item.totalInstallments - item.currentInstallment + 1).coerceAtLeast(1)
                            total += item.monthlyAmount * remainingCount
                        } else {
                            total += item.monthlyAmount
                        }
                    }
                }
            }
            return total
        }

    val availableLimit: Double
        get() {
            if (creditLimit <= 0.0) return 0.0
            return (creditLimit - totalCommittedDebt).coerceAtLeast(0.0)
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
