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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BillItem
import com.example.mybillsapp.toCurrency

@Composable
fun BankCard(
    bank: BankEntity,
    displayedItems: List<BillItem>,
    isPrivacy: Boolean,
    modifier: Modifier = Modifier,
    showFuture: Boolean = false,
    onAddBill: () -> Unit,
    onEditBank: () -> Unit,
    onRemoveBank: () -> Unit,
    onToggleBill: (String) -> Unit,
    onEditBill: (BillItem) -> Unit,
    onRemoveBill: (String) -> Unit,
    onMoveBillNextMonth: (String) -> Unit = {},
    onPayAllBills: () -> Unit = {},
    onBankClick: () -> Unit = {}
) {
    // Preserva o estado de expandido/recolhido durante a rolagem (scroll)
    var expanded by rememberSaveable { mutableStateOf(true) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")
    val bankColor = bank.getColor()
    
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
                    .clickable { onBankClick() }
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
                    val monthPending = displayedItems.filter { !it.isPaid }.sumOf { it.monthlyAmount }
                    val totalDebt = bank.totalCommittedDebt

                    Text(
                        text = bank.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (!showFuture) {
                        Text(
                            text = if (isPrivacy) "Falta no mês: R$ ****" else "Falta no mês: ${monthPending.toCurrency()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isPrivacy) "Dívida Total: R$ ****" else "Dívida Total: ${totalDebt.toCurrency()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = if (isPrivacy) "Dívida Futura: R$ ****" else "Dívida Futura: ${bank.subtotalFutureDebt.toCurrency()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Bank Info
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vence dia ${bank.defaultDueDay} | Fecha dia ${bank.closingDay}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    // Credit Limit Progress & Info
                    if (bank.creditLimit > 0.0) {
                        val available = bank.availableLimit
                        val availablePercent = (available / bank.creditLimit).toFloat().coerceIn(0f, 1f)
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Livre: ${if (isPrivacy) "****" else available.toCurrency()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { availablePercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = Color(0xFF00E676), // Verde representando o limite livre restante
                            trackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                    }
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

                }
            }
        }
    }

