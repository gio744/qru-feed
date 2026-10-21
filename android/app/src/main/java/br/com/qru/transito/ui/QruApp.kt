package br.com.qru.transito.ui
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import br.com.qru.transito.R
import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.com.qru.transito.domain.model.SearchResult
import br.com.qru.transito.domain.model.OperationalReferencePublication
import br.com.qru.transito.data.repository.OperationalFichePackage
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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

enum class QruTab(val label:String){BUSCA("Busca"),CTB("CTB"),NORMAS("Normas"),SITUACOES("Situações"),NOVIDADES("Novidades")}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun QruApp(){
 QruTheme{
  var tab by rememberSaveable{mutableStateOf(QruTab.BUSCA)}
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
    NavigationDrawerItem(label={Text("Como usar")},selected=false,
     icon={Icon(Icons.Default.HelpOutline,null)},onClick={support="help";scope.launch{drawer.close()}})
    NavigationDrawerItem(label={Text("Fontes oficiais")},selected=false,
     icon={Icon(Icons.Default.MenuBook,null)},onClick={tab=QruTab.NORMAS;scope.launch{drawer.close()}})
    NavigationDrawerItem(label={Text("Sobre o QRU")},selected=false,
     icon={Icon(Icons.Default.Info,null)},onClick={support="about";scope.launch{drawer.close()}})
    TextButton(onClick={scope.launch{drawer.close()}},modifier=Modifier.padding(12.dp)){Text("FECHAR MENU")}
   }
  }){
   Scaffold(bottomBar={NavigationBar(containerColor=Color(0xFF15101E)){
    QruTab.entries.forEach{item->NavigationBarItem(selected=tab==item,onClick={tab=item},modifier=Modifier.testTag("tab_"+item.name),
     icon={Icon(when(item){QruTab.BUSCA->Icons.Default.Search;QruTab.CTB->Icons.Default.MenuBook;QruTab.NORMAS->Icons.Default.Gavel;QruTab.SITUACOES->Icons.Default.PlayCircle;QruTab.NOVIDADES->Icons.Default.NewReleases},item.label)},
     label={Text(item.label)})}
   }}){padding->Box(Modifier.padding(padding).fillMaxSize()){
    tabStates.SaveableStateProvider(tab.name) {
     if(tab==QruTab.BUSCA) SearchHome{scope.launch{drawer.open()}} else CatalogPage(tab)
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

@Composable private fun SearchHome(onMenu:()->Unit){
 val context=LocalContext.current
 val app=context.applicationContext as QruApplication
 val vm:SearchViewModel=viewModel(factory=SearchViewModelFactory(app.searchLegalContent))
 val state by vm.state.collectAsState()
 val keyboard=LocalSoftwareKeyboardController.current
 var occurrence by rememberSaveable{mutableStateOf("")}
 var occurrenceExpanded by rememberSaveable{mutableStateOf(false)}
 var selected by remember{mutableStateOf<SearchResult?>(null)}
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

 val quickOptions=listOf(
  "230, V" to "65992","Art. 167" to "167","162, I" to "162, I",
  "Art. 252" to "252","165-A" to "165-A","230 XVIII" to "230 XVIII",
  "Art. 181" to "181","Art. 208" to "208","Art. 230" to "230",
  "Art. 244" to "244","Art. 168" to "168","Art. 195" to "195"
 )
 val quickPrefs=remember(context) { context.getSharedPreferences("qru_quick_access",android.content.Context.MODE_PRIVATE) }
 var quickKeys by remember {
  mutableStateOf((0..3).map { index ->
   quickPrefs.getString("slot_$index",null)?.takeIf { saved -> quickOptions.any { it.first==saved } }
    ?:quickOptions[index].first
  })
 }
 var editingQuick by rememberSaveable { mutableStateOf(false) }
 var editingSlot by rememberSaveable { mutableIntStateOf(-1) }
 fun selectQuick(slot:Int,label:String) {
  val updated=quickKeys.toMutableList()
  val other=updated.indexOf(label)
  if(other>=0 && other!=slot) updated[other]=updated[slot]
  updated[slot]=label
  quickKeys=updated
  quickPrefs.edit().apply {
   updated.forEachIndexed { index,value -> putString("slot_$index",value) }
  }.apply()
  editingSlot=-1
 }
 if(editingSlot>=0) AlertDialog(
  onDismissRequest={editingSlot=-1},
  title={Text("Escolher atalho")},
  text={Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
   quickOptions.forEach { (label,_) ->
    TextButton(onClick={selectQuick(editingSlot,label)},modifier=Modifier.fillMaxWidth()) { Text(label) }
   }
  }},
  confirmButton={TextButton(onClick={editingSlot=-1}){Text("CANCELAR")}}
 )
 LazyColumn(Modifier.fillMaxSize().testTag("search_results"),contentPadding=PaddingValues(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item(key="search_header") {
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Box(Modifier.fillMaxWidth().background(
   brush=Brush.linearGradient(listOf(Color(0xFF241047),Color(0xFF5620A0),Color(0xFF35114D))),
   shape=RoundedCornerShape(24.dp)
  ).border(1.dp,Color(0xFF6D3BA0),RoundedCornerShape(24.dp)).padding(vertical=18.dp,horizontal=10.dp)) {
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
  Text("CONSULTA OPERACIONAL  •  CATÁLOGO OFFLINE", style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
   }
  }
  if(state.query.isBlank()) item(key="quick_access") {
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
     Text("ACESSO RÁPIDO",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,
      color=Color(0xFFD9B6FF),fontWeight=FontWeight.SemiBold)
     TextButton(onClick={editingQuick=!editingQuick}) {
      Icon(Icons.Default.Edit,null,Modifier.size(16.dp))
      Spacer(Modifier.width(4.dp))
      Text(if(editingQuick) "CONCLUIR" else "PERSONALIZAR")
     }
    }
    for(row in 0..1) {
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
      for(column in 0..1) {
       val slot=row*2+column
       val label=quickKeys[slot]
       val query=quickOptions.firstOrNull { it.first==label }?.second ?: label
       QruQuickAction(label,if(editingQuick) Icons.Default.Edit else Icons.Default.Gavel,
        Modifier.weight(1f)) {
        if(editingQuick) editingSlot=slot else { vm.query(query);vm.run() }
       }
      }
     }
    }
    Text("CONSULTAS FREQUENTES",style=MaterialTheme.typography.labelSmall,color=Color(0xFFD9B6FF))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     listOf("165-A","230 XVIII","181").forEach { term ->
      SuggestionChip(onClick={vm.query(term);vm.run()},label={Text(term)})
     }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     TextButton(onClick={occurrenceExpanded=!occurrenceExpanded}) {
      Icon(Icons.Default.Description,null,Modifier.size(18.dp))
      Spacer(Modifier.width(4.dp))
      Text("Ocorrência")
     }
     TextButton(onClick={guideExpanded=true}) {
      Icon(Icons.Default.FactCheck,null,Modifier.size(18.dp))
      Spacer(Modifier.width(4.dp))
      Text("Análise guiada")
     }
    }
    if(occurrenceExpanded) {
     OutlinedTextField(occurrence,{occurrence=it},Modifier.fillMaxWidth(),
      label={Text("Descreva os fatos observados")},minLines=3)
     Button(onClick={keyboard?.hide();vm.query(occurrence);vm.run()},enabled=occurrence.isNotBlank()) {
      Text("BUSCAR REFERÊNCIAS")
     }
    }
    Text("Atalhos de pesquisa. Confira sempre a fonte oficial e a regra aplicável ao fato.",
     style=MaterialTheme.typography.bodySmall,color=Color(0xFFB9A9C9))
   }
  }
  // Numeric article queries display their subdivisions in legal order.
  val articleNumber=state.query.trim().replace(Regex("^(?i)art(?:igo)?\\.?\\s*"),"")
      .takeIf { it.matches(Regex("\\d+")) }
  if(articleNumber!=null && state.results.isNotEmpty()) item(key="article_hierarchy_header") {
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text("ART. $articleNumber • DESDOBRAMENTOS",color=Color(0xFFD9B6FF),
      style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    Text("Incisos em ordem legal. Toque em um enquadramento para abrir sua ficha individual.",
      style=MaterialTheme.typography.bodySmall)
   }
  }
  itemsIndexed(state.results,key={_,result -> "reference:"+result.stableKey},
      contentType={_,_->"reference"}) { index,result ->
   Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
    if(articleNumber!=null) {
     val inciso=result.article.orEmpty().substringAfter(',',"").trim()
     val previous=state.results.getOrNull(index-1)?.article.orEmpty().substringAfter(',',"").trim()
     if(index==0 || inciso!=previous) {
      Text(if(inciso.isBlank()) "ARTIGO $articleNumber" else "INCISO $inciso",
       color=Color(0xFFD2A8FF),fontWeight=FontWeight.SemiBold,
       style=MaterialTheme.typography.labelLarge)
     }
    }
    ReferenceCard(result) { selected=result }
   }
  }
  if(state.query.isNotBlank()) item(key="search_tools") {
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
 selected?.let { current ->
  val index=state.results.indexOfFirst { it.stableKey==current.stableKey }
  ReferenceDialog(
   result=current,
   onDismiss={selected=null},
   onPrevious=if(index>0) ({ selected=state.results[index-1] }) else null,
   onNext=if(index>=0 && index<state.results.lastIndex) ({ selected=state.results[index+1] }) else null,
   position=if(index>=0) index+1 else null,
   total=if(index>=0) state.results.size else null
  )
 }
}

private fun openOfficialSource(context: android.content.Context, url: String) {
    val uri=Uri.parse(url)
    val host=uri.host?.lowercase(Locale.ROOT)
    val official=host?.endsWith(".gov.br")==true || host=="www2.camara.leg.br"
    if(uri.scheme!="https" || !official) return
    try { context.startActivity(Intent(Intent.ACTION_VIEW,uri)) }
    catch(_:ActivityNotFoundException) { Toast.makeText(context,"Não foi possível abrir a fonte.",Toast.LENGTH_LONG).show() }
}
@Composable private fun QruQuickAction(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,
 modifier:Modifier=Modifier,onClick:()->Unit) {
 Card(onClick=onClick,modifier=modifier.heightIn(min=84.dp),
  colors=CardDefaults.cardColors(containerColor=Color(0xFF21172E)),
  border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF49305F)),
  shape=RoundedCornerShape(18.dp)) {
  Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Icon(icon,contentDescription=null,tint=Color(0xFFC48DFF))
   Text(label,style=MaterialTheme.typography.labelLarge,color=Color(0xFFF0E5FA))
  }
 }
}

