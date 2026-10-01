package com.example.mybillsapp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.model.BillCategory
import com.example.mybillsapp.model.BillItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(
    initialItem: BillItem? = null,
    defaultDueDay: Int = 10,
    onDismiss: () -> Unit,
    onConfirm: (
        description: String,
        monthlyAmount: Double,
        totalAmount: Double,
        installments: Int,
        currentInstallment: Int,
        dueDay: Int,
        category: BillCategory,
        notes: String
    ) -> Unit
) {
    val isEditing = initialItem != null

    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var isTotalAmountMode by remember { mutableStateOf(true) }
    var amountInput by remember {
        mutableStateOf(
            if (initialItem != null) {
                if (initialItem.totalInstallments > 1) initialItem.totalAmount.toString()
                else initialItem.monthlyAmount.toString()
            } else ""
        )
    }
    var installmentsInput by remember { mutableStateOf(initialItem?.totalInstallments?.toString() ?: "1") }
    var currentInstallmentInput by remember { mutableStateOf(initialItem?.currentInstallment?.toString() ?: "1") }
    var dueDayInput by remember { mutableStateOf(initialItem?.dueDay?.toString() ?: defaultDueDay.toString()) }
    var selectedCategory by remember { mutableStateOf(initialItem?.category ?: BillCategory.OUTROS) }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Editar Despesa" else "Nova Despesa",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { 
                        description = it
                        errorMessage = null
                        if (!isEditing && it.length >= 2) {
                            val inferred = BillCategory.inferFromText(it)
                            if (inferred != BillCategory.OUTROS) {
                                selectedCategory = inferred
                            }
                        }
                    },
                    label = { Text("Descrição (ex: Supermercado, TV 55\")") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount Mode Toggle (Total vs Parcela)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tipo do Valor:", style = MaterialTheme.typography.bodySmall)
                    Row {
                        FilterChip(
                            selected = isTotalAmountMode,
                            onClick = { isTotalAmountMode = true },
                            label = { Text("Valor Total") }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        FilterChip(
                            selected = !isTotalAmountMode,
                            onClick = { isTotalAmountMode = false },
                            label = { Text("Valor Parcela") }
                        )
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c == ',' }) amountInput = it.replace(',', '.'); errorMessage = null },
                    label = { Text(if (isTotalAmountMode) "Valor TOTAL (R$)" else "Valor de CADA PARCELA (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Installments Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = installmentsInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) installmentsInput = it },
                        label = { Text("Total Parcelas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = currentInstallmentInput,
                        onValueChange = { if (it.all { c -> c.isDigit() }) currentInstallmentInput = it },
                        label = { Text("Parcela Atual") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Due Day
                OutlinedTextField(
                    value = dueDayInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) dueDayInput = it },
                    label = { Text("Dia de Vencimento (1 a 31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selector
                Text("Categoria:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BillCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = category.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // Notes / Observações
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawAmount = amountInput.toDoubleOrNull()
                    val installments = installmentsInput.toIntOrNull() ?: 1
                    val currentInstallment = (currentInstallmentInput.toIntOrNull() ?: 1).coerceIn(1, installments)
                    val dueDay = (dueDayInput.toIntOrNull() ?: defaultDueDay).coerceIn(1, 31)

                    if (description.isBlank()) {
                        errorMessage = "Por favor, informe a descrição."
                        return@Button
                    }
                    if (rawAmount == null || rawAmount <= 0) {
                        errorMessage = "Por favor, informe um valor válido."
                        return@Button
                    }

                    val totalAmount: Double
                    val monthlyAmount: Double
                    if (isTotalAmountMode) {
                        totalAmount = rawAmount
                        monthlyAmount = totalAmount / installments.coerceAtLeast(1)
                    } else {
                        monthlyAmount = rawAmount
                        totalAmount = monthlyAmount * installments.coerceAtLeast(1)
                    }

                    onConfirm(
                        description.trim(),
                        monthlyAmount,
                        totalAmount,
                        installments.coerceAtLeast(1),
                        currentInstallment,
                        dueDay,
                        selectedCategory,
                        notes.trim()
                    )
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
