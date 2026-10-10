package br.com.qru.transito.ui
import android.content.ActivityNotFoundException
import android.widget.Toast
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import br.com.qru.transito.domain.model.SearchResult
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.qru.transito.QruApplication
import br.com.qru.transito.ui.search.SearchViewModel
import br.com.qru.transito.ui.search.SearchViewModelFactory
import br.com.qru.transito.ui.theme.QruTheme
import java.util.Locale

enum class QruTab(val label:String){BUSCA("Busca"),CTB("CTB"),NORMAS("Normas"),SITUACOES("Situações"),NOVIDADES("Novidades")}

@Composable fun QruApp(){
 QruTheme{
  var tab by remember{mutableStateOf(QruTab.BUSCA)}
  Scaffold(bottomBar={NavigationBar{
   QruTab.entries.forEach{item->NavigationBarItem(selected=tab==item,onClick={tab=item},
    icon={Icon(when(item){QruTab.BUSCA->Icons.Default.Search;QruTab.CTB->Icons.Default.MenuBook;QruTab.NORMAS->Icons.Default.Gavel;QruTab.SITUACOES->Icons.Default.PlayCircle;QruTab.NOVIDADES->Icons.Default.NewReleases},item.label)},
    label={Text(item.label)})}
  }}){padding->Box(Modifier.padding(padding).fillMaxSize()){if(tab==QruTab.BUSCA) SearchHome() else CatalogPage(tab)}}
 }
}

@Composable private fun SearchHome(){
 val context=LocalContext.current
 val app=context.applicationContext as QruApplication
 val vm:SearchViewModel=viewModel(factory=SearchViewModelFactory(app.searchLegalContent))
 val state by vm.state.collectAsState()
 var occurrence by remember{mutableStateOf("")}
 var selected by remember{mutableStateOf<SearchResult?>(null)}
 fun voiceMessage(text:String){Toast.makeText(context,text,Toast.LENGTH_LONG).show()}
 val speech=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){r->
  if(r.resultCode==Activity.RESULT_OK) r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{vm.query(it);vm.run()}
 }
 fun launchSpeech(){try{speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
  putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
  putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault().toLanguageTag())
 })}catch(_:ActivityNotFoundException){voiceMessage("Pesquisa por voz indisponível neste celular. Use o teclado.")}catch(_:SecurityException){voiceMessage("Permita o microfone ou use o teclado.")}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)launchSpeech() else voiceMessage("Microfone não autorizado. Use o teclado.")}
 fun voice(){if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)launchSpeech() else permission.launch(Manifest.permission.RECORD_AUDIO)}

 Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("QRU?",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary)
  Text("Trânsito",style=MaterialTheme.typography.titleLarge)
  Text("Consulta operacional",color=MaterialTheme.colorScheme.secondary)
  OutlinedTextField(state.query,{vm.query(it)},Modifier.fillMaxWidth(),label={Text("Busque artigo, código ou situação")},
   trailingIcon={IconButton({voice()}){Icon(Icons.Default.Mic,"Pesquisa por voz")}},singleLine=true)
  Button({vm.run()},Modifier.fillMaxWidth()){Icon(Icons.Default.Search,null);Spacer(Modifier.width(8.dp));Text("CONSULTAR")}
  if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  Text("Referências em conferência • consulta sem internet", style=MaterialTheme.typography.bodySmall)
  state.results.forEach{result -> ReferenceCard(result) { selected=result }}
  Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.padding(16.dp)){Text("MODO OCORRÊNCIA",color=MaterialTheme.colorScheme.primary)
    OutlinedTextField(occurrence,{occurrence=it},Modifier.fillMaxWidth(),label={Text("Descreva os fatos observados")},minLines=3)
    Button(onClick={vm.query(occurrence);vm.run()},enabled=occurrence.isNotBlank()) { Text("BUSCAR REFERÊNCIAS") }
    Text("A descrição pesquisa termos do catálogo. Confira os requisitos na fonte oficial.",style=MaterialTheme.typography.bodySmall)}
  }
  Text(state.message)
 }
 selected?.let { ReferenceDialog(it) { selected=null } }
}

