package com.example.mybillsapp.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
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
        "br.com.neon" to "NEON"
    )

    private var lastProcessedNotificationTime: Long = 0
    private var lastProcessedContent: String = ""

    companion object {
        fun forceRebind(context: Context) {
            requestRebind(ComponentName(context, BankNotificationService::class.java))
        }
    }

    private fun showDiagnosticToast(message: String) {
        // Removido os pop-ups visuais para não atrapalhar o uso do celular
        Log.d("BankNotificationDiagnostic", message)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName

        // Ignora notificações do próprio app para não criar loops
        if (packageName == applicationContext.packageName) return

        // Extrai texto de TODOS os campos possíveis de notificação do Android
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val ticker = sbn.notification.tickerText?.toString() ?: ""

        val rawContent = "$title $text $bigText $subText $ticker".trim()
        val content = rawContent.lowercase()

        // Notifica o usuário na tela que QUALQUER notificação foi vista pelo leitor
        showDiagnosticToast("🔔 Notificação de [$packageName]")

        // No modo debug/teste, se não achar no mapa de bancos, assume NUBANK
        val presetKey = bankPackageMap[packageName] ?: "NUBANK"

        // PREVENÇÃO DE DUPLICATAS: Reduzido para 2 segundos para permitir testes rápidos
        val currentTime = System.currentTimeMillis()
        if (content == lastProcessedContent && (currentTime - lastProcessedNotificationTime) < 2000) {
            showDiagnosticToast("⚠️ Ignorada: Notificação duplicada recente.")
            return
        }
        lastProcessedNotificationTime = currentTime
        lastProcessedContent = content

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
        
        var targetBank = currentBanks.find { it.presetKey == presetKey }
        if (targetBank == null) {
            targetBank = BankEntity(
                name = preset.name,
                colorHex = preset.hexColor,
                presetKey = preset.key
            )
            currentBanks.add(targetBank)
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
