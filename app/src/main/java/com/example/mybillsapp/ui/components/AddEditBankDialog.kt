package com.example.mybillsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BankPresets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBankDialog(
    initialBank: BankEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, hexColor: String, presetKey: String, defaultDueDay: Int, closingDay: Int, creditLimit: Double) -> Unit
) {
    val isEditing = initialBank != null

    var name by remember { mutableStateOf(initialBank?.name ?: "") }
    var selectedPresetKey by remember { mutableStateOf(initialBank?.presetKey ?: "OUTRO") }
    var selectedHexColor by remember { mutableStateOf(initialBank?.colorHex ?: "#7C4DFF") }
    var defaultDueDayStr by remember { mutableStateOf(initialBank?.defaultDueDay?.toString() ?: "10") }
    var closingDayStr by remember { mutableStateOf(initialBank?.closingDay?.let { if (it > 0) it.toString() else "1" } ?: "1") }
    var creditLimitStr by remember { mutableStateOf(initialBank?.creditLimit?.let { if (it > 0) it.toString() else "" } ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Editar Banco / Cartão" else "Novo Banco / Cartão",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Escolha um banco ou crie um personalizado:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Presets Scrollable Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BankPresets.presets.forEach { preset ->
                        FilterChip(
                            selected = selectedPresetKey == preset.key,
                            onClick = {
                                selectedPresetKey = preset.key
                                selectedHexColor = preset.hexColor
                                if (name.isBlank() || BankPresets.presets.any { it.name == name }) {
                                    name = if (preset.key != "OUTRO") preset.name else ""
                                }
                            },
                            label = { Text(preset.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(preset.color)
                                )
                            }
                        )
                    }
                }

                // Bank Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(
                        Locale.getDefault()) else char.toString() } },
                    label = { Text("Nome da Instituição (ex: Nubank, Cartão C6)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )

                // Default Due Day & Closing Day Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = defaultDueDayStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) defaultDueDayStr = it },
                        label = { Text("Vencimento (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = closingDayStr,
                        onValueChange = { if (it.all { char -> char.isDigit() }) closingDayStr = it },
                        label = { Text("Fechamento (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Credit Limit Input
                OutlinedTextField(
                    value = creditLimitStr,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' || char == ',' }) creditLimitStr = it.replace(',', '.') },
                    label = { Text("Limite Total de Crédito (R$ - opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val parsedDueDay = defaultDueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 10
                        val parsedClosingDay = closingDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 1
                        val parsedLimit = creditLimitStr.toDoubleOrNull() ?: 0.0
                        onConfirm(name.trim(), selectedHexColor, selectedPresetKey, parsedDueDay, parsedClosingDay, parsedLimit)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isEditing) "Salvar" else "Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
