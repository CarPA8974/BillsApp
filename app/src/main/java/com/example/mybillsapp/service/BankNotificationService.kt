package com.example.mybillsapp.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.mybillsapp.data.BillsRepository
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BankPresets
import com.example.mybillsapp.model.BillCategory
import com.example.mybillsapp.model.BillItem

class BankNotificationService : NotificationListenerService() {

    private val bankPackageMap = mapOf(
        "com.nu.production" to "NUBANK",
        "com.itau" to "ITAU",
        "com.c6bank.app" to "C6_BANK",
        "br.com.intermedium" to "INTER",
        "com.santander.app" to "SANTANDER",
        "br.com.bb.android" to "BANCO_DO_BRASIL"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val presetKey = bankPackageMap[packageName] ?: return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val content = "$title $text".lowercase()

        Log.d("BankNotification", "Recebida de $presetKey: $content")

        // 1. Filtrar notificações indesejadas (Pix recebido, transferências, promoções)
        val ignoreKeywords = listOf(
            "transferência recebida", "pix recebido", "você recebeu", 
            "dinheiro guardado", "rendimento", "promoção", "fatura fechada",
            "pagamento recebido", "transferiu para você"
        )
        if (ignoreKeywords.any { content.contains(it) }) {
            Log.d("BankNotification", "Notificação ignorada por conter palavra reservada de recebimento/info.")
            return
        }

        // 2. Garantir que é uma notificação de "saída" de dinheiro (compra, pagamento, pix enviado)
        val validKeywords = listOf(
            "compra", "aprovada", "pagamento", "transferência enviada", 
            "pix enviado", "você pagou", "você transferiu"
        )
        if (validKeywords.none { content.contains(it) }) {
            Log.d("BankNotification", "Notificação ignorada pois não parece ser uma transação de saída de dinheiro.")
            return
        }

        // 3. Regex para extrair valores em reais: R$ 1.500,90 ou R$ 45,00
        val regex = Regex("""R\$\s*([\d\.,]+)""")
        val match = regex.find(content)

        if (match != null) {
            val rawValue = match.groupValues[1]
            val valueStr = rawValue.replace(".", "").replace(",", ".")
            val amount = valueStr.toDoubleOrNull() ?: return

            // 4. Extrair descrição da notificação original (mantendo o case)
            val originalTitle = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val originalText = extras.getString(Notification.EXTRA_TEXT) ?: ""
            val description = extrairDescricao(originalTitle, originalText)

            saveBillAutomatically(presetKey, description, amount)
        }
    }

    private fun extrairDescricao(title: String, text: String): String {
        // Tenta pegar palavras chave comuns em compras: "Compra aprovada em [Estabelecimento]"
        val compraRegex = Regex("""(?:em|no|na|para)\s+([A-Za-z0-9\s*.\-&]+)""", RegexOption.IGNORE_CASE)
        val match = compraRegex.find(text)
        
        if (match != null && match.groupValues[1].isNotBlank()) {
            val extracted = match.groupValues[1].trim()
            // Limpa coisas como "no valor de" que a regex pode acabar pegando
            if (!extracted.lowercase().startsWith("valor")) {
                return extracted
            }
        }
        
        return title.takeIf { it.isNotBlank() } ?: "Despesa Automática"
    }

    private fun saveBillAutomatically(presetKey: String, description: String, amount: Double) {
        val repository = BillsRepository(applicationContext)
        val currentBanks = repository.loadData().toMutableList()

        val preset = BankPresets.getPresetByKey(presetKey)
        
        // Encontra o banco ou cria se não existir
        var targetBank = currentBanks.find { it.presetKey == presetKey }
        if (targetBank == null) {
            targetBank = BankEntity(
                name = preset.name,
                colorHex = preset.hexColor,
                presetKey = preset.key
            )
            currentBanks.add(targetBank)
        }

        // Evita duplicação exata (mesma descrição e mesmo valor nos últimos 5 itens cadastrados)
        val isDuplicate = targetBank.items.takeLast(5).any { 
            it.description == description && it.monthlyAmount == amount 
        }
        if (isDuplicate) return

        val newItem = BillItem(
            description = description,
            monthlyAmount = amount,
            totalAmount = amount,
            totalInstallments = 1,
            currentInstallment = 1,
            dueDay = 10,
            category = BillCategory.OUTROS,
            isPaid = false,
            notes = "Adicionado automaticamente"
        )

        val updatedBank = targetBank.copy(items = targetBank.items + newItem)
        val index = currentBanks.indexOfFirst { it.id == targetBank.id }
        if (index != -1) {
            currentBanks[index] = updatedBank
        }

        repository.saveData(currentBanks)
        Log.d("BankNotification", "Despesa $description de R$$amount salva no banco ${preset.name}")
    }
}
