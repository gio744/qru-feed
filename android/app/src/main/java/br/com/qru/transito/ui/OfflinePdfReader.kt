package br.com.qru.transito.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

private fun openPdf(file:File):PdfRenderer {
 val descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
 return try { PdfRenderer(descriptor) } catch(error:Throwable) { descriptor.close();throw error }
}
private fun pdfHash(file:File):String {
 val digest=MessageDigest.getInstance("SHA-256")
 file.inputStream().use { input ->
  val buffer=ByteArray(64*1024)
  while(true) { val count=input.read(buffer);if(count<0)break;digest.update(buffer,0,count) }
 }
 return digest.digest().joinToString("") { "%02x".format(it) }
}

/** Virtualized continuous pages; only nearby pages allocate bitmaps. */
@Composable fun OfflinePdfReader(expectedHash:String,assetName:String="resolucao10312026.pdf",title:String="Resolução 1.031/2026",pageCount:Int=20,annexPage:Int?=12,initialPage:Int?=null,pageRange:IntRange?=null,onDismiss:()->Unit) {
 key(expectedHash,initialPage,pageRange) {
 val context=LocalContext.current
 val preferences=remember(context) { context.getSharedPreferences("qru_pdf_reading",android.content.Context.MODE_PRIVATE) }
 val savedPage=remember(expectedHash) { runCatching { preferences.getInt(expectedHash,1) }.getOrDefault(1) }
 val window=remember(pageRange,pageCount) { PdfPageWindow(pageRange?.first ?: 1,pageRange?.last ?: pageCount,pageCount) }
 val startPage=remember(expectedHash,initialPage,pageCount,window) {
  initialPdfPage(initialPage,savedPage,pageCount).coerceIn(window.firstPage,window.lastPage)
 }
 val listState=rememberLazyListState(initialFirstVisibleItemIndex=window.listIndex(startPage))
 val page by remember { derivedStateOf { listState.firstVisibleItemIndex } }
 val scope=rememberCoroutineScope()
 val keyboard=LocalSoftwareKeyboardController.current
 val renderMutex=remember(expectedHash) { Mutex() }
 var file by remember(expectedHash) { mutableStateOf<File?>(null) }
 var failed by remember(expectedHash) { mutableStateOf(false) }
 var attempt by remember { mutableStateOf(0) }
 var jumpOpen by rememberSaveable(assetName) { mutableStateOf(false) }
 var jumpText by rememberSaveable(assetName) { mutableStateOf("") }
 var zoom by rememberSaveable(assetName) { mutableStateOf(1) }
 LaunchedEffect(assetName,expectedHash,attempt) {
  failed=false;file=null
  try {
   file=withContext(Dispatchers.IO) {
    require(expectedHash.matches(Regex("[a-f0-9]{64}")))
    require(assetName.matches(Regex("[A-Za-z0-9]+\\.pdf")))
    val destination=File(context.cacheDir,"norm-$expectedHash.pdf")
    if(!destination.exists() || pdfHash(destination)!=expectedHash) {
     val temporary=File.createTempFile("qru-pdf-", ".tmp",context.cacheDir)
     try {
      context.assets.open("norms/$assetName").use { input -> temporary.outputStream().use { input.copyTo(it) } }
      require(pdfHash(temporary)==expectedHash) { "PDF integrity mismatch" }
      check(temporary.renameTo(destination))
     } finally { temporary.delete() }
    }
    openPdf(destination).use { require(it.pageCount==pageCount) }
    destination
   }
  } catch(cancelled:CancellationException) { throw cancelled }
    catch(_:Exception) { failed=true }
 }
 fun go(index:Int) { if(file!=null) scope.launch { listState.scrollToItem(index.coerceIn(0,window.count-1)) } }
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.fillMaxSize()) {
   Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text(title,style=MaterialTheme.typography.titleLarge)
    Text(if(pageRange==null) "Deslize para percorrer as páginas • PDF offline" else "Ficha individual • em conferência",style=MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     TextButton(onClick={go(page-1)},enabled=page>0 && file!=null){Text("ANTERIOR")}
     TextButton(onClick={jumpText=(page+1).toString();jumpOpen=true},enabled=file!=null) { Text("${page+1} / ${window.count} ▾") }
     TextButton(onClick={go(page+1)},enabled=page<window.count-1 && file!=null){Text("PRÓXIMA")}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     TextButton(onClick={go(0)},enabled=page!=0 && file!=null){Text("PRIMEIRA")}
     annexPage?.takeIf { it in window.firstPage..window.lastPage }?.let { first ->
      TextButton(onClick={go(window.listIndex(first))},enabled=page!=window.listIndex(first) && file!=null){Text("ANEXOS · P. $first")}
     }
     TextButton(onClick={go(window.count-1)},enabled=page!=window.count-1 && file!=null){Text("ÚLTIMA")}
    }
    val document=file
    if(failed) {
     Text("Não foi possível abrir o PDF.")
     TextButton(onClick={attempt++}) { Text("TENTAR NOVAMENTE") }
    } else if(document==null) LinearProgressIndicator(Modifier.fillMaxWidth())
    Box(Modifier.weight(1f).fillMaxWidth()) {
     if(document!=null) LazyColumn(state=listState,modifier=Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      items(count=window.count,key={it},contentType={"pdf_page"}) { index ->
       ContinuousPdfPage(document,window.documentIndex(index),zoom,renderMutex,page==index) {
        if(pageRange==null) preferences.edit().putInt(expectedHash,window.documentIndex(index)+1).apply()
       }
      }
     }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     TextButton(onClick={zoom=if(zoom==3) 1 else zoom+1}) { Text("AMPLIAR ${zoom}×") }
     TextButton(onClick=onDismiss) { Text("VOLTAR") }
    }
   }
  }
 }
 if(jumpOpen) {
  val target=jumpText.toIntOrNull()
  val valid=target!=null && target in 1..window.count
  fun jump() { if(valid) {go(target!!-1);jumpOpen=false;keyboard?.hide()} }
  AlertDialog(onDismissRequest={jumpOpen=false},title={Text("Ir para página")},text={
   OutlinedTextField(jumpText,{value->jumpText=value.filter { it in '0'..'9' }.take(window.count.toString().length)},
    label={Text("Página de 1 a ${window.count}")},singleLine=true,isError=jumpText.isNotBlank() && !valid,
    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Go),
    keyboardActions=KeyboardActions(onGo={jump()}))
  },confirmButton={TextButton(onClick={jump()},enabled=valid){Text("IR")}},
   dismissButton={TextButton(onClick={jumpOpen=false}){Text("CANCELAR")}})
 }
 }
}

