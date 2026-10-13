package br.com.qru.transito.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.qru.transito.domain.usecase.*

@Composable internal fun HabilitationGuideCard(onReference: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var facts by remember { mutableStateOf(GuideFacts()) }
    Card(modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("ANÁLISE GUIADA • HABILITAÇÃO",color=MaterialTheme.colorScheme.primary)
            Text("Organize suspensão, reciclagem e validade da CNH.")
            TextButton(onClick={expanded=!expanded}) { Text(if(expanded) "RECOLHER ANÁLISE" else "ORGANIZAR O CASO") }
            if(expanded) {
                Text("Piloto em conferência. Reúne verificações e referências; a validação jurídica do cenário está pendente.",style=MaterialTheme.typography.bodySmall)
                OutlinedTextField(value=facts.date,onValueChange={facts=facts.copy(date=it)},label={Text("Data do fato (AAAA-MM-DD)")},singleLine=true,modifier=Modifier.fillMaxWidth())
                GuideChoice("Suspensão",SuspensionState.entries.map{it.label},facts.suspension.ordinal) {
                    facts=facts.copy(suspension=SuspensionState.entries[it])
                }
                GuideChoice("Reciclagem",RecyclingState.entries.map{it.label},facts.recycling.ordinal) {
                    facts=facts.copy(recycling=RecyclingState.entries[it])
                }
                GuideChoice("Validade da CNH",ExpiryState.entries.map{it.label},facts.expiry.ordinal) {
                    facts=facts.copy(expiry=ExpiryState.entries[it])
                }
                val review=HabilitationGuide.review(facts)
                Text("O QUE CONFERIR",style=MaterialTheme.typography.titleMedium)
                review.checks.forEach { Text("• $it") }
                if(review.referenceCodes.isNotEmpty()) {
                    Text("REFERÊNCIAS RELACIONADAS",style=MaterialTheme.typography.titleMedium)
                    review.referenceCodes.forEach { code ->
                        OutlinedButton(onClick={onReference(code)},modifier=Modifier.fillMaxWidth()) { Text("CONFERIR $code") }
                    }
                }
                TextButton(onClick={facts=GuideFacts()}) { Text("LIMPAR O CASO") }
            }
        }
    }
}
@Composable private fun GuideChoice(title:String,options:List<String>,selected:Int,onSelect:(Int)->Unit) {
    var open by remember { mutableStateOf(false) }
    Text(title,style=MaterialTheme.typography.titleSmall)
    Box {
        OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) { Text(options[selected]) }
        DropdownMenu(expanded=open,onDismissRequest={open=false}) {
            options.forEachIndexed { index,label ->
                DropdownMenuItem(text={Text(label)},onClick={onSelect(index);open=false})
            }
        }
    }
}