private fun openOfficialSource(context: android.content.Context, url: String) {
    val uri=Uri.parse(url)
    if(uri.scheme!="https" || uri.host?.endsWith(".gov.br")!=true) return
    try { context.startActivity(Intent(Intent.ACTION_VIEW,uri)) }
    catch(_:ActivityNotFoundException) { Toast.makeText(context,"Não foi possível abrir a fonte.",Toast.LENGTH_LONG).show() }
}
@Composable private fun ReferenceCard(result:SearchResult,onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(result.title,style=MaterialTheme.typography.titleMedium)
            Text("Art. ${result.article.orEmpty()} • ${result.code.orEmpty()}",color=MaterialTheme.colorScheme.secondary)
            Text("Em conferência • toque para abrir a fonte",style=MaterialTheme.typography.bodySmall)
            if(result.appliesFrom.isNotBlank() || result.appliesUntil.isNotBlank()) {
                Text("Período registrado: ${result.appliesFrom.ifBlank { "—" }} até ${result.appliesUntil.ifBlank { "sem fim informado" }}",style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
@Composable private fun ReferenceDialog(result:SearchResult,onDismiss:()->Unit) {
    val context=LocalContext.current
    AlertDialog(onDismissRequest=onDismiss,title={Text(result.title)},text={
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Art. ${result.article.orEmpty()} • código ${result.code.orEmpty()}")
            Text("Referência em conferência. A validação final da ficha está pendente.")
            Text(result.reviewStatus,style=MaterialTheme.typography.bodySmall)
            if(result.appliesFrom.isNotBlank()) Text("Início registrado no catálogo: ${result.appliesFrom}")
            if(result.appliesUntil.isNotBlank()) Text("Fim registrado no catálogo: ${result.appliesUntil}")
            Text("Confira a fonte oficial e a regra aplicável à data do fato.")
        }
    },confirmButton={result.source?.let{source->TextButton(onClick={openOfficialSource(context,source)}){Text("ABRIR FONTE OFICIAL")}}},
      dismissButton={TextButton(onClick=onDismiss){Text("VOLTAR")}})
}
@Composable private fun CatalogPage(tab:QruTab) {
    val context=LocalContext.current
    val app=context.applicationContext as QruApplication
    var query by remember(tab){mutableStateOf("")}
    var selected by remember{mutableStateOf<SearchResult?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(tab.label,style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary)
        when(tab) {
            QruTab.CTB -> {
                Text("Índice por artigo • ${app.bundledCatalog.all().size} referências em conferência")
                OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Artigo, código ou palavra")},singleLine=true)
                TextButton(onClick={openOfficialSource(context,"https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm")}){Text("CTB INTEGRAL — FONTE OFICIAL")}
                val results=if(query.isBlank()) app.bundledCatalog.all().take(50) else app.bundledCatalog.search(query)
                results.forEach{ReferenceCard(it){selected=it}}
                if(query.isBlank()) Text("Primeiras 50 referências. Use a busca para localizar as demais.")
                else if(results.isEmpty()) Text("Nenhuma referência encontrada.")
            }
            QruTab.NORMAS -> {
                Text("Fontes oficiais para conferir os documentos e a vigência.")
                Button(onClick={openOfficialSource(context,"https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-Senatran/resolucoes-contran")},modifier=Modifier.fillMaxWidth()){Text("RESOLUÇÕES CONTRAN")}
                Button(onClick={openOfficialSource(context,"https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/Resolucao9852022.pdf")},modifier=Modifier.fillMaxWidth()){Text("MBFT — RESOLUÇÃO 985/2022")}
                Text("Nas fichas, o botão de fonte abre o documento registrado para aquela referência.")
            }
            QruTab.SITUACOES -> {
                Text("Consulta por situação")
                listOf("bafômetro","recusa","CNH vencida","licenciamento","pneu","retrovisor").forEach{term->
                    OutlinedButton(onClick={query=term},modifier=Modifier.fillMaxWidth()){Text(term)}
                }
                if(query.isNotBlank()) {
                    Text("Referências para: $query")
                    app.bundledCatalog.search(query).forEach{ReferenceCard(it){selected=it}}
                }
                Text("Vídeos ainda não disponíveis.",style=MaterialTheme.typography.bodySmall)
            }
            QruTab.NOVIDADES -> {
                Text("Atualizações do QRU",style=MaterialTheme.typography.titleLarge)
                Text("10/10/2026 • ${app.bundledCatalog.all().size} referências disponíveis para busca sem internet.")
                Text("Busca por artigo, código e termos práticos. Links para conferir as fontes oficiais.")
                Text("Validação final das fichas: pendente.")
                Text("Atualizações online: aguardando configuração do servidor.")
            }
            else -> Unit
        }
    }
    selected?.let{ReferenceDialog(it){selected=null}}
}
