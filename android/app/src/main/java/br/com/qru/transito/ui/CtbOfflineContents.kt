package br.com.qru.transito.ui

import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.GZIPInputStream

private data class CtbChapter(val id:String="",val title:String="",val paragraphs:List<String> = emptyList())
private data class CtbDocument(val source:String="",val retrievedOn:String="",val chapters:List<CtbChapter> = emptyList())

@Composable fun CtbOfflineContents(openSource:(String)->Unit) {
 val context=LocalContext.current
 var document by remember { mutableStateOf<CtbDocument?>(null) }
 var failed by remember { mutableStateOf(false) }
 var crimes by rememberSaveable { mutableStateOf(false) }
 var query by rememberSaveable { mutableStateOf("") }
 var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
 LaunchedEffect(Unit) {
  val result=withContext(Dispatchers.IO) { runCatching {
   val encoded=context.assets.open("ctb/chapters.json.gz.b64").bufferedReader().use { it.readText() }
   GZIPInputStream(Base64.decode(encoded,Base64.DEFAULT).inputStream()).bufferedReader().use {
    Gson().fromJson(it,CtbDocument::class.java)
   }.also { doc -> require(doc.chapters.none { it.id=="CAPÍTULO XV" }) }
  }}
  document=result.getOrNull();failed=result.isFailure
 }
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  FilterChip(selected=!crimes,onClick={crimes=false;query=""},label={Text("Parte geral")})
  FilterChip(selected=crimes,onClick={crimes=true;query=""},label={Text("Crimes de trânsito")})
 }
 if(failed) { Text("Não foi possível carregar o CTB neste aparelho.");return }
 val doc=document
 if(doc==null) { LinearProgressIndicator(Modifier.fillMaxWidth());return }
 Text("Leitura offline • fonte: Câmara dos Deputados",style=MaterialTheme.typography.labelLarge)
 Text("Cópia obtida em ${doc.retrievedOn}. Conferência de vigência e fidelidade pendente. As anotações da fonte foram preservadas.",
  style=MaterialTheme.typography.bodySmall)
 OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Capítulo, artigo ou palavra")},singleLine=true)
 val chapters=remember(doc,crimes,query) {
  doc.chapters.filter { (it.id=="CAPÍTULO XIX")==crimes }
   .filter { matchesNorm(it.title,it.paragraphs.joinToString(" "),query) }
 }
 chapters.forEach { chapter ->
  Card(onClick={selectedId=chapter.id},modifier=Modifier.fillMaxWidth()) {
   Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text(chapter.id,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
    Text(chapter.title,style=MaterialTheme.typography.titleMedium)
    Text("Toque para ler",style=MaterialTheme.typography.bodySmall)
   }
  }
 }
 if(chapters.isEmpty()) Text("Nenhum capítulo encontrado.")
 TextButton(onClick={openSource(doc.source)}) { Text("CONFERIR FONTE OFICIAL · ONLINE") }
 doc.chapters.firstOrNull { it.id==selectedId }?.let { chapter ->
  Dialog(onDismissRequest={selectedId=null},properties=DialogProperties(usePlatformDefaultWidth=false)) {
   Surface(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(chapter.title,style=MaterialTheme.typography.titleLarge)
     Text("Cópia documental offline • conferência pendente",style=MaterialTheme.typography.bodySmall)
     val listState=rememberLazyListState()
     val scope=rememberCoroutineScope()
     var readerQuery by rememberSaveable(chapter.id) { mutableStateOf(query) }
     var hit by rememberSaveable(chapter.id) { mutableStateOf(0) }
     val hits=remember(chapter.id,readerQuery) {
      if(readerQuery.isBlank()) emptyList() else chapter.paragraphs.indices.filter {
       matchesNorm("",chapter.paragraphs[it],readerQuery)
      }
     }
     LaunchedEffect(chapter.id,readerQuery) {
      hit=0
      hits.firstOrNull()?.let { listState.scrollToItem(it) }
     }
     OutlinedTextField(readerQuery,{hit=0;readerQuery=it},Modifier.fillMaxWidth(),
      label={Text("Encontrar artigo ou palavra")},singleLine=true)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      TextButton(onClick={scope.launch { listState.scrollToItem(0) }}) { Text("INÍCIO") }
      TextButton(onClick={scope.launch { listState.scrollToItem(chapter.paragraphs.lastIndex.coerceAtLeast(0)) }}) { Text("FIM") }
     }
     if(readerQuery.isNotBlank()) {
      Text(if(hits.isEmpty()) "Nenhum trecho encontrado." else "Trecho ${hit+1} de ${hits.size}",
       style=MaterialTheme.typography.bodySmall)
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
       TextButton(enabled=hit>0,onClick={hit--;scope.launch { listState.scrollToItem(hits[hit]) }}) { Text("ANTERIOR") }
       TextButton(enabled=hit<hits.lastIndex,onClick={hit++;scope.launch { listState.scrollToItem(hits[hit]) }}) { Text("PRÓXIMO") }
      }
     }
     LazyColumn(Modifier.weight(1f),state=listState,verticalArrangement=Arrangement.spacedBy(10.dp)) {
      itemsIndexed(chapter.paragraphs) { _,text ->
       SelectionContainer { Text(text,style=MaterialTheme.typography.bodyLarge) }
      }
     }
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      TextButton(onClick={openSource(doc.source)}) { Text("FONTE ONLINE") }
      TextButton(onClick={selectedId=null}) { Text("VOLTAR") }
     }
    }
   }
  }
 }
}
