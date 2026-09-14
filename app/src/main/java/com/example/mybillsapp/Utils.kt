package com.example.mybillsapp

import java.text.NumberFormat
import java.util.Locale

fun Double.toCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(this)
}
