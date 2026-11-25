package br.com.qru.transito.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

private data class ContranEntry(val number:Int, val year:Int?, val subject:String, val source:String, val officialSituation:String?, val documentKind:String?)
private data class ContranCatalogData(val records:List<ContranEntry>)

@Composable
internal fun ContranCatalogDialog(onClose:()->Unit) {
 val context=LocalContext.current
 val uriHandler=LocalUriHandler.current
 var entries by remember { mutableStateOf<List<ContranEntry>?>(null) }
 var failed by remember { mutableStateOf(false) }
 var query by remember { mutableStateOf("") }
 var linkError by remember { mutableStateOf(false) }
 LaunchedEffect(Unit) {
  val result=withContext(Dispatchers.IO) { runCatching {
   context.assets.open("norms/contran-catalog.json").bufferedReader().use {
    Gson().fromJson(it,ContranCatalogData::class.java).records
   }.also { require(it.isNotEmpty()) }
  } }
  entries=result.getOrNull()
  failed=result.isFailure
 }
 val visible=remember(entries,query) { entries.orEmpty().filter {
  matchesNorm("Resolução ${it.number}/${it.year ?: ""}",it.subject,query)
 } }
 Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.fillMaxSize()) {
   Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("Catálogo CONTRAN",style=MaterialTheme.typography.titleLarge)
    Text("Índice documental. Vigência pendente de conferência. Os links exigem internet.")
    OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Número, ano ou assunto")},singleLine=true)
    if(failed) Text("Catálogo ampliado indisponível nesta versão.")
    else if(entries==null) LinearProgressIndicator(Modifier.fillMaxWidth())
    else Text("${visible.size} registros")
    if(linkError) Text("Não foi possível abrir o endereço oficial.")
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     items(visible) { entry ->
      Card(Modifier.fillMaxWidth()) {
       Column(Modifier.padding(12.dp)) {
        Text("Resolução ${entry.number}/${entry.year ?: "ano não informado"}")
        if(entry.documentKind=="correction") Text("Retificação")
        if(entry.documentKind=="republication") Text("Republicação")
        Text(entry.subject)
        entry.officialSituation?.takeIf { it.isNotBlank() }?.let { Text("Situação na fonte: $it") }
        TextButton(onClick={
         linkError=runCatching {
          val uri=URI(entry.source)
          require(uri.scheme=="https" && uri.host in setOf("gov.br","www.gov.br","in.gov.br","www.in.gov.br"))
          uriHandler.openUri(entry.source)
         }.isFailure
        }) { Text("ABRIR DOCUMENTO OFICIAL") }
       }
      }
     }
    }
    TextButton(onClick=onClose) { Text("VOLTAR") }
   }
  }
 }
}
