package com.example.mybillsapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BillItem
import com.example.mybillsapp.toCurrency

@Composable
fun BankCard(
    bank: BankEntity,
    isPrivacy: Boolean,
    modifier: Modifier = Modifier,
    showFuture: Boolean = false,
    isCurrentMonth: Boolean = true, // repassando pro BillListItem
    onAddBill: () -> Unit,
    onEditBank: () -> Unit,
    onRemoveBank: () -> Unit,
    onToggleBill: (String) -> Unit,
    onEditBill: (BillItem) -> Unit,
    onRemoveBill: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")
    val bankColor = bank.getColor()
    
    // O valor do banco agora vai DIMINUINDO conforme for pago ("value disappears" effect)
    val valueToShow = if (showFuture) bank.subtotalFutureDebt else bank.subtotalCurrentMonthPending

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                // Color Circle Indicator
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(bankColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bank.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPrivacy) "R$ ****" else "Falta " + valueToShow.toCurrency(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showFuture) MaterialTheme.colorScheme.onSurfaceVariant else bankColor
                    )
                }

                if (!showFuture) {
                    IconButton(onClick = onEditBank, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar Banco",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onRemoveBank, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remover Banco",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation)
                )
            }

            // Expanded List of Bills
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (bank.items.isEmpty()) {
                        Text(
                            text = "Nenhuma despesa cadastrada neste banco.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bank.items.forEach { item ->
                                val shouldShow = if (showFuture) item.remainingDebt > 0.0 else true
                                if (shouldShow) {
                                    BillListItem(
                                        item = item,
                                        isPrivacy = isPrivacy,
                                        showFuture = showFuture,
                                        currentMonthDisplay = isCurrentMonth,
                                        onTogglePaid = { onToggleBill(item.id) },
                                        onEdit = { onEditBill(item) },
                                        onDelete = { onRemoveBill(item.id) }
                                    )
                                }
                            }
                        }
                    }

                    if (!showFuture) {
                        OutlinedButton(
                            onClick = onAddBill,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, bankColor.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = bankColor)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adicionar Despesa", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
