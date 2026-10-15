package br.com.qru.transito.ui
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import br.com.qru.transito.R
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.qru.transito.QruApplication
import br.com.qru.transito.ui.search.SearchViewModel
import br.com.qru.transito.ui.search.SearchViewModelFactory
import br.com.qru.transito.ui.theme.QruTheme
import java.util.Locale
import kotlinx.coroutines.launch

enum class QruTab(val label:String){BUSCA("Busca"),CTB("CTB"),NORMAS("Normas"),SITUACOES("Situações"),NOVIDADES("Novidades")}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun QruApp(){
 QruTheme{
  var tab by rememberSaveable{mutableStateOf(QruTab.BUSCA)}
  var support by rememberSaveable{mutableStateOf<String?>(null)}
  val tabStates=rememberSaveableStateHolder()
  val drawer=rememberDrawerState(DrawerValue.Closed)
  val scope=rememberCoroutineScope()
  ModalNavigationDrawer(drawerState=drawer,drawerContent={
   ModalDrawerSheet {
    Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     QruWordmark(compact=true)
     Text("QUAL A SITUAÇÃO",style=MaterialTheme.typography.labelLarge)
    }
    NavigationDrawerItem(label={Text("Como usar")},selected=false,
     icon={Icon(Icons.Default.HelpOutline,null)},onClick={support="help";scope.launch{drawer.close()}})
    NavigationDrawerItem(label={Text("Fontes oficiais")},selected=false,
     icon={Icon(Icons.Default.MenuBook,null)},onClick={tab=QruTab.NORMAS;scope.launch{drawer.close()}})
    NavigationDrawerItem(label={Text("Sobre o QRU")},selected=false,
     icon={Icon(Icons.Default.Info,null)},onClick={support="about";scope.launch{drawer.close()}})
    TextButton(onClick={scope.launch{drawer.close()}},modifier=Modifier.padding(12.dp)){Text("FECHAR MENU")}
   }
  }){
   Scaffold(bottomBar={NavigationBar{
    QruTab.entries.forEach{item->NavigationBarItem(selected=tab==item,onClick={tab=item},
     icon={Icon(when(item){QruTab.BUSCA->Icons.Default.Search;QruTab.CTB->Icons.Default.MenuBook;QruTab.NORMAS->Icons.Default.Gavel;QruTab.SITUACOES->Icons.Default.PlayCircle;QruTab.NOVIDADES->Icons.Default.NewReleases},item.label)},
     label={Text(item.label)})}
   }}){padding->Box(Modifier.padding(padding).fillMaxSize()){
    tabStates.SaveableStateProvider(tab.name) {
     if(tab==QruTab.BUSCA) SearchHome{scope.launch{drawer.open()}} else CatalogPage(tab)
    }
   }}
  }
  support?.let { kind ->
   AlertDialog(onDismissRequest={support=null},title={Text(if(kind=="help") "Como usar" else "Sobre o QRU")},
    text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
     if(kind=="help"){
      Text("Pesquise uma palavra, artigo ou código. Toque no resultado para conferir a referência e abrir sua fonte.")
      Text("Use Descrever ocorrência para pesquisar pelos fatos. A análise guiada organiza verificações de habilitação.")
      Text("O catálogo e as normas indicadas como offline podem ser consultados sem internet. Abrir fontes oficiais requer conexão.")
     }else{
      Text("QRU? — QUAL A SITUAÇÃO. Consulta de referências de trânsito.")
      Text("Versão em desenvolvimento. As fichas e a análise guiada permanecem em conferência jurídica.")
      Text("O QRU não representa um órgão público. Consulte a fonte e a regra aplicável à data do fato.")
     }
    }},confirmButton={TextButton(onClick={support=null}){Text("ENTENDI")}})
  }
 }
}

