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
        "br.com.bb.android" to "BANCO_DO_BRASIL",
        "br.com.bradesco.next" to "NEXT",
        "br.com.brainweb.ifood" to "OUTRO", // Se quiser capturar do ifood
        "com.mercadopago.wallet" to "MERCADO_PAGO",
        "com.picpay" to "PICPAY",
        "br.com.uol.ps.myaccount" to "PAGBANK",
        "br.com.neon" to "NEON"
    )

    private var lastProcessedNotificationTime: Long = 0
    private var lastProcessedContent: String = ""

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val presetKey = bankPackageMap[packageName] ?: return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val content = "$title $text".lowercase()

        // PREVENÇÃO DE DUPLICATAS: Evita processar a exata mesma notificação em menos de 5 segundos
        val currentTime = System.currentTimeMillis()
        if (content == lastProcessedContent && (currentTime - lastProcessedNotificationTime) < 5000) {
            Log.d("BankNotification", "Notificação duplicada ignorada (Tempo: ${currentTime - lastProcessedNotificationTime}ms)")
            return
        }
        lastProcessedNotificationTime = currentTime
        lastProcessedContent = content

        Log.d("BankNotification", "Recebida de $presetKey: $content")

        // 1. Filtrar notificações indesejadas (Pix recebido, transferências, promoções)
        val ignoreKeywords = listOf(
            "transferência recebida", "pix recebido", "você recebeu", 
            "dinheiro guardado", "rendimento", "promoção", "fatura fechada",
            "pagamento recebido", "transferiu para você", "chegou um pix"
        )
        if (ignoreKeywords.any { content.contains(it) }) {
            Log.d("BankNotification", "Notificação ignorada por conter palavra reservada de recebimento/info.")
            return
        }

        // 2. Garantir que é uma notificação de "saída" de dinheiro (compra, pagamento, pix enviado)
        // Adicionado "pix feito" e "transferência realizada"
        val validKeywords = listOf(
            "compra", "aprovada", "pagamento", "transferência enviada", 
            "pix enviado", "pix realizado", "você pagou", "você transferiu",
            "pagamento aprovado", "transferência realizada", "pix feito",
            "aprovado", "débito"
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
        } else {
             Log.d("BankNotification", "Valor em R$ não encontrado na notificação.")
        }
    }

    private fun extrairDescricao(title: String, text: String): String {
        // Combinamos título e texto para garantir que não perdemos o nome do estabelecimento
        val fullContent = "$title $text"
        
        // Tenta pegar palavras chave comuns em compras: "Compra aprovada em [Estabelecimento]"
        val compraRegex = Regex("""(?:em|no|na|para)\s+([A-Za-z0-9\s*.\-&]+?)(?=\s+para|\s+no valor|\s*$)""", RegexOption.IGNORE_CASE)
        val match = compraRegex.find(fullContent)
        
        if (match != null && match.groupValues[1].isNotBlank()) {
            var extracted = match.groupValues[1].trim()
            
            // Limpezas extras baseadas na formatação do Nubank/Itaú
            val cleanRegex = Regex("""(?i)^(o cartão|o crédito|crédito|débito|valor de|r\$).*""")
            if (cleanRegex.matches(extracted)) {
                // Se pegou lixo, tenta fallback para o título
                 return fallbackDescricao(title)
            }
            
            // Remove coisas do tipo " para o cartão final..." que vêm coladas
            val breakPoint = extracted.indexOf(" para o cartão")
            if (breakPoint > 0) {
                extracted = extracted.substring(0, breakPoint).trim()
            }

            return extracted
        }
        
        return fallbackDescricao(title)
    }

    private fun fallbackDescricao(title: String): String {
        val cleanTitle = title.replace(Regex("""(?i)compra (no crédito|no débito)?\s?aprovada\s?:?"""), "").trim()
        return cleanTitle.takeIf { it.isNotBlank() } ?: "Despesa Automática"
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
        if (isDuplicate) {
            Log.d("BankNotification", "Despesa $description já foi cadastrada recentemente. Ignorando.")
            return
        }

        val newItem = BillItem(
            description = description,
            monthlyAmount = amount,
            totalAmount = amount,
            totalInstallments = 1,
            currentInstallment = 1,
            dueDay = 10,
            category = BillCategory.OUTROS,
            isPaid = false,
            notes = "Adicionado automaticamente via notificação"
        )

        val updatedBank = targetBank.copy(items = targetBank.items + newItem)
        val index = currentBanks.indexOfFirst { it.id == targetBank.id }
        if (index != -1) {
            currentBanks[index] = updatedBank
        }

        repository.saveData(currentBanks)
        Log.d("BankNotification", "Despesa $description de R$$amount salva com SUCESSO no banco ${preset.name}")
    }
}