@Composable private fun ContinuousPdfPage(file:File,index:Int,zoom:Int,mutex:Mutex,isCurrent:Boolean,onDisplayed:()->Unit) {
 var bitmap by remember(file,index) { mutableStateOf<Bitmap?>(null) }
 var failed by remember(file,index) { mutableStateOf(false) }
 var attempt by remember(file,index) { mutableStateOf(0) }
 LaunchedEffect(file,index,attempt) {
  bitmap=null;failed=false
  try {
   bitmap=withContext(Dispatchers.IO) {
    mutex.withLock {
     coroutineContext.ensureActive()
     openPdf(file).use { pdf ->
      pdf.openPage(index).use { source ->
       val width=1600
       val height=(width.toFloat()*source.height/source.width).toInt().coerceAtLeast(1)
       val frame=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
       try {
        frame.eraseColor(Color.WHITE)
        source.render(frame,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        frame
       } catch(error:Throwable) { frame.recycle();throw error }
      }
     }
    }
   }
  } catch(cancelled:CancellationException) { throw cancelled }
    catch(_:Exception) { failed=true }
 }
 val frame=bitmap
 LaunchedEffect(frame,isCurrent) { if(frame!=null && isCurrent) onDisplayed() }
 Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
  Text("Página ${index+1}",style=MaterialTheme.typography.labelSmall)
  BoxWithConstraints(Modifier.fillMaxWidth()) {
   val width=maxWidth*zoom
   Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
    if(frame!=null) Image(frame.asImageBitmap(),"Página ${index+1} do PDF original",
     modifier=Modifier.width(width).aspectRatio(frame.width.toFloat()/frame.height))
    else Box(Modifier.width(width).aspectRatio(1f/1.4142f),contentAlignment=Alignment.Center) {
     if(failed) Column(horizontalAlignment=Alignment.CenterHorizontally) {
      Text("Não foi possível abrir esta página.")
      TextButton(onClick={attempt++}) { Text("TENTAR NOVAMENTE") }
     } else CircularProgressIndicator()
    }
   }
  }
 }
}
