package com.example.mybillsapp.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {
    private const val LOG_FILE_NAME = "notification_logs.txt"
    private const val TAG = "BankNotification"
    private const val MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB

    fun d(context: Context, message: String) {
        Log.d(TAG, message)
        writeToFile(context, "DEBUG", message)
    }

    fun e(context: Context, message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        val errorMessage = if (throwable != null) "$message - ${throwable.message}" else message
        writeToFile(context, "ERROR", errorMessage)
    }

    private fun writeToFile(context: Context, level: String, message: String) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)

            // Rotaciona (limpa) se passar de 2MB para não lotar o celular
            if (file.exists() && file.length() > MAX_FILE_SIZE_BYTES) {
                file.delete()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logLine = "[$timestamp] [$level] $message\n"

            val writer = FileWriter(file, true)
            writer.append(logLine)
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao escrever no arquivo de log: ${e.message}")
        }
    }

    fun getLogContent(context: Context): String {
        val file = File(context.filesDir, LOG_FILE_NAME)
        return if (file.exists()) {
            file.readText()
        } else {
            "Nenhum log de notificação registrado até o momento."
        }
    }

    fun clearLogs(context: Context) {
        val file = File(context.filesDir, LOG_FILE_NAME)
        if (file.exists()) file.delete()
    }
}
