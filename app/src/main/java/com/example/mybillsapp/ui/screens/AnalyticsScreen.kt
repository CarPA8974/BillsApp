package com.example.mybillsapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.mybillsapp.toCurrency
import com.example.mybillsapp.ui.components.ChartSlice
import com.example.mybillsapp.ui.components.DonutChart

enum class AnalyticsType {
    BY_BANK,
    BY_CATEGORY
}

@Composable
fun AnalyticsScreen(
    banks: List<BankEntity>,
    isPrivacy: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(AnalyticsType.BY_BANK) }

    // O gráfico agora mostrará apenas o que FALTA pagar!
    val totalPendingAmount = banks.sumOf { it.subtotalCurrentMonthPending }

    // Slices for By Bank
    val bankSlices = remember(banks) {
        banks.filter { it.subtotalCurrentMonthPending > 0 }.map { bank ->
            ChartSlice(
                label = bank.name,
                amount = bank.subtotalCurrentMonthPending,
                color = bank.getColor()
            )
        }
    }

    // Slices for By Category
    val categorySlices = remember(banks) {
        val allBills = banks.flatMap { it.items }.filter { !it.isPaid } // Só pega as que NÃO estão pagas!
        val grouped = allBills.groupBy { it.category }
        grouped.map { (category, items) ->
            val sum = items.sumOf { it.monthlyAmount }
            ChartSlice(
                label = category.displayName,
                amount = sum,
                color = category.color
            )
        }.filter { it.amount > 0 }.sortedByDescending { it.amount }
    }

    val activeSlices = if (selectedType == AnalyticsType.BY_BANK) bankSlices else categorySlices

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Type Selector Tab Row
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedType == AnalyticsType.BY_BANK,
                onClick = { selectedType = AnalyticsType.BY_BANK },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Por Banco")
            }
            SegmentedButton(
                selected = selectedType == AnalyticsType.BY_CATEGORY,
                onClick = { selectedType = AnalyticsType.BY_CATEGORY },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Por Categoria")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (totalPendingAmount <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Todas as contas estão pagas! 🎉",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Donut Chart Item
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            slices = activeSlices,
                            totalAmount = totalPendingAmount,
                            isPrivacy = isPrivacy
                        )
                    }
                }

                // Breakdown list header
                item {
                    Text(
                        text = if (selectedType == AnalyticsType.BY_BANK) "Detalhamento (Falta Pagar) por Instituição" else "Detalhamento (Falta Pagar) por Categoria",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                // Slice Items
                items(activeSlices, key = { it.label }) { slice ->
                    val percentage = (slice.amount / totalPendingAmount * 100).toInt()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = slice.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isPrivacy) "R$ ****" else slice.amount.toCurrency(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
