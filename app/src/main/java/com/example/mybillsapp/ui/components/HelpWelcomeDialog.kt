package com.example.mybillsapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpWelcomeDialog(
    isFirstLaunch: Boolean = false,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isFirstLaunch) "👋 Bem-vindo ao Contas a Pagar!" else "Como usar o aplicativo",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("1. Adicionar Bancos e Cartões", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Na tela 'Início', toque no botão flutuante 'Novo Banco' para cadastrar sua instituição. Você pode usar uma predefinição rápida ou criar uma cor personalizada.", style = MaterialTheme.typography.bodySmall)

                Text("2. Leitura Automática de Notificações", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Para que suas compras de cartão e pagamentos sejam cadastrados automaticamente sem você precisar digitar nada, é fundamental permitir a leitura de notificações! Se o ícone de sino vermelho aparecer no topo da tela, toque nele e habilite a permissão.", style = MaterialTheme.typography.bodySmall)

                Text("3. Cadastrar Despesas Manualmente", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Abra o banco desejado e clique em 'Adicionar Despesa'. Você pode inserir o valor total da compra e a quantidade de parcelas, e o app dividirá o valor pra você.", style = MaterialTheme.typography.bodySmall)

                Text("4. Faturas Pagas e Pendentes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Ao clicar no check ao lado da despesa, ela é marcada como 'Paga'. O valor do resumo 'Falta Pagar' e do saldo do Banco vai diminuir instantaneamente, ajudando você a focar apenas no que ainda falta quitar.", style = MaterialTheme.typography.bodySmall)

                Text("5. Virar Mês / Recarregar Faturas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Ao chegar em um novo mês, use o botão 'Virar Mês' no topo da tela 'Início' ou nas Configurações para renovar suas faturas e avançar as parcelas automaticamente.", style = MaterialTheme.typography.bodySmall)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("❤️ Agradecimento", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Muito obrigado por escolher o nosso aplicativo! Desenvolvemos esta ferramenta com muito carinho para simplificar sua vida e te dar controle financeiro total. Conte conosco!", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isFirstLaunch) "Começar a Usar!" else "Entendi")
            }
        }
    )
}
