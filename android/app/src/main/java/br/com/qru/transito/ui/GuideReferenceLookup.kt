package br.com.qru.transito.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.qru.transito.domain.model.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Looks up only the requested code in the same repository used by search. */
@Composable internal fun GuideReferenceLookup(
 code:String,
 search:suspend (String)->List<SearchResult>,
 onSelected:(SearchResult)->Unit,
 onDismiss:()->Unit
) {
 var attempt by remember { mutableStateOf(0) }
 var loading by remember { mutableStateOf(true) }
 var message by remember { mutableStateOf("") }
 val select by rememberUpdatedState(onSelected)
 LaunchedEffect(code,attempt) {
  loading=true;message=""
  try {
   val result=search(code).singleOrNull { it.code==code }
   coroutineContext.ensureActive()
   if(result!=null) select(result)
   else message="A referência $code não está disponível de forma única na base atual."
  } catch(cancelled:CancellationException) { throw cancelled }
    catch(_:Exception) {
     coroutineContext.ensureActive()
     message="Não foi possível consultar a referência $code. Tente novamente."
    }
  loading=false
 }
 AlertDialog(onDismissRequest=onDismiss,
  title={Text(if(loading) "Abrindo referência" else "Referência indisponível")},
  text={
   Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
    if(loading) {
     LinearProgressIndicator(Modifier.fillMaxWidth())
     Text("Consultando a referência $code.")
    } else Text(message)
   }
  },
  confirmButton={if(!loading) TextButton(onClick={attempt++}) { Text("TENTAR NOVAMENTE") }},
  dismissButton={TextButton(onClick=onDismiss) { Text("VOLTAR") }}
 )
}
