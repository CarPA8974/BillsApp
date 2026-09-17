package com.example.mybillsapp.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.mybillsapp.data.BillsRepository
import com.example.mybillsapp.model.BankEntity
import com.example.mybillsapp.model.BankPresets
import com.example.mybillsapp.model.BillCategory
import com.example.mybillsapp.model.BillItem
import java.util.Calendar

class BankNotificationService : NotificationListenerService() {

    private val bankPackageMap = mapOf(
        "com.nu.production" to "NUBANK",
        "com.itau" to "ITAU",
        "com.c6bank.app" to "C6_BANK",
        "br.com.intermedium" to "INTER",
        "com.santander.app" to "SANTANDER",
        "br.com.bb.android" to "BANCO_DO_BRASIL",
        "br.com.bradesco.next" to "NEXT",
        "br.com.brainweb.ifood" to "OUTRO",
        "com.mercadopago.wallet" to "MERCADO_PAGO",
        "com.picpay" to "PICPAY",
        "br.com.uol.ps.myaccount" to "PAGBANK",
        "br.com.neon" to "NEON",
        "org.kman.AquaMail" to "NUBANK", // Testes com Fake Notification
        "com.app.fake.notification" to "NUBANK", // Simuladores
        "com.whatsapp.w4b" to "NUBANK", // WhatsApp Business
        "com.whatsapp" to "NUBANK" // WhatsApp Normal
    )

    private val processedNotificationKeys = mutableSetOf<String>()

    companion object {
        fun forceRebind(context: Context) {
            requestRebind(ComponentName(context, BankNotificationService::class.java))
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            processedNotificationKeys.remove(it.key)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName

        // Ignora notificações do próprio app
        if (packageName == applicationContext.packageName) return

        val notificationKey = sbn.key
        
        // Evita processar a mesma notificação repetidamente
        if (processedNotificationKeys.contains(notificationKey)) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val ticker = sbn.notification.tickerText?.toString() ?: ""

        val rawContent = "$title $text $bigText $subText $ticker".trim()
        val content = rawContent.lowercase()

        // Identifica o banco ou assume NUBANK para testes
        val presetKey = bankPackageMap[packageName] ?: "NUBANK"

        // 1. FILTRAGEM RIGOROSA DE ENTRADA (Ignora dinheiro entrando, estornos, saldos, etc.)
        val ignoreKeywords = listOf(
            "transferência recebida", "pix recebido", "você recebeu", "recebido", "recebida",
            "dinheiro guardado", "rendimento", "promoção", "fatura fechada", "depositado",
            "pagamento recebido", "transferiu para você", "chegou um pix", "cashback",
            "estorno", "reembolso", "devolvido", "falhou", "negada", "cancelada", "recusada",
            "não aprovada", "bloqueada", "código", "erro", "saldo", "limite"
        )
        if (ignoreKeywords.any { content.contains(it) }) {
            Log.d("BankNotification", "Notificação ignorada por palavra de bloqueio.")
            return
        }

        // 2. GARANTIR QUE TEM O SÍMBOLO R$
        if (!content.contains("r$")) {
            Log.d("BankNotification", "Notificação ignorada: Sem R$.")
            return
        }

        // 3. PALAVRAS-CHAVE AMPLAS E ROBUSTAS DE DÉBITO/COMPRA/PAGAMENTO
        val validKeywords = listOf(
            "compra", "aprovada", "aprovado", "pagamento", "pago", "débito",
            "transferência", "pix", "gasto", "cartão", "efetuado", "realizado"
        )
        if (validKeywords.none { content.contains(it) }) {
            Log.d("BankNotification", "Notificação ignorada: Sem palavra de débito/pagamento.")
            return
        }

        // 4. REGEX PARA EXTRAÇÃO DE VALORES (Ex: R$ 68,00 ou R$1.500,90)
        val regex = Regex("""(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+[\.,]\d{2})""", RegexOption.IGNORE_CASE)
        val match = regex.find(rawContent)

        if (match != null) {
            val rawValue = match.groupValues[1]
            val valueStr = rawValue.replace(".", "").replace(",", ".")
            val amount = valueStr.toDoubleOrNull()
            
            if (amount == null || amount <= 0.0) return

            // 5. EXTRAÇÃO DA DESCRIÇÃO DO ESTABELECIMENTO
            val description = extrairDescricao(title, text.ifBlank { bigText })

            saveBillAutomatically(presetKey, description, amount)
            
            // Marca como processada
            processedNotificationKeys.add(notificationKey)
            if (processedNotificationKeys.size > 50) {
                val iterator = processedNotificationKeys.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }
    }

    private fun extrairDescricao(title: String, text: String): String {
        val fullContent = "$title $text"
        val compraRegex = Regex("""(?:em|no|na|para)\s+([A-Za-z0-9\s*.\-&]+?)(?=\s+para|\s+no valor|\s*$)""", RegexOption.IGNORE_CASE)
        val match = compraRegex.find(fullContent)
        
        if (match != null && match.groupValues[1].isNotBlank()) {
            var extracted = match.groupValues[1].trim()
            val cleanRegex = Regex("""(?i)^(o cartão|o crédito|crédito|débito|valor de|r\$).*""")
            if (cleanRegex.matches(extracted)) {
                 return fallbackDescricao(title)
            }
            
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
        
        val prefs = applicationContext.getSharedPreferences("mybills_prefs", MODE_PRIVATE)
        val activeMonth = prefs.getInt("active_month", Calendar.getInstance().get(Calendar.MONTH))
        val activeYear = prefs.getInt("active_year", Calendar.getInstance().get(Calendar.YEAR))
        
        val targetBank = currentBanks.find { it.presetKey == presetKey } ?: run {
            val newBank = BankEntity(
                name = preset.name,
                colorHex = preset.hexColor,
                presetKey = preset.key,
                defaultDueDay = 10
            )
            currentBanks.add(newBank)
            newBank
        }

        val capDesc = description.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val newItem = BillItem(
            description = capDesc,
            monthlyAmount = amount,
            totalAmount = amount,
            totalInstallments = 1,
            currentInstallment = 1,
            dueDay = targetBank.defaultDueDay,
            category = BillCategory.OUTROS,
            isPaid = false,
            notes = "Adicionado via notificação",
            month = activeMonth,
            year = activeYear
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
