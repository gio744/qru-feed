package br.com.qru.transito.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

private data class PdfFrame(val bitmap:Bitmap,val count:Int)

/** One page at a time keeps allocation bounded even for image-only annexes. */
@Composable fun OfflinePdfReader(expectedHash:String,onDismiss:()->Unit) {
 val context=LocalContext.current
 var page by rememberSaveable { mutableStateOf(0) }
 var jumpOpen by rememberSaveable { mutableStateOf(false) }
 var jumpText by rememberSaveable { mutableStateOf("") }
 val keyboard=LocalSoftwareKeyboardController.current
 var zoom by rememberSaveable { mutableStateOf(1) }
 var frame by remember { mutableStateOf<PdfFrame?>(null) }
 var failed by remember { mutableStateOf(false) }
 var loading by remember { mutableStateOf(true) }
 LaunchedEffect(page) {
  loading=true;failed=false
  val result=withContext(Dispatchers.IO) {
   runCatching {
    val file=File(context.cacheDir,"norm-1031-$expectedHash.pdf")
    if(!file.exists()) {
     val temporary=File(context.cacheDir,"norm-1031.tmp")
     try {
      context.assets.open("norms/resolucao10312026.pdf").use { input ->
       temporary.outputStream().use { output -> input.copyTo(output) }
      }
      val actual=MessageDigest.getInstance("SHA-256").digest(temporary.readBytes())
       .joinToString("") { "%02x".format(it) }
      require(actual==expectedHash) { "PDF integrity mismatch" }
      check(temporary.renameTo(file))
     } finally { temporary.delete() }
    }
    val descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer=try { PdfRenderer(descriptor) } catch(error:Throwable) { descriptor.close();throw error }
    renderer.use { pdf ->
     require(pdf.pageCount==20)
     pdf.openPage(page.coerceIn(0,pdf.pageCount-1)).use { source ->
      val width=1600
      val height=(width.toFloat()*source.height/source.width).toInt().coerceAtLeast(1)
      val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
      try {
       bitmap.eraseColor(Color.WHITE)
       source.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
       PdfFrame(bitmap,pdf.pageCount)
      } catch(error:Throwable) { bitmap.recycle();throw error }
     }
    }
   }
  }
  frame=result.getOrNull();failed=result.isFailure;loading=false
 }
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
  Surface(Modifier.fillMaxSize()) {
   Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("Resolução 1.031/2026",style=MaterialTheme.typography.titleLarge)
    Text("PDF completo offline • cópia incluída nesta versão",style=MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     TextButton(onClick={page--},enabled=page>0 && !loading){Text("ANTERIOR")}
     TextButton(onClick={jumpText=(page+1).toString();jumpOpen=true}) { Text("${page+1} / 20 ▾") }
     TextButton(onClick={page++},enabled=page<19 && !loading){Text("PRÓXIMA")}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     TextButton(onClick={page=0},enabled=page!=0 && !loading){Text("PRIMEIRA")}
     TextButton(onClick={page=11},enabled=page!=11 && !loading){Text("ANEXOS · P. 12")}
     TextButton(onClick={page=19},enabled=page!=19 && !loading){Text("ÚLTIMA")}
    }
    if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if(failed) Text("Não foi possível abrir o PDF. Feche e tente novamente.")
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
     val width=maxWidth
     key(page,zoom) {
      Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState())) {
       if(!loading) frame?.let {
        Image(it.bitmap.asImageBitmap(),"Página ${page+1} do PDF original",
         modifier=Modifier.width(width*zoom).aspectRatio(it.bitmap.width.toFloat()/it.bitmap.height))
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
  val valid=target!=null && target in 1..20
  fun go() { if(valid) {page=target!!-1;jumpOpen=false;keyboard?.hide()} }
  AlertDialog(onDismissRequest={jumpOpen=false},title={Text("Ir para página")},text={
   OutlinedTextField(jumpText,{value->jumpText=value.filter { it in '0'..'9' }.take(2)},
    label={Text("Página de 1 a 20")},singleLine=true,
    isError=jumpText.isNotBlank() && !valid,
    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number,imeAction=ImeAction.Go),
    keyboardActions=KeyboardActions(onGo={go()}))
  },confirmButton={TextButton(onClick={go()},enabled=valid){Text("IR")}},
   dismissButton={TextButton(onClick={jumpOpen=false}){Text("CANCELAR")}})
 }

}
