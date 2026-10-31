package br.com.qru.transito.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
@Composable fun OfflineNormLibrary() {
 var category by rememberSaveable { mutableStateOf("Resoluções") }
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  listOf("Resoluções","Portarias","Deliberações","Manuais").forEach { name ->
   FilterChip(selected=category==name,onClick={category=name},label={Text(name)})
  }
 }
 if(category!="Resoluções" && category!="Manuais") {
  Text("Nenhum documento desta categoria foi incluído nesta versão.",style=MaterialTheme.typography.bodyMedium)
  return
 }
 val context=LocalContext.current
 var catalog by remember { mutableStateOf<List<OfflinePdfDocument>?>(null) }
 var catalogFailed by remember { mutableStateOf(false) }
 var retry by remember { mutableStateOf(0) }
 LaunchedEffect(retry) {
  catalogFailed=false
  val result=withContext(Dispatchers.IO) { runCatching {
   context.assets.open("norms/pdf-catalog.json").bufferedReader().use {
    Gson().fromJson(it,Array<OfflinePdfDocument>::class.java).toList()
   }
  } }
  catalog=result.getOrNull();catalogFailed=result.isFailure
 }
 if(catalogFailed) {
  Text("Não foi possível carregar o catálogo de PDFs.")
  TextButton(onClick={retry++}) { Text("TENTAR NOVAMENTE") }
 } else if(catalog==null) LinearProgressIndicator(Modifier.fillMaxWidth())
 val count=catalog?.count { it.category==category }
 Text(if(count==null) "$category • catálogo offline" else "$category • $count "+(if(count==1) "documento offline" else "documentos offline"),
  style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
 Text("Publicações originais. Conferência de alterações e vigência pendente.",style=MaterialTheme.typography.bodySmall)
 var catalogQuery by rememberSaveable { mutableStateOf("") }
 OutlinedTextField(catalogQuery,{catalogQuery=it},Modifier.fillMaxWidth(),label={Text("Número ou assunto da norma")},singleLine=true)
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
 if(error && category=="Resoluções") Text("A pesquisa no texto da Resolução 1.031 está indisponível. Você pode abrir o PDF completo.")
 AdditionalOfflineNorms(catalogQuery,category,catalog,document!=null,{opened=true})
 if(pdfOpened && document!=null) OfflinePdfReader(document.pdfSha256) { pdfOpened=false }
 if(opened && document!=null) {
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

@Composable private fun AdditionalOfflineNorms(query:String,category:String,documents:List<OfflinePdfDocument>?,textAvailable:Boolean,onSearchText:()->Unit) {
 var selectedFile by rememberSaveable { mutableStateOf<String?>(null) }
 val visible=remember(documents,query,category) { documents.orEmpty().filter { it.category==category && matchesNorm(it.title,it.subtitle,query) }.sortedWith(compareByDescending<OfflinePdfDocument> { normYear(it.title) }.thenByDescending { normNumber(it.title) }.thenBy { it.title }) }
 if(documents!=null && query.isNotBlank()) {
  Text("${visible.size} ${if(visible.size==1) "resultado encontrado" else "resultados encontrados"}",style=MaterialTheme.typography.labelMedium)
  if(visible.isEmpty()) Text("Nenhuma norma encontrada. Tente outro número ou assunto.")
 }
 var previousYear = -1
 visible.forEach { doc ->
  val year = normYear(doc.title)
  if(category=="Resoluções" && year!=previousYear) {
   Text(year.toString(),style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.secondary)
   previousYear=year
  }
  Card(onClick={selectedFile=doc.file},modifier=Modifier.fillMaxWidth()) {
   Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text(doc.title,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.secondary,modifier=Modifier.weight(1f))
     Icon(Icons.Default.ChevronRight,"Abrir PDF")
    }
    Text(doc.subtitle,style=MaterialTheme.typography.bodyMedium)
    if(doc.file=="resolucao10312026.pdf" && textAvailable) TextButton(onClick=onSearchText) { Text("PESQUISAR TEXTO") }
    Text("PDF original completo offline • ${doc.pageCount} páginas",style=MaterialTheme.typography.bodySmall)
   }
  }
 }
 documents.orEmpty().firstOrNull { it.file==selectedFile }?.let { doc ->
  OfflinePdfReader(doc.sha256,doc.file,doc.title,doc.pageCount,doc.annexPage) { selectedFile=null }
 }
}

internal fun matchesNorm(title:String,subtitle:String,query:String):Boolean {
 fun normalized(value:String)=normSearch(value).replace(Regex("(?<=\\d)\\.(?=\\d)"),"")
  .replace(Regex("[^a-z0-9]+")," ").trim()
 val tokens=normalized(query).split(" ").filter { it.isNotBlank() }
 val text=normalized("$title $subtitle")
 return tokens.all { text.contains(it) }
}

/** Chronological hierarchy for norms: year, then number, both descending. */
internal fun normYear(title:String):Int = Regex("(?:19|20)\\d{2}").find(title)?.value?.toIntOrNull() ?: 0
internal fun normNumber(title:String):Int {
 val match=Regex("(?:Resolução|Portaria|Deliberação)\\s*(?:CONTRAN\\s*)?(?:n[º°o.]\\s*)?([0-9][0-9.]*)",RegexOption.IGNORE_CASE).find(title)
 return match?.groupValues?.get(1)?.replace(".","")?.toIntOrNull() ?: 0
}
