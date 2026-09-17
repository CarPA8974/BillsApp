package com.example.mybillsapp

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale

fun Double.toCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(this)
}

fun Color.getContrastingTextColor(): Color {
    val luminance = 0.299f * this.red + 0.587f * this.green + 0.114f * this.blue
    return if (luminance > 0.5f) Color(0xFF121212) else Color(0xFFFFFFFF)
}
