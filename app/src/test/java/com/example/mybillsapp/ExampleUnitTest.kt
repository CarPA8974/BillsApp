package com.example.mybillsapp

import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BillCategory
import com.example.mybillsapp.model.BillItem
import com.example.mybillsapp.model.MonthYear
import com.google.gson.Gson
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun testBillItemDebtCalculation() {
        val item = BillItem(
            description = "TV 55",
            monthlyAmount = 200.0,
            totalInstallments = 10,
            currentInstallment = 2,
            isPaid = false
        )
        // 10 installments total, 2 paid/current -> 8 remaining installments -> 8 * 200 = 1600 remaining debt
        assertEquals(1600.0, item.remainingDebt, 0.01)
        assertEquals(8, item.remainingInstallmentsCount)
    }

    @Test
    fun testBankEntityTotals() {
        val item1 = BillItem(
            description = "Mercado",
            monthlyAmount = 300.0,
            isPaid = true
        )
        val item2 = BillItem(
            description = "Internet",
            monthlyAmount = 120.0,
            isPaid = false
        )
        val bank = BankEntity(
            name = "Nubank",
            items = listOf(item1, item2)
        )

        assertEquals(420.0, bank.subtotalCurrentMonth, 0.01)
        assertEquals(300.0, bank.subtotalCurrentMonthPaid, 0.01)
        assertEquals(120.0, bank.subtotalCurrentMonthPending, 0.01)
    }

    @Test
    fun testJsonSerializationDeserialization() {
        val gson = Gson()
        val item = BillItem(
            description = "Aluguel",
            monthlyAmount = 1500.0,
            dueDay = 10,
            category = BillCategory.MORADIA,
            month = 2,
            year = 2025
        )
        val bank = BankEntity(
            name = "Itaú",
            colorHex = "#EC7000",
            presetKey = "ITAU",
            items = listOf(item)
        )
        val originalList = listOf(bank)

        val jsonText = gson.toJson(originalList)
        assertNotNull(jsonText)

        // Test Type-Safe Array Deserialization
        val deserializedArray = gson.fromJson(jsonText, Array<BankEntity>::class.java)
        assertNotNull(deserializedArray)
        assertEquals(1, deserializedArray.size)

        val restoredBank = deserializedArray[0]
        assertEquals("Itaú", restoredBank.name)
        assertEquals(1, restoredBank.items.size)

        val restoredItem = restoredBank.items[0]
        assertEquals("Aluguel", restoredItem.description)
        assertEquals(1500.0, restoredItem.monthlyAmount, 0.01)
        assertEquals(2, restoredItem.month)
        assertEquals(2025, restoredItem.year)
        assertEquals(BillCategory.MORADIA, restoredItem.category)
    }

    @Test
    fun testMonthYearNavigation() {
        val my = MonthYear(month = 0, year = 2025) // Jan 2025
        val next = my.nextMonth()
        assertEquals(1, next.month) // Feb
        assertEquals(2025, next.year)

        val prev = my.previousMonth()
        assertEquals(11, prev.month) // Dec
        assertEquals(2024, prev.year)
    }

    @Test
    fun testCategoryFromString() {
        val cat = BillCategory.fromString("Alimentação")
        assertEquals(BillCategory.ALIMENTACAO, cat)

        val unknown = BillCategory.fromString("XYZ_UNKNOWN")
        assertEquals(BillCategory.OUTROS, unknown)
    }
}
