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
        "org.kman.AquaMail" to "NUBANK", // Apenas para os testes com Fake Notification Apps
        "com.app.fake.notification" to "NUBANK", // Outros package names de simuladores
        "com.whatsapp.w4b" to "NUBANK" // Whatsapp Business para testes
    )

    private val processedNotificationKeys = mutableSetOf<String>()

    companion object {
        fun forceRebind(context: Context) {
            requestRebind(ComponentName(context, BankNotificationService::class.java))
        }
    }

    private fun showDiagnosticToast(message: String) {
        Log.d("BankNotificationDiagnostic", message)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            processedNotificationKeys.remove(it.key)
            Log.d("BankNotificationDiagnostic", "Notificação ${it.key} removida pelo usuário, limpando do cache.")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName

        // Ignora notificações do próprio app para não criar loops
        if (packageName == applicationContext.packageName) return

        val notificationKey = sbn.key
        
        // Se a notificação (id único fornecido pelo sistema Android) já foi processada com sucesso, ignora
        if (processedNotificationKeys.contains(notificationKey)) {
            Log.d("BankNotificationDiagnostic", "⚠️ Ignorada: Notificação já lida e processada.")
            return
        }

        // Extrai texto de TODOS os campos possíveis de notificação do Android
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val ticker = sbn.notification.tickerText?.toString() ?: ""

        val rawContent = "$title $text $bigText $subText $ticker".trim()
        val content = rawContent.lowercase()

        // No modo debug/teste, se não achar no mapa de bancos, não faz nada
        val presetKey = bankPackageMap[packageName]
        if (presetKey == null) {
            Log.d("BankNotificationDiagnostic", "⚠️ Ignorada: O pacote [$packageName] não é de um banco oficial do sistema.")
            return
        }

        // 1. Filtrar notificações indesejadas (Pix recebido, transferências, promoções)
        val ignoreKeywords = listOf(
            "transferência recebida", "pix recebido", "você recebeu", "recebido", "recebida",
            "dinheiro guardado", "rendimento", "promoção", "fatura fechada", "depositado",
            "pagamento recebido", "transferiu para você", "chegou um pix", "cashback",
            "estorno", "reembolso", "devolvido", "falhou", "negada", "cancelada", "recusada",
            "não aprovada", "bloqueada", "código", "erro"
        )
        if (ignoreKeywords.any { content.contains(it) }) {
            showDiagnosticToast("⚠️ Ignorada: Palavra de bloqueio detectada.")
            return
        }

        // 2. Garantir que tem o símbolo "R$" ou "R $ "
        if (!content.contains("r$")) {
            showDiagnosticToast("⚠️ Ignorada: Símbolo 'R$' não encontrado no texto.")
            return
        }

        // 3. Garantir que é realmente uma saída de dinheiro com palavras de "Ação"
        val validKeywords = listOf(
            "compra", "aprovada", "pagamento realizado", "pagamento aprovado", 
            "transferência enviada", "pix enviado", "pix realizado", "você pagou", 
            "você transferiu", "transferência realizada", "pix feito", "compra aprovada"
        )
        if (validKeywords.none { content.contains(it) }) {
            showDiagnosticToast("⚠️ Ignorada: Não contém palavra que confirme débito/pagamento.")
            return
        }
        // 4. Extrair valor numérico de Real R$
        val regex = Regex("""(?:R\$\s*)?(\d{1,3}(?:\.\d{3})*,\d{2}|\d+[\.,]\d{2})""", RegexOption.IGNORE_CASE)
        val match = regex.find(rawContent)

        if (match != null) {
            val rawValue = match.groupValues[1]
            val valueStr = rawValue.replace(".", "").replace(",", ".")
            val amount = valueStr.toDoubleOrNull()
            
            if (amount == null || amount <= 0.0) {
                showDiagnosticToast("⚠️ Ignorada: Valor numérico inválido ($rawValue)")
                return
            }

            // 5. Extrair descrição da notificação
            val description = extrairDescricao(title, text.ifBlank { bigText })

            saveBillAutomatically(presetKey, description, amount)
            
            // Marca a notificação como LIDA COM SUCESSO para não reprocessar no OnPosted duplo do Android
            processedNotificationKeys.add(notificationKey)
            
            // Controle de memória: se a lista crescer muito, mantém apenas as últimas 50 chaves
            if (processedNotificationKeys.size > 50) {
                val iterator = processedNotificationKeys.iterator()
                iterator.next()
                iterator.remove()
            }
        } else {
            showDiagnosticToast("⚠️ Ignorada: Valor em R$ não localizado na notificação.")
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
        
        // RECUPERA O MÊS QUE O USUÁRIO ESTÁ VISUALIZANDO ATUALMENTE NO APP!
        val prefs = applicationContext.getSharedPreferences("mybills_prefs", MODE_PRIVATE)
        val activeMonth = prefs.getInt("active_month", Calendar.getInstance().get(Calendar.MONTH))
        val activeYear = prefs.getInt("active_year", Calendar.getInstance().get(Calendar.YEAR))
        
            val targetBank = currentBanks.find { it.presetKey == presetKey } ?: run {
                val newBank = BankEntity(
                    name = preset.name,
                    colorHex = preset.hexColor,
                    presetKey = preset.key
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
        showDiagnosticToast("✅ SUCESSO! R$$amount salvo no ${preset.name}!")
        Logd("BankNotification", "Despesa $description de R$$amount salva com SUCESSO no banco ${preset.name}")
    }

    private fun Logd(tag: String, msg: String) {
        Log.d(tag, msg)
    }
}
