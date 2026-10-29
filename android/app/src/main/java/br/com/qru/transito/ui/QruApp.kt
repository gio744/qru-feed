package br.com.qru.transito.ui
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import br.com.qru.transito.R
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import br.com.qru.transito.domain.model.SearchResult
import br.com.qru.transito.domain.model.OperationalReferencePublication
import br.com.qru.transito.data.repository.OperationalFichePackage
import br.com.qru.transito.data.repository.QruVideoCatalog
import br.com.qru.transito.data.repository.QruVideo
import br.com.qru.transito.data.repository.QruStudioOffline
import br.com.qru.transito.data.repository.QruStudioDownload
import br.com.qru.transito.domain.model.OperationalReference
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson

enum class QruTab(val label:String){BUSCA("Busca"),CTB("CTB"),NORMAS("Normas"),SITUACOES("Situações"),NOVIDADES("Novidades"),MAIS("Mais")}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun QruApp(){
 QruTheme{
  var tab by rememberSaveable{mutableStateOf(QruTab.BUSCA)}
  var homeVisible by rememberSaveable { mutableStateOf(true) }
  var quickQuery by rememberSaveable { mutableStateOf("") }
  fun openHome() { tab=QruTab.BUSCA;homeVisible=true;quickQuery="" }
  fun openSearch(query:String="") { tab=QruTab.BUSCA;quickQuery=query;homeVisible=false }
  var support by rememberSaveable{mutableStateOf<String?>(null)}
  val newsContext=LocalContext.current
  val newsPreferences=remember(newsContext) { newsContext.getSharedPreferences("qru_news",android.content.Context.MODE_PRIVATE) }
  fun seenNews():Set<String> = runCatching { newsPreferences.getStringSet("seen_publications",emptySet()).orEmpty().toSet() }.getOrDefault(emptySet())
  var showStartupNews by rememberSaveable(CurrentQruNews.id) { mutableStateOf(shouldShowStartupNews(CurrentQruNews.id,seenNews())) }
  fun dismissStartupNews(dontShowAgain:Boolean) {
   if(dontShowAgain) newsPreferences.edit().putStringSet("seen_publications",seenNewsAfterDismissal(seenNews(),CurrentQruNews.id,true)).apply()
   showStartupNews=false
  }
  val tabStates=rememberSaveableStateHolder()
  val drawer=rememberDrawerState(DrawerValue.Closed)
  val scope=rememberCoroutineScope()
  ModalNavigationDrawer(drawerState=drawer,drawerContent={
   ModalDrawerSheet {
    Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     QruWordmark(compact=true)
     Text("QUAL A SITUAÇÃO",style=MaterialTheme.typography.labelLarge)
    }
    NavigationDrawerItem(label={Text("Busca")},selected=tab==QruTab.BUSCA,icon={Icon(Icons.Default.Search,null)},onClick={openHome();scope.launch{drawer.close()}})
    NavigationDrawerItem(label={Text("Como usar")},selected=false,
     icon={Icon(Icons.Default.HelpOutline,null)},onClick={support="help";scope.launch{drawer.close()}})
    NavigationDrawerItem(label={Text("Fontes oficiais")},selected=false,
     icon={Icon(Icons.Default.MenuBook,null)},onClick={tab=QruTab.NORMAS;scope.launch{drawer.close()}})
    NavigationDrawerItem(label={Text("Novidades")},selected=false,
     icon={Icon(Icons.Default.Notifications,null)},onClick={tab=QruTab.NOVIDADES;scope.launch{drawer.close()}})
    NavigationDrawerItem(label={Text("Sobre o QRU")},selected=false,
     icon={Icon(Icons.Default.Info,null)},onClick={support="about";scope.launch{drawer.close()}})
    TextButton(onClick={scope.launch{drawer.close()}},modifier=Modifier.padding(12.dp)){Text("FECHAR MENU")}
   }
  }){
   Scaffold(bottomBar={NavigationBar{
    listOf(QruTab.CTB,QruTab.NORMAS,QruTab.MAIS,QruTab.SITUACOES,QruTab.NOVIDADES).forEach{item->NavigationBarItem(selected=tab==item,onClick={tab=item;homeVisible=false},modifier=Modifier.testTag("tab_"+item.name).semantics { contentDescription = if(item==QruTab.MAIS) "Abrir central QRU" else item.label },
     icon={if(item==QruTab.MAIS) Surface(shape=RoundedCornerShape(50),color=androidx.compose.ui.graphics.Color(0xFF081D32),border=androidx.compose.foundation.BorderStroke(2.dp,androidx.compose.ui.graphics.Color(0xFF53BDF7)),modifier=Modifier.size(49.dp)) { Box(contentAlignment=Alignment.Center) { Text("Q",style=MaterialTheme.typography.headlineMedium.copy(fontWeight=FontWeight.Black,brush=androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.White,androidx.compose.ui.graphics.Color(0xFFB7C9D9),androidx.compose.ui.graphics.Color(0xFF6B91AC),androidx.compose.ui.graphics.Color(0xFFE4F7FF))))) } } else Icon(when(item){QruTab.CTB->Icons.Default.MenuBook;QruTab.NORMAS->Icons.Default.Gavel;QruTab.SITUACOES->Icons.Default.PlayCircle;QruTab.MAIS->Icons.Default.GridView;QruTab.NOVIDADES->Icons.Default.Notifications;else->Icons.Default.Search},item.label)},
     label={Text(if(item==QruTab.MAIS) "QRU" else item.label)})}
   }}){padding->Box(Modifier.padding(padding).fillMaxSize()){
    tabStates.SaveableStateProvider(tab.name) {
     Column(Modifier.fillMaxSize()) {
      if(tab!=QruTab.BUSCA || !homeVisible) {
       TextButton(onClick={openHome()},modifier=Modifier.testTag("return_home")) {
        Icon(Icons.Default.Home,null)
        Spacer(Modifier.width(8.dp))
        Text("Página inicial")
       }
      }
      Box(Modifier.weight(1f)) {
       if(tab==QruTab.BUSCA && homeVisible) QruOperationalHome(onMenu={scope.launch{drawer.open()}},onSearch={openSearch(it)})
       else if(tab==QruTab.BUSCA) SearchHome(initialQuery=quickQuery,onMenu={scope.launch{drawer.open()}},openReferenceDirectly=quickQuery.isNotBlank())
       else if(tab==QruTab.MAIS) MoreLibraryPage(onOpenNorms={tab=QruTab.NORMAS})
       else CatalogPage(tab)
      }
     }
    }
   }}
  }
  if(showStartupNews) QruStartupNewsDialog(
   onContinue={dismissStartupNews(it)},
   onDetails={dismissStartupNews(it);tab=QruTab.NOVIDADES}
  )
  support?.let { kind ->
   AlertDialog(onDismissRequest={support=null},title={Text(if(kind=="help") "Como usar" else "Sobre o QRU")},
    text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
     if(kind=="help"){
      Text("Pesquise uma palavra, artigo ou código. Toque no resultado para conferir a referência e abrir sua fonte.")
      Text("Use Descrever ocorrência para pesquisar pelos fatos. A análise guiada organiza verificações de habilitação.")
      Text("As fontes incluídas nesta versão podem ser lidas dentro do QRU sem internet. As demais publicações abrem no navegador e exigem conexão.")
     }else{
      Text("QRU? — QUAL A SITUAÇÃO. Consulta de referências de trânsito.")
      Text("Versão em desenvolvimento. As fichas e a análise guiada permanecem em conferência jurídica.")
      Text("O QRU não representa um órgão público. Consulte a fonte e a regra aplicável à data do fato.")
     }
    }},confirmButton={TextButton(onClick={support=null}){Text("ENTENDI")}})
  }
 }
}

