package com.example.mybillsapp.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mybillsapp.service.BankNotificationService
import com.example.mybillsapp.ui.MainViewModel
import com.example.mybillsapp.ui.components.HelpWelcomeDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    isPrivacy: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
    ) {
        // App Preferences
        item {
            Text(
                text = "Preferências do Aplicativo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Privacy Mode Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPrivacy) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Modo Privacidade", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Ocultar valores monetários na tela",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isPrivacy,
                            onCheckedChange = { viewModel.togglePrivacy() }
                        )
                    }
                }
            }
        }

        // Help Section
        item {
            Text(
                text = "Ajuda & Suporte",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Como usar o aplicativo")
                    }

                    OutlinedButton(
                        onClick = {
                            BankNotificationService.forceRebind(context)
                            Toast.makeText(context, "Serviço de Leitura Reconectado com Sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reconectar Leitor de Notificações")
                    }
                }
            }
        }

        // Cycle Management
        item {
            Text(
                text = "Gestão do Ciclo Mensal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { viewModel.advanceMonthCycle(); Toast.makeText(context, "Ciclo mensal avançado com sucesso!", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Autorenew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Virar Mês / Recarregar Faturas")
                    }
                }
            }
        }

        // Data Management & Backup
        item {
            Text(
                text = "Dados & Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export JSON
                    OutlinedButton(
                        onClick = {
                            exportText = viewModel.exportDataJson()
                            showExportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Backup (JSON)")
                    }

                    // Import JSON
                    OutlinedButton(
                        onClick = {
                            importInputText = ""
                            showImportDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar Backup (JSON)")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Clear All Data
                    Button(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apagar Todos os Dados")
                    }
                }
            }
        }

        // App Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contas a Pagar - App de Gestão Financeira", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Versão 2.1 • Desenvolvido para facilitar o controle visual das suas contas bancárias, cartões e despesas parceladas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Help Dialog
    if (showHelpDialog) {
        HelpWelcomeDialog(
            isFirstLaunch = false,
            onDismiss = { showHelpDialog = false }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Backup dos Dados", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Copie o texto abaixo para guardar seu backup:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("MyBillsBackup", exportText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Copiar Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Cole o texto JSON do seu backup abaixo:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isNotBlank()) {
                            val success = viewModel.importDataJson(importInputText)
                            if (success) {
                                Toast.makeText(context, "Dados importados com sucesso!", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            } else {
                                Toast.makeText(context, "Erro ao ler JSON. Verifique o texto colado.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Clear Confirm Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Apagar Todos os Dados?", fontWeight = FontWeight.Bold) },
            text = { Text("Esta ação é irreversível. Todos os bancos e despesas cadastrados serão apagados.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Todos os dados foram apagados.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sim, Apagar Tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