@Composable private fun ReferenceCard(result:SearchResult,onClick:()->Unit) {
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag("reference_card")
        .border(1.dp,Color(0xFF49305F),RoundedCornerShape(18.dp)),
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=Color(0xFF191326))) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(result.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            Text("Art. ${result.article.orEmpty()}  •  ${result.code.orEmpty()}",
                color=Color(0xFFD2A8FF),style=MaterialTheme.typography.bodyMedium)
            Text("REFERÊNCIA EM CONFERÊNCIA",color=Color(0xFFB9A9C9),
                style=MaterialTheme.typography.labelSmall)
        }
    }
}
@Composable private fun QruExpandableSection(title:String,referenceKey:String,initiallyOpen:Boolean=false,content:@Composable ColumnScope.()->Unit) {
 var expanded by rememberSaveable(referenceKey,title) { mutableStateOf(initiallyOpen) }
 Surface(color=Color(0xFF21172E),shape=RoundedCornerShape(16.dp),
  border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF49305F))) {
  Column {
   Row(Modifier.fillMaxWidth().clickable { expanded=!expanded }.padding(14.dp),
    verticalAlignment=Alignment.CenterVertically) {
    Text(title,Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,
     color=Color(0xFFE1C3FF),fontWeight=FontWeight.SemiBold)
    Icon(if(expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
     contentDescription=if(expanded) "Recolher" else "Expandir",tint=Color(0xFFC48DFF))
   }
   if(expanded) {
    HorizontalDivider(color=Color(0xFF49305F))
    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)
   }
  }
 }
}