@Composable private fun SearchHome(onMenu:()->Unit){
 val context=LocalContext.current
 val app=context.applicationContext as QruApplication
 val vm:SearchViewModel=viewModel(factory=SearchViewModelFactory(app.searchLegalContent))
 val state by vm.state.collectAsState()
 val keyboard=LocalSoftwareKeyboardController.current
 var occurrence by rememberSaveable{mutableStateOf("")}
 var occurrenceExpanded by rememberSaveable{mutableStateOf(false)}
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

 Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Box(Modifier.fillMaxWidth()) {
  Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
  QruWordmark()
  Text("QUAL A SITUAÇÃO",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
  }
   IconButton(onClick=onMenu,modifier=Modifier.align(Alignment.TopStart)){Icon(Icons.Default.Menu,"Abrir menu")}
  }
  OutlinedTextField(state.query,{vm.query(it)},Modifier.fillMaxWidth(),placeholder={Text("Artigo, código ou situação")},
   leadingIcon={IconButton(onClick={keyboard?.hide();vm.run()},enabled=state.query.isNotBlank()){Icon(Icons.Default.Search,"Executar consulta")}},
   trailingIcon={IconButton({voice()}){Icon(Icons.Default.Mic,"Pesquisa por voz")}},singleLine=true,
   keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
   keyboardActions=KeyboardActions(onSearch={keyboard?.hide();vm.run()}))
  if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  Text("Catálogo em conferência • disponível offline", style=MaterialTheme.typography.bodySmall)
  state.results.forEach{result -> ReferenceCard(result) { selected=result }}
  TextButton(onClick={occurrenceExpanded=!occurrenceExpanded},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
   Icon(Icons.Default.Description,null,modifier=Modifier.size(20.dp))
   Spacer(Modifier.width(8.dp))
   Text(if(occurrenceExpanded) "Recolher ocorrência" else "Descrever ocorrência")
  }
  if(occurrenceExpanded){
   Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     OutlinedTextField(occurrence,{occurrence=it},Modifier.fillMaxWidth(),label={Text("Descreva os fatos observados")},minLines=3)
     Button(onClick={keyboard?.hide();vm.query(occurrence);vm.run()},enabled=occurrence.isNotBlank()){Text("BUSCAR REFERÊNCIAS")}
     Text("A descrição pesquisa termos do catálogo. Confira os requisitos na fonte oficial.",style=MaterialTheme.typography.bodySmall)
    }
   }
  }
  HabilitationGuideCard { code ->
   app.bundledCatalog.all().firstOrNull { it.code==code }?.let { selected=it }
  }
  if(state.message.isNotBlank() && state.message!="Faça uma consulta à base disponível.") Text(state.message,style=MaterialTheme.typography.bodySmall)
 }
 selected?.let { ReferenceDialog(it) { selected=null } }
}

private fun openOfficialSource(context: android.content.Context, url: String) {
    val uri=Uri.parse(url)
    val host=uri.host?.lowercase(Locale.ROOT)
    val official=host?.endsWith(".gov.br")==true || host=="www2.camara.leg.br"
    if(uri.scheme!="https" || !official) return
    try { context.startActivity(Intent(Intent.ACTION_VIEW,uri)) }
    catch(_:ActivityNotFoundException) { Toast.makeText(context,"Não foi possível abrir a fonte.",Toast.LENGTH_LONG).show() }
}
@Composable private fun ReferenceCard(result:SearchResult,onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(result.title,style=MaterialTheme.typography.titleMedium)
            Text("Art. ${result.article.orEmpty()} • ${result.code.orEmpty()}",color=MaterialTheme.colorScheme.secondary)
            Text("Em conferência",style=MaterialTheme.typography.bodySmall)
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
    var query by rememberSaveable(tab){mutableStateOf("")}
    var selected by remember{mutableStateOf<SearchResult?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(tab.label,style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary)
        when(tab) {
            QruTab.CTB -> {
                CtbOfflineContents { url -> openOfficialSource(context,url) }

            }
            QruTab.NORMAS -> {
                Text("Biblioteca de normas",style=MaterialTheme.typography.titleLarge)
                OfflineNormLibrary { url -> openOfficialSource(context,url) }
                Text("A biblioteca offline está em expansão. Confira atualizações e documentos adicionais nas fontes oficiais.",style=MaterialTheme.typography.bodySmall)
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
                Text("As atualizações de conteúdo são recebidas pelo servidor e verificadas por assinatura. A disponibilidade depende da publicação de um pacote.")
            }
            else -> Unit
        }
    }
    selected?.let{ReferenceDialog(it){selected=null}}
}

@Composable private fun CtbSectionCard(title:String,description:String,onClick:()->Unit) {
 Card(onClick=onClick,modifier=Modifier.fillMaxWidth()) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
   Row(Modifier.fillMaxWidth()) {
    Text(title,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.secondary,modifier=Modifier.weight(1f))
    Icon(Icons.Default.ChevronRight,"Abrir fonte oficial online")
   }
   Text(description,style=MaterialTheme.typography.bodyMedium)
   Text("Fonte oficial • requer internet",style=MaterialTheme.typography.labelSmall)
  }
 }
}

@Composable private fun QruWordmark(compact:Boolean=false) {
 Image(
  painter=painterResource(R.mipmap.ic_launcher),
  contentDescription="QRU?",
  contentScale=ContentScale.Crop,
  modifier=Modifier.width(if(compact) 144.dp else 180.dp).height(if(compact) 48.dp else 64.dp)
 )
}