@Composable private fun QruOperationalHome(onMenu:()->Unit,onSearch:(String)->Unit) {
 val context=LocalContext.current
 val prefs=remember(context) { context.getSharedPreferences("qru_home",android.content.Context.MODE_PRIVATE) }
 val options=listOf("230 V","167","162 I","165-A","230 XVIII","208","252","181")
 val titles=mapOf("230 V" to "Licenciamento vencido","167" to "Cinto de segurança","162 I" to "Sem habilitação","165-A" to "Recusa ao teste","230 XVIII" to "Pneus e rodas","208" to "Avanço de sinal vermelho","252" to "Condutas do condutor","181" to "Estacionamento irregular")
 var favorites by remember { mutableStateOf(prefs.getString("favorites","230 V|167|162 I|165-A").orEmpty().split("|").filter { it in options }.distinct().take(4).ifEmpty { listOf("230 V","167","162 I","165-A") }) }
 var customizing by rememberSaveable { mutableStateOf(false) }
 val blue=androidx.compose.ui.graphics.Color(0xFF32B8FF)
 val dark=androidx.compose.ui.graphics.Color(0xFF061B2E)
 val border=androidx.compose.ui.graphics.Color(0xFF277BB1)
 LazyColumn(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFF071729),androidx.compose.ui.graphics.Color(0xFF020C16),androidx.compose.ui.graphics.Color(0xFF020B13)))).testTag("operational_home"),contentPadding=PaddingValues(horizontal=14.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item {
   Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
     IconButton(onClick=onMenu) { Icon(Icons.Default.Menu,"Abrir menu",tint=androidx.compose.ui.graphics.Color.White) }
     Icon(Icons.Default.Notifications,"Novidades",tint=blue,modifier=Modifier.padding(end=12.dp))
    }
    Text("QRU",modifier=Modifier.testTag("home_metallic_wordmark"),style=MaterialTheme.typography.displayLarge.copy(fontWeight=FontWeight.Black,brush=androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFFFFFFFF),androidx.compose.ui.graphics.Color(0xFFB8C9D8),androidx.compose.ui.graphics.Color(0xFF6886A0),androidx.compose.ui.graphics.Color(0xFFE2F4FF)))),maxLines=1)
    Text("T R Â N S I T O",color=androidx.compose.ui.graphics.Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(8.dp))
    Text("Q U A L   A   S I T U A Ç Ã O ?",color=blue,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.labelMedium)
   }
  }
  item {
   OutlinedButton(onClick={onSearch("")},modifier=Modifier.fillMaxWidth().heightIn(min=62.dp).testTag("home_search"),shape=RoundedCornerShape(18.dp),border=androidx.compose.foundation.BorderStroke(1.5.dp,blue),colors=ButtonDefaults.outlinedButtonColors(containerColor=dark)) {
    Icon(Icons.Default.Search,null,tint=androidx.compose.ui.graphics.Color.White)
    Spacer(Modifier.width(10.dp))
    Text("Buscar artigo, infração, código ou situação...",modifier=Modifier.weight(1f),color=androidx.compose.ui.graphics.Color(0xFFB3D5F6),style=MaterialTheme.typography.bodySmall)
    Icon(Icons.Default.Mic,null,tint=blue)
   }
  }
  item {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
    Text("MEU PAINEL OPERACIONAL",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=androidx.compose.ui.graphics.Color.White)
    TextButton(onClick={customizing=!customizing}) {
     Icon(Icons.Default.Edit,null,tint=blue,modifier=Modifier.size(16.dp))
     Spacer(Modifier.width(4.dp))
     Text(if(customizing) "CONCLUIR" else "PERSONALIZAR",color=blue,style=MaterialTheme.typography.labelSmall)
    }
   }
  }
  if(customizing) item {
   Column {
    Text("Escolha até quatro atalhos. A seleção fica salva neste aparelho.",style=MaterialTheme.typography.bodySmall)
    options.forEach { code ->
     Row(verticalAlignment=Alignment.CenterVertically) {
      Checkbox(checked=code in favorites,onCheckedChange={checked ->
       favorites=if(checked) (favorites+code).distinct().take(4) else favorites.filterNot { it==code }
       prefs.edit().putString("favorites",favorites.joinToString("|")).apply()
      },enabled=code in favorites || favorites.size<4)
      Text("Art. $code — ${titles[code].orEmpty()}")
     }
    }
   }
  }
  items(favorites.chunked(2)) { pair ->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    pair.forEach { code ->
     OutlinedCard(onClick={onSearch(code)},modifier=Modifier.weight(1f).heightIn(min=86.dp).testTag("favorite_"+code.replace(" ","_")),shape=RoundedCornerShape(12.dp),border=androidx.compose.foundation.BorderStroke(1.dp,border),colors=CardDefaults.outlinedCardColors(containerColor=dark)) {
      Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
       Surface(shape=RoundedCornerShape(10.dp),color=androidx.compose.ui.graphics.Color(0xFF124573),modifier=Modifier.size(44.dp)) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Default.Gavel,null,tint=androidx.compose.ui.graphics.Color(0xFFD5E9FF),modifier=Modifier.size(27.dp)) } }
       Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("Art. ${code.replace(" ", ", ")}",fontWeight=FontWeight.Bold,color=androidx.compose.ui.graphics.Color.White,style=MaterialTheme.typography.titleSmall)
        Text(titles[code].orEmpty(),style=MaterialTheme.typography.labelSmall,color=androidx.compose.ui.graphics.Color(0xFFB9D0E8),maxLines=2)
       }
       Icon(Icons.Default.ChevronRight,null,tint=blue,modifier=Modifier.size(16.dp))
      }
     }
    }
    if(pair.size==1) Spacer(Modifier.weight(1f))
   }
  }
  item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text("CONSULTAS FREQUENTES",style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=androidx.compose.ui.graphics.Color.White); TextButton(onClick={customizing=true}) { Icon(Icons.Default.Settings,null,tint=blue,modifier=Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("GERENCIAR",color=blue,style=MaterialTheme.typography.labelSmall) } } }
  item {
   // Keep frequent consultations in one horizontally scrollable row, leaving the
   // operational dashboard visible without forcing an extra vertical row.
   Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("frequent_consultations"),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    options.forEach { code ->
     OutlinedButton(onClick={onSearch(code)},modifier=Modifier.height(42.dp).testTag("frequent_"+code.replace(" ","_")),shape=RoundedCornerShape(10.dp),border=androidx.compose.foundation.BorderStroke(1.dp,border),contentPadding=PaddingValues(horizontal=14.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=dark)) {
      Text(code,style=MaterialTheme.typography.labelSmall,color=androidx.compose.ui.graphics.Color(0xFFB8D9FF),maxLines=1)
     }
    }
    OutlinedButton(onClick={customizing=true},modifier=Modifier.height(42.dp).testTag("frequent_more"),shape=RoundedCornerShape(10.dp),border=androidx.compose.foundation.BorderStroke(1.dp,border),contentPadding=PaddingValues(horizontal=14.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=dark)) { Icon(Icons.Default.MoreHoriz,"Gerenciar consultas frequentes",tint=blue) }
   }
  }
 }
}

