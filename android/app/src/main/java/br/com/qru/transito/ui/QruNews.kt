package br.com.qru.transito.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal object CurrentQruNews {
 const val id="contran-1031-2026-anexo-20261017"
 const val title="Recusa: alterações no MBFT"
 const val date="17/10/2026"
 const val sourceFile="resolucao10312026.pdf"
 const val sourceHash="9f6d8d8c6e7b7996fce21326a229c9b262d45178907fff7bd9dd159ca5ba9b5a"
 const val sourcePages=20
 const val summary="A Resolução 1.031/2026 distingue a recusa relacionada ao álcool (757-91) da recusa relacionada a outras substâncias psicoativas (757-92). As alterações do anexo do MBFT têm aplicação a partir de 17/10/2026."
}
@Composable private fun NewsText() {
 Text(CurrentQruNews.title,style=MaterialTheme.typography.titleMedium)
 Text(CurrentQruNews.date,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
 Text(CurrentQruNews.summary)
 Text("Confira o procedimento e a regra aplicável à data do fato. As fichas do QRU continuam em conferência.",style=MaterialTheme.typography.bodySmall)
 Text("Nesta versão: rolagem contínua dos PDFs, busca durante a digitação e retomada de leitura.",style=MaterialTheme.typography.bodySmall)
}
@Composable internal fun QruStartupNewsDialog(onContinue:(Boolean)->Unit,onDetails:(Boolean)->Unit) {
 var dontShowAgain by rememberSaveable(CurrentQruNews.id) { mutableStateOf(true) }
 AlertDialog(onDismissRequest={onContinue(dontShowAgain)},title={Text("Novidades do QRU")},text={
  Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   NewsText()
   Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(
    value=dontShowAgain,role=Role.Checkbox,onValueChange={dontShowAgain=it}),
    verticalAlignment=Alignment.CenterVertically) {
    Checkbox(checked=dontShowAgain,onCheckedChange=null)
    Text("Não mostrar esta novidade novamente",style=MaterialTheme.typography.bodySmall)
   }
  }
 },confirmButton={TextButton(onClick={onDetails(dontShowAgain)}){Text("VER DETALHES")}},
 dismissButton={TextButton(onClick={onContinue(dontShowAgain)}){Text("CONTINUAR")}})
}
@Composable internal fun QruLatestNewsCard() {
 var open by rememberSaveable { mutableStateOf(false) }
 Card(Modifier.fillMaxWidth()) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   NewsText()
   TextButton(onClick={open=true}) { Text("LER RESOLUÇÃO NO QRU") }
  }
 }
 if(open) OfflinePdfReader(CurrentQruNews.sourceHash,CurrentQruNews.sourceFile,
  "Resolução 1.031/2026",CurrentQruNews.sourcePages,annexPage=12) {open=false}
}
