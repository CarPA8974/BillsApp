package com.example.mybillsapp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class BillCategory(
    val displayName: String,
    val icon: ImageVector,
    val color: Color
) {
    ALIMENTACAO("Alimentação", Icons.Default.ShoppingCart, Color(0xFF4CAF50)),
    MORADIA("Moradia & Contas", Icons.Default.Home, Color(0xFFFF9800)),
    TRANSPORTE("Transporte", Icons.Default.DirectionsCar, Color(0xFF2196F3)),
    CARTAO("Cartão de Crédito", Icons.Default.CreditCard, Color(0xFF9C27B0)),
    LAZER("Lazer & Entretenimento", Icons.Default.Movie, Color(0xFFE91E63)),
    SAUDE("Saúde & Bem-estar", Icons.Default.LocalHospital, Color(0xFFF44336)),
    ASSINATURAS("Assinaturas", Icons.Default.Subscriptions, Color(0xFF00BCD4)),
    EDUCACAO("Educação", Icons.Default.School, Color(0xFF3F51B5)),
    OUTROS("Outros", Icons.Default.MoreHoriz, Color(0xFF9E9E9E));

    companion object {
        fun fromString(name: String?): BillCategory {
            if (name.isNullOrEmpty()) return OUTROS
            return entries.find { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: OUTROS
        }
    }
}
