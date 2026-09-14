package com.example.mybillsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BankPreset
import com.example.mybillsapp.model.BankPresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBankDialog(
    initialBank: BankEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, hexColor: String, presetKey: String) -> Unit
) {
    val isEditing = initialBank != null

    var name by remember { mutableStateOf(initialBank?.name ?: "") }
    var selectedPresetKey by remember { mutableStateOf(initialBank?.presetKey ?: "OUTRO") }
    var selectedHexColor by remember { mutableStateOf(initialBank?.colorHex ?: "#7C4DFF") }

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
                    onValueChange = { name = it },
                    label = { Text("Nome da Instituição (ex: Nubank, Cartão C6)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedHexColor, selectedPresetKey)
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