@Composable private fun ReferenceDialog(result:SearchResult,onDismiss:()->Unit,onPrevious:(()->Unit)?=null,onNext:(()->Unit)?=null,position:Int?=null,total:Int?=null) {
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
    AlertDialog(onDismissRequest=onDismiss,containerColor=Color(0xFF191326),shape=RoundedCornerShape(24.dp),title={Text(result.title,fontWeight=FontWeight.SemiBold)},text={
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Art. ${result.article.orEmpty()}  •  código ${result.code.orEmpty()}",color=Color(0xFFD2A8FF),style=MaterialTheme.typography.labelLarge)
            if(position!=null && total!=null && total>1) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(onClick={onPrevious ?: {}},enabled=onPrevious!=null) { Text("ANTERIOR") }
                    Text("$position de $total",style=MaterialTheme.typography.labelSmall,color=Color(0xFFD2A8FF))
                    TextButton(onClick={onNext ?: {}},enabled=onNext!=null) { Text("PRÓXIMA") }
                }
            }
            if (approvedOperationalFiche == null) {
                Text("REFERÊNCIA EM CONFERÊNCIA • validação jurídica pendente",color=Color(0xFFD7BDDE),style=MaterialTheme.typography.labelSmall)
                Text("As seções abaixo não constituem orientação de autuação enquanto não houver aprovação editorial.",style=MaterialTheme.typography.bodySmall)
            } else {
                Text("ORIENTAÇÃO OPERACIONAL APROVADA • confira a vigência e as fontes",color=Color(0xFFBCE7D0),style=MaterialTheme.typography.labelSmall)
            }
            result.source?.let { source ->
                OutlinedButton(onClick={openOfficialSource(context,source)},modifier=Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(8.dp))
                    Text("CONSULTAR FONTE OFICIAL")
                }
            }
            QruExpandableSection("QUANDO AUTUAR",referenceKey=result.stableKey,initiallyOpen=true) {
            if (approvedOperationalFiche == null) Text("Orientação operacional ainda não aprovada. Consulte a ficha oficial antes de decidir.")
            else approvedOperationalFiche?.whenApplicable?.forEach { item ->
                Text(item.text)
                item.evidence.forEach { source ->
                    TextButton(onClick={ openOfficialSource(context, source.url) }) {
                        Text("Fonte oficial: ${source.locator}", style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }
            QruExpandableSection("QUANDO ESTE ARTIGO NÃO SE APLICA",referenceKey=result.stableKey) {
            if (approvedOperationalFiche == null) Text("Hipóteses de não enquadramento pendentes de revisão jurídica. A ausência de requisitos deste artigo não significa necessariamente outra infração.")
            else approvedOperationalFiche?.exceptions?.forEach { item ->
                Text(item.text)
                item.evidence.forEach { source ->
                    TextButton(onClick={ openOfficialSource(context, source.url) }) {
                        Text("Fonte oficial: ${source.locator}", style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
            }
            QruExpandableSection("EXEMPLOS DE OBSERVAÇÃO NO AIT",referenceKey=result.stableKey) {
                Text("Utilize somente os dados efetivamente constatados. Adapte o modelo ao caso concreto; não registre fatos presumidos.",style=MaterialTheme.typography.bodySmall)
                val examples=approvedOperationalFiche?.observations.orEmpty()
                if(examples.isEmpty()) {
                    Text("Exemplos específicos deste enquadramento aguardam revisão e aprovação editorial. Consulte o MBFT e a norma vigente antes de preencher o AIT.")
                } else {
                    examples.forEachIndexed { index,item ->
                        Text("EXEMPLO ${index+1}",fontWeight=FontWeight.SemiBold,color=Color(0xFFD2A8FF))
                        Text(item.text)
                        OutlinedButton(onClick={
                            val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Observação AIT — QRU",item.text))
                            Toast.makeText(context,"Observação copiada. Confira e adapte aos fatos constatados.",Toast.LENGTH_LONG).show()
                        },modifier=Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.ContentCopy,contentDescription=null,modifier=Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("COPIAR OBSERVAÇÃO")
                        }
                        item.evidence.forEach { source ->
                            TextButton(onClick={openOfficialSource(context,source.url)}) {
                                Text("Fundamento: ${source.locator}",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider(color=Color(0xFF49305F))
                    }
                }
            }
            QruExpandableSection("ENQUADRAMENTOS RELACIONADOS",referenceKey=result.stableKey) {
            Text("Nenhuma combinação ou enquadramento alternativo é sugerido automaticamente.",fontWeight=FontWeight.SemiBold)
            Text("Para comparar referências, utilize a busca do QRU e abra cada ficha individual. Verifique os elementos de cada infração, a responsabilidade e a regra aplicável à data do fato.",style=MaterialTheme.typography.bodySmall)
            Text("A coexistência de artigos em uma pesquisa não comprova concurso de infrações nem autoriza autuação cumulativa.",style=MaterialTheme.typography.bodySmall)
            }
            QruExpandableSection("MEDIDAS ADMINISTRATIVAS",referenceKey=result.stableKey) {
            if (approvedOperationalFiche == null) Text("Retenção, remoção e regularização dependem da norma aplicável e de conferência editorial.")
            else approvedOperationalFiche?.measures?.forEach { item ->
                Text("MODALIDADE: " + when(item.kind.name) {
                    "RETENTION" -> "RETENÇÃO"
                    "REMOVAL" -> "REMOÇÃO"
                    else -> "SEM MEDIDA PREVISTA NA FICHA"
                },fontWeight=FontWeight.SemiBold,color=Color(0xFFD2A8FF))
                Text(item.rule.text)
                item.rule.evidence.forEach { source ->
                    TextButton(onClick={ openOfficialSource(context, source.url) }) {
                        Text("Fonte oficial: ${source.locator}", style=MaterialTheme.typography.bodySmall)
                    }
                }
                item.conditions.forEach { condition ->
                    Text("CONDIÇÃO: ${condition.text}",style=MaterialTheme.typography.bodySmall)
                    condition.evidence.forEach { source ->
                        TextButton(onClick={openOfficialSource(context,source.url)}) {
                            Text("Fundamento da condição: ${source.locator}",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item.deadline?.let { deadline ->
                    Text("PRAZO MÁXIMO REGISTRADO: ${deadline.maximumDays} dias",style=MaterialTheme.typography.bodySmall)
                    Text(deadline.legalBasis.text,style=MaterialTheme.typography.bodySmall)
                    deadline.legalBasis.evidence.forEach { source ->
                        TextButton(onClick={openOfficialSource(context,source.url)}) {
                            Text("Fundamento do prazo: ${source.locator}",style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item.systemDependency?.let { dependency ->
                    Text("DEPENDÊNCIA OPERACIONAL: $dependency",style=MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color=Color(0xFF49305F))
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
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(tab.label,style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold)
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
 LazyColumn(Modifier.fillMaxSize().testTag("situations_results"),
  contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  item(key="situations_header") {
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(14.dp)) {
    Text("Situações",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold)
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
    Text("Vídeos ainda não disponíveis.",style=MaterialTheme.typography.bodySmall)
   }
  }
 }
 selected?.let { ReferenceDialog(it,onDismiss={selected=null}) }
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
