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
    // Retorna as faturas do mês específico sem projeção dinâmica (agora as parcelas são objetos reais no banco)
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

    // Soma de todos os débitos não pagos neste banco (independentemente do mês)
    val totalUnpaidAmount: Double get() = items.filter { !it.isPaid }.sumOf { it.monthlyAmount }

    // Limite disponível variável e absoluto (Limite Total minus todas as pendências + parcelas futuras)
    val availableLimit: Double
        get() {
            if (creditLimit <= 0.0) return 0.0
            val totalCommitted = totalUnpaidAmount
            return (creditLimit - totalCommitted).coerceAtLeast(0.0)
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
