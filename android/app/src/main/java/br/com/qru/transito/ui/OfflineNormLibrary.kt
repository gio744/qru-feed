package br.com.qru.transito.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

private data class OfflineNorm(
 val title:String="", val subtitle:String="", val source:String="",
 val pdfSha256:String="", val pages:List<String> = emptyList()
)
private fun normSearch(text:String):String =
 Normalizer.normalize(text.replace("ﬁ","fi").replace("ﬂ","fl"),Normalizer.Form.NFD)
 .replace(Regex("\\p{M}+"),"").lowercase(java.util.Locale.ROOT)

/** Bundled documentary text; does not mark any operational ficha as validated. */
@Composable fun OfflineNormLibrary(openSource:(String)->Unit) {
 val context=LocalContext.current
 var norm by remember { mutableStateOf<OfflineNorm?>(null) }
 var error by remember { mutableStateOf(false) }
 var pdfOpened by rememberSaveable { mutableStateOf(false) }
 var opened by rememberSaveable { mutableStateOf(false) }
 var query by rememberSaveable { mutableStateOf("") }
 LaunchedEffect(Unit) {
  val result=withContext(Dispatchers.IO) {
   runCatching {
    context.assets.open("norms/resolucao10312026.json").bufferedReader().use {
     Gson().fromJson(it,OfflineNorm::class.java)
    }.also { require(it.pages.isNotEmpty() && it.pages.first().isNotBlank()) }
   }
  }
  norm=result.getOrNull();error=result.isFailure
 }
 val document=norm
 if(error) { Text("Não foi possível carregar a norma neste aparelho.");return }
 if(document==null) { LinearProgressIndicator(Modifier.fillMaxWidth());return }
 Card(Modifier.fillMaxWidth()) {
  Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
   Text(document.title,style=MaterialTheme.typography.titleMedium)
   Text(document.subtitle,style=MaterialTheme.typography.bodyMedium)
   Text("PDF completo offline • texto pesquisável disponível",style=MaterialTheme.typography.bodySmall)
   TextButton(onClick={pdfOpened=true}) { Text("LER PDF COMPLETO") }
   TextButton(onClick={opened=true}) { Text("PESQUISAR TEXTO") }
  }
 }
 AdditionalOfflineNorms()
 if(pdfOpened) OfflinePdfReader(document.pdfSha256) { pdfOpened=false }
 if(opened) {
  val needle=normSearch(query.trim())
  val pages=remember(document,needle) {
   document.pages.mapIndexed { index,text->index to text }
    .filter { needle.isBlank() || normSearch(it.second).contains(needle) }
  }
  androidx.compose.ui.window.Dialog(onDismissRequest={opened=false},
   properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
   Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(document.title,style=MaterialTheme.typography.titleLarge)
     Text("Texto parcial extraído do documento enviado. A busca não abrange anexos em imagem. Confira tabelas e anexos no PDF completo offline. A cópia não verifica alterações posteriores.",
      style=MaterialTheme.typography.bodySmall)
     OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Buscar no documento")},singleLine=true)
     Text(if(needle.isBlank()) "${pages.size} páginas" else "${pages.size} páginas com o termo",style=MaterialTheme.typography.bodySmall)
     LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      if(pages.isEmpty()) item { Text("Nenhuma ocorrência encontrada.") }
      itemsIndexed(pages,key={_,page->page.first}) { _,page->
       Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
         Text("Página ${page.first+1}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
         SelectionContainer { Text(if(page.second.isBlank()) "Página em imagem. Abra o PDF completo offline." else page.second,style=MaterialTheme.typography.bodyMedium) }
        }
       }
      }
     }
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      TextButton(onClick={opened=false;pdfOpened=true}) { Text("PDF OFFLINE") }
      TextButton(onClick={opened=false}) { Text("VOLTAR") }
     }
    }
   }
  }
 }
}

private data class NormPdfEntry(
 val file:String="",val title:String="",val subtitle:String="",
 val sha256:String="",val pageCount:Int=0,val annexPage:Int?=null
)
@Composable private fun AdditionalOfflineNorms() {
 val context=LocalContext.current
 var documents by remember { mutableStateOf<List<NormPdfEntry>>(emptyList()) }
 var selectedFile by rememberSaveable { mutableStateOf<String?>(null) }
 var failed by remember { mutableStateOf(false) }
 LaunchedEffect(Unit) {
  val result=withContext(Dispatchers.IO) { runCatching {
   context.assets.open("norms/pdf-catalog.json").bufferedReader().use {
    Gson().fromJson(it,Array<NormPdfEntry>::class.java).toList()
   }.filter { it.file!="resolucao10312026.pdf" }
  }}
  documents=result.getOrDefault(emptyList());failed=result.isFailure
 }
 if(failed) Text("Não foi possível carregar os documentos adicionais.")
 Text("Documentos originais. Alterações e revogações posteriores precisam ser conferidas; esta biblioteca ainda não apresenta textos consolidados.",style=MaterialTheme.typography.bodySmall)
 documents.forEach { doc ->
  Card(Modifier.fillMaxWidth()) {
   Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text(doc.title,style=MaterialTheme.typography.titleMedium)
    Text(doc.subtitle,style=MaterialTheme.typography.bodyMedium)
    Text("PDF original completo offline • ${doc.pageCount} páginas",style=MaterialTheme.typography.bodySmall)
    TextButton(onClick={selectedFile=doc.file}) { Text("LER PDF COMPLETO") }
   }
  }
 }
 documents.firstOrNull { it.file==selectedFile }?.let { doc ->
  OfflinePdfReader(doc.sha256,doc.file,doc.title,doc.pageCount,doc.annexPage) { selectedFile=null }
 }
}
