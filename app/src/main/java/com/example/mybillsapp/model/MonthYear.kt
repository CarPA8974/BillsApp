package com.example.mybillsapp.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthYear(
    val month: Int, // 0..11
    val year: Int
) {
    fun formatDisplay(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val localePt = Locale.forLanguageTag("pt-BR")
        val monthName = SimpleDateFormat("MMMM", localePt).format(calendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localePt) else it.toString() }
        return "$monthName de $year"
    }

    fun nextMonth(): MonthYear {
        val cal = toCalendar()
        cal.add(Calendar.MONTH, 1)
        return MonthYear(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    fun previousMonth(): MonthYear {
        val cal = toCalendar()
        cal.add(Calendar.MONTH, -1)
        return MonthYear(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
    }

    private fun toCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal
    }

    companion object {
        fun current(): MonthYear {
            val cal = Calendar.getInstance()
            return MonthYear(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
        }
    }
}