@Composable private fun SearchHome(initialQuery:String="",onMenu:()->Unit,openReferenceDirectly:Boolean=false){
 val context=LocalContext.current
 val app=context.applicationContext as QruApplication
 val vm:SearchViewModel=viewModel(factory=SearchViewModelFactory(app.searchLegalContent))
 val state by vm.state.collectAsState()
 LaunchedEffect(initialQuery) { if(initialQuery.isNotBlank()) { vm.query(initialQuery);vm.run() } }
 val keyboard=LocalSoftwareKeyboardController.current
 var occurrence by rememberSaveable{mutableStateOf("")}
 var occurrenceExpanded by rememberSaveable{mutableStateOf(false)}
 var selected by remember{mutableStateOf<SearchResult?>(null)}
 var shortcutHandled by remember(initialQuery,openReferenceDirectly) { mutableStateOf(false) }
 var shortcutNoExactMatch by remember(initialQuery,openReferenceDirectly) { mutableStateOf(false) }
 LaunchedEffect(state.loading,state.results,initialQuery,openReferenceDirectly) {
  if(openReferenceDirectly && !shortcutHandled && initialQuery.isNotBlank() && !state.loading && state.query==initialQuery) {
   // Never open an unrelated legal fiche when a shortcut has no exact match.
   // For article-only shortcuts, require an exact article; for inciso shortcuts,
   // require that the inciso is explicitly present in the indexed reference.
   val normalizedQuery=initialQuery.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"),"")
   val matching=state.results.firstOrNull { result ->
    val article=result.article.orEmpty().uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"),"")
    article==normalizedQuery
   }
   if(matching!=null) { selected=matching;shortcutHandled=true;shortcutNoExactMatch=false }
   else { shortcutHandled=true;shortcutNoExactMatch=true }
  }
 }
 var guideReference by remember { mutableStateOf<String?>(null) }
 var guideExpanded by rememberSaveable { mutableStateOf(false) }
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

 LazyColumn(Modifier.fillMaxSize().testTag("search_results"),contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item(key="search_header") {
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Box(Modifier.fillMaxWidth()) {
  Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
  QruWordmark()
  Text("QUAL A SITUAÇÃO",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
  }
   IconButton(onClick=onMenu,modifier=Modifier.align(Alignment.TopStart)){Icon(Icons.Default.Menu,"Abrir menu")}
  }
  OutlinedTextField(state.query,{vm.query(it)},Modifier.fillMaxWidth().testTag("search_query"),placeholder={Text("Artigo, código ou situação")},
   leadingIcon={IconButton(onClick={keyboard?.hide();vm.run()},enabled=state.query.isNotBlank()){Icon(Icons.Default.Search,"Executar consulta")}},
   trailingIcon={IconButton({voice()}){Icon(Icons.Default.Mic,"Pesquisa por voz")}},singleLine=true,
   keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),
   keyboardActions=KeyboardActions(onSearch={keyboard?.hide();vm.run()}))
  if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  if(shortcutNoExactMatch && state.query==initialQuery) Text("Não foi localizada ficha com correspondência exata para Art. $initialQuery. Confira os resultados abaixo antes de abrir uma referência.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("shortcut_exact_match_warning"))
  Text("Catálogo em conferência • disponível offline", style=MaterialTheme.typography.bodySmall)
   }
  }
  items(state.results,key={result -> "reference:"+result.stableKey},contentType={"reference"}) { result ->
   ReferenceCard(result) { selected=result }
  }
  item(key="search_tools") {
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
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
  HabilitationGuideCard { guideExpanded=true }
  if(state.message.isNotBlank() && state.message!="Faça uma consulta à base disponível.") Text(state.message,style=MaterialTheme.typography.bodySmall)
   }
  }
 }
 HabilitationGuideDialog(guideExpanded,onDismiss={guideExpanded=false}) { code -> guideReference=code }
 guideReference?.let { code ->
  GuideReferenceLookup(code,search={app.searchLegalContent(it)},
   onSelected={selected=it;guideReference=null},onDismiss={guideReference=null})
 }
 selected?.let { ReferenceDialog(it,autoOpenOffline=openReferenceDirectly) { selected=null } }
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
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag("reference_card")) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(result.title,style=MaterialTheme.typography.titleMedium)
            Text("Art. ${result.article.orEmpty()} • ${result.code.orEmpty()}",color=MaterialTheme.colorScheme.secondary)
            Text("Em conferência",style=MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable private fun ReferenceDialog(result:SearchResult,autoOpenOffline:Boolean=false,onDismiss:()->Unit) {
    val context=LocalContext.current
    var documents by remember { mutableStateOf<List<OfflinePdfDocument>>(emptyList()) }
    var ranges by remember { mutableStateOf<List<ReferencePageRange>>(emptyList()) }
    var referenceLoading by remember { mutableStateOf(true) }
    var offlineOpened by rememberSaveable(result.stableKey) { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val loaded=withContext(Dispatchers.IO) {
            val docs=runCatching {
                context.assets.open("norms/pdf-catalog.json").bufferedReader().use {
                    Gson().fromJson(it,Array<OfflinePdfDocument>::class.java).toList()
                }
            }.getOrDefault(emptyList())
            val pages=runCatching {
                context.assets.open("norms/reference-page-ranges.json").bufferedReader().use {
                    Gson().fromJson(it,Array<ReferencePageRange>::class.java).toList()
                }
            }.getOrDefault(emptyList())
            docs to pages
        }
        documents=loaded.first;ranges=loaded.second;referenceLoading=false
    }
    val offline=result.source?.let { resolveOfflineReference(documents,ranges,it,result.code) }
    // Shortcut navigation opens only the verified, locally indexed PDF range.
    // Never redirect to a website or open a different article as a fallback.
    LaunchedEffect(autoOpenOffline,referenceLoading,offline,result.stableKey) {
        if(autoOpenOffline && !referenceLoading && offline!=null) offlineOpened=true
    }
    // Editorial package starts empty. Never infer approval from a bundled PDF or search result.
    var approvedOperationalFiche by remember(result.stableKey) { mutableStateOf<OperationalReference?>(null) }
    LaunchedEffect(result.stableKey) {
        approvedOperationalFiche = withContext(Dispatchers.IO) {
            runCatching {
                val json = context.assets.open("operational/approved-fiches.json")
                    .bufferedReader().use { it.readText() }
                // Recheck the selected result against the active catalog before publication.
                val code = result.code ?: return@runCatching null
                val expectedDigest = context.assets.open("operational/approved-fiches.sha256")
                    .bufferedReader().use { it.readText().trim() }
                val app = context.applicationContext as QruApplication
                if (!app.legalRepository.isActiveReference(result)) return@runCatching null
                OperationalFichePackage.approvedByCodeWithDigest(json, setOf(code), expectedDigest)[code]
            }.getOrNull()
        }
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text(result.title)},text={
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Art. ${result.article.orEmpty()} • código ${result.code.orEmpty()}")
            if (approvedOperationalFiche == null) Text("Referência em conferência. A validação final da ficha está pendente.")
            Text("QUANDO AUTUAR",style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary)
            if (approvedOperationalFiche == null) Text("Orientação operacional ainda não aprovada. Consulte a ficha oficial antes de decidir.")
            else approvedOperationalFiche?.whenApplicable?.forEach { item ->
                Text(item.text)
                item.evidence.forEach { source ->
                    TextButton(onClick={ openOfficialSource(context, source.url) }) {
                        Text("Fonte oficial: ${source.locator}", style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("QUANDO NÃO AUTUAR / EXCEÇÕES",style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary)
            if (approvedOperationalFiche == null) Text("Exceções e hipóteses de não enquadramento pendentes de revisão técnica.")
            else approvedOperationalFiche?.exceptions?.forEach { item ->
                Text(item.text)
                item.evidence.forEach { source ->
                    TextButton(onClick={ openOfficialSource(context, source.url) }) {
                        Text("Fonte oficial: ${source.locator}", style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text("MEDIDAS ADMINISTRATIVAS",style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.primary)
            if (approvedOperationalFiche == null) Text("Retenção, remoção e regularização dependem da norma aplicável e de conferência editorial.")
            else approvedOperationalFiche?.measures?.forEach { item ->
                Text(item.rule.text)
                item.rule.evidence.forEach { source ->
                    TextButton(onClick={ openOfficialSource(context, source.url) }) {
                        Text("Fonte oficial: ${source.locator}", style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(result.reviewStatus,style=MaterialTheme.typography.bodySmall)
            if(result.appliesFrom.isNotBlank()) Text("Início registrado no catálogo: ${result.appliesFrom}")
            if(result.appliesUntil.isNotBlank()) Text("Fim registrado no catálogo: ${result.appliesUntil}")
            Text("Confira a fonte oficial e a regra aplicável à data do fato.")
            if(offline!=null) {
                Text("Ficha individual da publicação original • ${offline.range.last-offline.range.first+1} páginas. Confira também alterações posteriores.",style=MaterialTheme.typography.bodySmall)
                result.source?.let { source -> TextButton(onClick={openOfficialSource(context,source)}) { Text("VER PUBLICAÇÃO ONLINE") } }
            } else if(!referenceLoading) Text("Ficha individual ainda indisponível nesta versão. A opção abaixo abre a fonte completa.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={
        if(referenceLoading) TextButton(onClick={},enabled=false){Text("CARREGANDO")}
        else if(offline!=null) TextButton(onClick={offlineOpened=true}) { Text("ABRIR FICHA") }
        else result.source?.let{source->TextButton(onClick={openOfficialSource(context,source)}){Text("ABRIR FONTE OFICIAL")}}
      },dismissButton={TextButton(onClick=onDismiss){Text("VOLTAR")}})
    if(offlineOpened && offline!=null) {
        val doc=offline.document
        OfflinePdfReader(doc.sha256,doc.file,"Ficha ${result.code} • art. ${result.article}",doc.pageCount,initialPage=offline.range.first,pageRange=offline.range) { offlineOpened=false }
    }
}
@Composable private fun CatalogPage(tab:QruTab) {
    val context=LocalContext.current
    val app=context.applicationContext as QruApplication
    if(tab==QruTab.SITUACOES) { SituationsPage();return }
    if(tab==QruTab.MAIS) return
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
            QruTab.NOVIDADES -> {
                QruLatestNewsCard()
                Text("Recursos desta versão",style=MaterialTheme.typography.titleLarge)
                Text("Catálogo • ${app.bundledCatalog.all().size} referências disponíveis para busca sem internet.")
                Text("Busca por artigo, código e termos práticos. Links para conferir as fontes oficiais.")
                Text("Validação final das fichas: pendente.")
                Text("As atualizações de conteúdo são recebidas pelo servidor e verificadas por assinatura. A disponibilidade depende da publicação de um pacote.")
            }
            else -> Unit
        }
    }
}


@Composable private fun SituationsPage() {
 val context=LocalContext.current
 val app=context.applicationContext as QruApplication
 val vm:SearchViewModel=viewModel(key="situations_search",factory=SearchViewModelFactory(app.searchLegalContent))
 val state by vm.state.collectAsState()
 var selected by remember { mutableStateOf<SearchResult?>(null) }
 val studioVideos=remember(context) { runCatching { context.assets.open("studio/videos.json").bufferedReader().use { QruVideoCatalog.published(it.readText()) } }.getOrDefault(emptyList()) }
 LazyColumn(Modifier.fillMaxSize().testTag("situations_results"),
  contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item(key="situations_header") {
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    Text("Situações",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary)
    Text("Consulta por situação")
    listOf("bafômetro","recusa","CNH vencida","licenciamento","pneu","retrovisor").forEach { term ->
     OutlinedButton(onClick={vm.query(term);vm.run()},
      modifier=Modifier.fillMaxWidth().testTag("situation_"+term)) { Text(term) }
    }
    if(state.query.isNotBlank()) Text("Referências para: ${state.query}")
    if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("situations_loading"))
   }
  }
  items(state.results,key={ "situation:"+it.stableKey },contentType={ "reference" }) { result ->
   ReferenceCard(result) { selected=result }
  }
  item(key="situations_footer") {
   Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
    if(state.query.isNotBlank() && !state.loading && state.results.isEmpty()) {
     Text(state.message,style=MaterialTheme.typography.bodySmall)
     TextButton(onClick={vm.run()}) { Text("TENTAR NOVAMENTE") }
    }
    Text("QRU Studio",style=MaterialTheme.typography.titleLarge)
    if(studioVideos.isEmpty()) Text("Vídeos em preparação editorial. Nenhum vídeo aprovado para publicação nesta versão.",style=MaterialTheme.typography.bodySmall)
    studioVideos.forEach { video ->
     Card(modifier=Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
      Text(video.title,style=MaterialTheme.typography.titleMedium)
      Text(video.description,style=MaterialTheme.typography.bodyMedium)
      Text("Referência: ${video.legalReference} • Revisado em ${video.reviewedAt}",style=MaterialTheme.typography.bodySmall)
      StudioVideoActions(video)
     } }
    }
   }
  }
 }
 selected?.let { ReferenceDialog(it) { selected=null } }
}


@Composable private fun StudioVideoActions(video: QruVideo) {
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 var downloading by remember(video.id) { mutableStateOf(false) }
 var verified by remember(video.id) { mutableStateOf(QruStudioOffline.verifiedFile(context,video)!=null) }
 var feedback by remember(video.id) { mutableStateOf<String?>(null) }
 Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  TextButton(onClick={openOfficialSource(context,video.youtubeUrl ?: video.videoUrl)}) { Text("ASSISTIR ONLINE") }
  if(verified) {
   TextButton(onClick={
    val file=QruStudioOffline.verifiedFile(context,video)
    if(file==null) { verified=false; feedback="Arquivo offline inválido. Baixe novamente." }
    else {
     val uri=androidx.core.content.FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
     val intent=Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri,"video/mp4");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
     try { context.startActivity(intent) } catch (_:ActivityNotFoundException) { feedback="Nenhum reprodutor de vídeo disponível." }
    }
   }) { Text("VER OFFLINE") }
  } else {
   TextButton(enabled=!downloading,onClick={
    downloading=true
    scope.launch {
     val ok=QruStudioDownload.fetch(context,video)
     verified=ok && QruStudioOffline.verifiedFile(context,video)!=null
     feedback=if(verified) "Vídeo salvo e verificado." else "Download não concluído ou arquivo inválido."
     downloading=false
    }
   }) { Text(if(downloading) "BAIXANDO..." else "BAIXAR") }
  }
 }
 feedback?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
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
  painter=painterResource(R.drawable.qru_wordmark),
  contentDescription="QRU?",
  contentScale=ContentScale.Fit,
  modifier=Modifier.width(if(compact) 144.dp else 180.dp).height(if(compact) 48.dp else 64.dp)
 )
}


/** Biblioteca especializada: entradas em preparação não simulam conteúdo publicado. */
@Composable private fun MoreLibraryPage(onOpenNorms:()->Unit) {
 val context=LocalContext.current
 var showAlcoolemia by rememberSaveable { mutableStateOf(false) }
 val topics=listOf(
  "MBFT" to "Manual Brasileiro de Fiscalização",
  "Sinalização" to "Placas e marcas viárias",
  "Equipamentos" to "Requisitos e irregularidades",
  "Alcoolemia" to "Testes e procedimentos",
  "Guias operacionais" to "Orientações de fiscalização",
  "Habilitação" to "CNH, PPD e categorias",
  "Engate" to "Requisitos técnicos",
  "Dicionário" to "Termos de trânsito",
  "Crimes" to "Crimes de trânsito",
  "Sinistros" to "Providências administrativas",
  "Licenciamento" to "Documentos e regularização",
  "Velocidade" to "Limites e fiscalização",
  "Acordos" to "Regras internacionais",
  "Anotações" to "Recurso futuro"
 )
 Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(16.dp),
  verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Central QRU",style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
  Text("Biblioteca especializada • conteúdo em expansão",style=MaterialTheme.typography.bodySmall)
  topics.chunked(2).forEach { pair ->
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()) {
    pair.forEach { (title,description) ->
     Card(onClick={
       when(title) {
        "MBFT" -> openOfficialSource(context,"https://www.gov.br/transportes/pt-br/assuntos/transito/conteudo-contran/resolucoes/Resolucao9852022.pdf")
        "Alcoolemia" -> showAlcoolemia=true
        "Sinalização" -> onOpenNorms()
       }
      },enabled=(title=="MBFT" || title=="Alcoolemia" || title=="Sinalização"),modifier=Modifier.weight(1f).heightIn(min=106.dp),shape=RoundedCornerShape(16.dp)) {
      Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
       Icon(Icons.Default.MenuBook,null,tint=MaterialTheme.colorScheme.primary)
       Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
       Text(description,style=MaterialTheme.typography.bodySmall)
       Text(when(title){"MBFT"->"Abrir MBFT oficial";"Alcoolemia"->"Consultar módulo";"Sinalização"->"Abrir Normas";else->"Em preparação"},style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
      }
     }
    }
    if(pair.size==1) Spacer(Modifier.weight(1f))
   }
  }
  if(showAlcoolemia) AlertDialog(
   onDismissRequest={showAlcoolemia=false},
   title={Text("Alcoolemia")},
   text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("Referências de fiscalização: arts. 165, 165-A e 306 do CTB.")
    Text("Procedimentos e critérios técnicos: Resolução CONTRAN nº 1.030/2025. Confira o texto oficial vigente antes de aplicar.")
    Text("As fichas operacionais específicas ainda estão em conferência jurídica.")
   }},
   confirmButton={TextButton(onClick={showAlcoolemia=false;onOpenNorms()}){Text("CONSULTAR NORMAS")}},
   dismissButton={TextButton(onClick={showAlcoolemia=false}){Text("FECHAR")}}
  )

 }
}
