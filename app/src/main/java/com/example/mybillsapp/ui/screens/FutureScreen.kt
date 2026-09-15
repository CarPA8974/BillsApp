package com.example.mybillsapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.toCurrency
import com.example.mybillsapp.ui.components.BankCard

@Composable
fun FutureScreen(
    banks: List<BankEntity>,
    isPrivacy: Boolean,
    modifier: Modifier = Modifier
) {
    val totalFutureDebt = banks.sumOf { it.subtotalFutureDebt }
    val banksWithFutureDebt = banks.filter { it.subtotalFutureDebt > 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Total Future Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Parcelado Futuro",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPrivacy) "R$ ****" else totalFutureDebt.toCurrency(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            }
        }

        // Info Alert
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Estes valores representam o saldo devedor restante das parcelas futuras (excluindo a parcela do mês atual).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (banksWithFutureDebt.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma compra parcelada pendente para os próximos meses!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(banksWithFutureDebt, key = { it.id }) { bank ->
                    BankCard(
                        bank = bank,
                        isPrivacy = isPrivacy,
                        showFuture = true,
                        isCurrentMonth = false,
                        onAddBill = {},
                        onEditBank = {},
                        onRemoveBank = {},
                        onToggleBill = {},
                        onEditBill = {},
                        onRemoveBill = {}
                    )
                }
            }
        }
    }
}
