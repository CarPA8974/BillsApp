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
    val colorIndex: Int = 0 // For backwards compatibility
) {
    fun itemsForMonth(month: Int, year: Int): List<BillItem> {
        return items.filter { it.month == month && it.year == year }
    }

    fun subtotalMonth(month: Int, year: Int): Double = itemsForMonth(month, year).sumOf { it.monthlyAmount }
    fun subtotalMonthPending(month: Int, year: Int): Double = itemsForMonth(month, year).filter { !it.isPaid }.sumOf { it.monthlyAmount }
    fun subtotalMonthPaid(month: Int, year: Int): Double = itemsForMonth(month, year).filter { it.isPaid }.sumOf { it.monthlyAmount }

    val subtotalCurrentMonth: Double get() = items.sumOf { it.monthlyAmount }
    val subtotalCurrentMonthPending: Double get() = items.filter { !it.isPaid }.sumOf { it.monthlyAmount }
    val subtotalCurrentMonthPaid: Double get() = items.filter { it.isPaid }.sumOf { it.monthlyAmount }
    val subtotalFutureDebt: Double get() = items.sumOf { it.remainingDebt }

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
