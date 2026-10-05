package br.com.qru.transito.ui
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
  }}){padding->Box(Modifier.padding(padding).fillMaxSize()){if(tab==QruTab.BUSCA) SearchHome() else Placeholder(tab.label)}}
 }
}

@Composable private fun SearchHome(){
 val context=LocalContext.current
 val app=context.applicationContext as QruApplication
 val vm:SearchViewModel=viewModel(factory=SearchViewModelFactory(app.searchLegalContent))
 val state by vm.state.collectAsState()
 var occurrence by remember{mutableStateOf("")}
 val speech=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){r->
  if(r.resultCode==Activity.RESULT_OK) r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{vm.query(it)}
 }
 fun launchSpeech(){speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
  putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
  putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
 })}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)launchSpeech()}
 fun voice(){if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)launchSpeech() else permission.launch(Manifest.permission.RECORD_AUDIO)}

 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("QRU?",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary)
  Text("Trânsito",style=MaterialTheme.typography.titleLarge)
  Text("Consulta operacional • offline-first",color=MaterialTheme.colorScheme.secondary)
  OutlinedTextField(state.query,{vm.query(it)},Modifier.fillMaxWidth(),label={Text("Busque artigo, código ou situação")},
   trailingIcon={IconButton({voice()}){Icon(Icons.Default.Mic,"Pesquisa por voz")}},singleLine=true)
  Button({vm.run()},Modifier.fillMaxWidth()){Icon(Icons.Default.Search,null);Spacer(Modifier.width(8.dp));Text("CONSULTAR")}
  if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  state.results.forEach{r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){
   Text(r.title,style=MaterialTheme.typography.titleMedium)
   Text(listOfNotNull(r.article,r.code).joinToString(" • "),color=MaterialTheme.colorScheme.secondary)
   Text("Base ${r.releaseVersion} • ${r.jurisdiction}",style=MaterialTheme.typography.bodySmall)
  }}}
  Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.padding(16.dp)){Text("MODO OCORRÊNCIA",color=MaterialTheme.colorScheme.primary)
    OutlinedTextField(occurrence,{occurrence=it},Modifier.fillMaxWidth(),label={Text("Descreva os fatos observados")},minLines=3)
    Text("A narrativa localiza hipóteses; somente fatos confirmados alimentam o fluxo.",style=MaterialTheme.typography.bodySmall)}
  }
  Text(state.message)
 }
}
@Composable private fun Placeholder(name:String){Column(Modifier.fillMaxSize().padding(24.dp)){Text(name,style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary);Text("Módulo nativo reservado.")}}
