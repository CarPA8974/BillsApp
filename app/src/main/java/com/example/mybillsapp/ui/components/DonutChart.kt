package com.example.mybillsapp.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.toCurrency

data class ChartSlice(
    val label: String,
    val amount: Double,
    val color: Color
)

@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    totalAmount: Double,
    isPrivacy: Boolean,
    modifier: Modifier = Modifier
) {
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "chartAnim"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokeWidth = 36f
            val radius = size.minDimension / 2 - strokeWidth
            drawCircle(
                color = Color.Gray.copy(alpha = 0.15f),
                radius = radius,
                style = Stroke(strokeWidth)
            )

            if (totalAmount > 0) {
                var startAngle = -90f
                slices.forEach { slice ->
                    val proportion = (slice.amount / totalAmount).toFloat()
                    val sweepAngle = proportion * 360f * animProgress
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Falta Pagar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isPrivacy) "R$ ****" else totalAmount.toCurrency(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
