package com.example.mybillsapp.model

import androidx.compose.ui.graphics.Color

data class BankPreset(
    val key: String,
    val name: String,
    val color: Color,
    val hexColor: String
)

object BankPresets {
    val presets = listOf(
        BankPreset("NUBANK", "Nubank", Color(0xFF8A05BE), "#8A05BE"),
        BankPreset("INTER", "Banco Inter", Color(0xFFFF7A00), "#FF7A00"),
        BankPreset("ITAU", "Itaú", Color(0xFFEC7000), "#EC7000"),
        BankPreset("BRADESCO", "Bradesco", Color(0xFFCC092F), "#CC092F"),
        BankPreset("SANTANDER", "Santander", Color(0xFFEC0000), "#EC0000"),
        BankPreset("CAIXA", "Caixa Econômica", Color(0xFF005CA9), "#005CA9"),
        BankPreset("C6_BANK", "C6 Bank", Color(0xFF2B2B2B), "#2B2B2B"),
        BankPreset("BANCO_DO_BRASIL", "Banco do Brasil", Color(0xFF0038A8), "#0038A8"),
        BankPreset("BTG", "BTG Pactual", Color(0xFF001E62), "#001E62"),
        BankPreset("OUTRO", "Outro Banco", Color(0xFF7C4DFF), "#7C4DFF")
    )

    fun getPresetByKey(key: String): BankPreset {
        return presets.find { it.key.equals(key, ignoreCase = true) } ?: presets.last()
    }
}
