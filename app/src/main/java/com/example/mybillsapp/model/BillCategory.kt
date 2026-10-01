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
    OUTROS("Outros", Icons.Default.Receipt, Color(0xFF9E9E9E));

    companion object {
        fun fromString(name: String?): BillCategory {
            if (name.isNullOrEmpty()) return OUTROS
            return entries.find { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) }
                ?: OUTROS
        }

        fun inferFromText(rawText: String): BillCategory {
            if (rawText.isBlank()) return OUTROS
            val text = rawText.lowercase()

            val alimentacaoKeywords = listOf(
                "ifood", "rappi", "ze delivery", "zé delivery", "ubereats", "aiqfome", "cornerstore", "daki",
                "mercado", "supermercado", "carrefour", "pao de acucar", "pão de açúcar", "extra", "assai", "assaí",
                "atacadão", "atacadao", "sams club", "sam's club", "makro", "dia%", "guanabara", "zona sul", "verdemar",
                "st marche", "st. marche", "swift", "hortifruti", "natural da terra", "sacolão", "sacolao", "açougue",
                "acougue", "padaria", "panificadora", "mcdonald", "mcdonalds", "mc donalds", "burger king", "outback",
                "habib", "habibs", "subway", "spoleto", "bobs", "giraffas", "madero", "coco bambu", "ragazzo", "starbucks",
                "cacau show", "kopenhagen", "kfc", "dominos", "pizza hut", "pizzaria", "churrascaria", "lanchonete",
                "restaurante", "bar ", "pub ", "cafeteria", "bacio di latte", "chiquinho", "sorvete"
            )

            val transporteKeywords = listOf(
                "uber", "99", "99app", "99pop", "indrive", "indriver", "cabify", "blablacar", "taxi", "táxi",
                "ipiranga", "shell", "petrobras", "petrobrás", "posto", "combustivel", "combustível", "gasolina",
                "etanol", "diesel", "graal", "sem parar", "semparar", "veloe", "conectcar", "move mais", "estacionamento",
                "estapar", "indigo", "multipark", "zona azul", "latam", "gol", "azul", "voegol", "voeazul", "decolar",
                "123milhas", "clickbus", "buser", "passagem", "rodoviaria", "metrô", "metro", "cptm", "sptrans", "viamobilidade"
            )

            val moradiaKeywords = listOf(
                "enel", "sabesp", "comgas", "comgás", "cemig", "copel", "cpfl", "light", "equatorial", "neoenergia",
                "sanepar", "saneago", "cagece", "embasa", "corsan", "edp", "claro", "vivo", "tim", "sky", "algar",
                "brisanet", "sumicity", "desktop", "vero", "telecom", "fibra", "internet", "leroy merlin", "leroy",
                "telhanorte", "c&c", "cec", "dicico", "tok&stok", "tok stok", "etna", "mobly", "casaevideo", "casas bahia",
                "ponto frio", "fast shop", "magalu", "magazine luiza", "condominio", "condomínio", "aluguel",
                "quinto andar", "quintoandar", "loft", "imobiliaria", "iptu"
            )

            val lazerKeywords = listOf(
                "netflix", "spotify", "youtube", "deezer", "apple music", "tidal", "globoplay", "disney", "disney+",
                "star+", "hbo", "max", "paramount", "prime video", "telecine", "crunchyroll", "twitch", "steam",
                "playstation", "psn", "xbox", "nintendo", "epic games", "riot games", "blizzard", "roblox", "cinemark",
                "kinoplex", "uci", "cinesystem", "cinépolis", "ingressos.com", "sympla", "eventim", "blueticket", "ticket360"
            )

            val saudeKeywords = listOf(
                "drogasil", "droga raia", "drogaraia", "pague menos", "drogaria sp", "drogaria sao paulo", "pacheco",
                "ultrafarma", "panvel", "farmacia", "drogaria", "manipulação", "fleury", "delboni", "lavoisier",
                "hermes pardini", "labi", "dasa", "sulamerica", "bradesco saude", "unimed", "hapvida", "notredame",
                "intermédica", "porto seguro saude", "doctoralia", "consulta", "smartfit", "smart fit", "bluefit",
                "bodytech", "selfit", "gympass", "totalpass", "panobianco", "suplementos", "growth"
            )

            val assinaturasKeywords = listOf(
                "chatgpt", "openai", "midjourney", "claude", "canva", "adobe", "microsoft 365", "office 365", "github",
                "google one", "icloud", "notion", "slack", "zoom", "duolingo", "wine", "clube "
            )

            val educacaoKeywords = listOf(
                "udemy", "coursera", "alura", "domestika", "rocketseat", "estácio", "estacio", "kroton", "cogna",
                "anhanguera", "unip", "puc", "fgv", "senac", "senai", "livraria", "saraiva", "leitura", "kindle"
            )

            val cartaoKeywords = listOf(
                "cartão de crédito", "cartao de credito", "anuidade", "fatura cartão", "fatura de cartão", "tarifa cartão"
            )

            return when {
                alimentacaoKeywords.any { text.contains(it) } -> ALIMENTACAO
                transporteKeywords.any { text.contains(it) } -> TRANSPORTE
                moradiaKeywords.any { text.contains(it) } -> MORADIA
                lazerKeywords.any { text.contains(it) } -> LAZER
                saudeKeywords.any { text.contains(it) } -> SAUDE
                assinaturasKeywords.any { text.contains(it) } -> ASSINATURAS
                educacaoKeywords.any { text.contains(it) } -> EDUCACAO
                cartaoKeywords.any { text.contains(it) } -> CARTAO
                else -> OUTROS
            }
        }
    }
}
