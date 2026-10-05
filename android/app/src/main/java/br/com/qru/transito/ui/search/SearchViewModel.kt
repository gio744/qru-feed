package br.com.qru.transito.ui.search
import androidx.lifecycle.*;import br.com.qru.transito.domain.model.SearchResult;import br.com.qru.transito.domain.usecase.SearchLegalContent;import kotlinx.coroutines.flow.*;import kotlinx.coroutines.launch
data class SearchUiState(val query:String="",val loading:Boolean=false,val results:List<SearchResult> = emptyList(),val message:String="Base local pronta.")
class SearchViewModel(private val search:SearchLegalContent):ViewModel(){private val _s=MutableStateFlow(SearchUiState());val state:StateFlow<SearchUiState> = _s
 fun query(v:String){_s.value=_s.value.copy(query=v)}
 fun run(){val q=_s.value.query.trim();if(q.isBlank()){_s.value=_s.value.copy(results=emptyList(),message="Digite ou fale uma consulta.");return};viewModelScope.launch{_s.value=_s.value.copy(loading=true);val r=search(q);_s.value=_s.value.copy(loading=false,results=r,message=if(r.isEmpty())"BASE EM EXPANSÃO — nenhum conteúdo auditado local corresponde à consulta." else "QRU localizou hipóteses. A constatação do agente confirma os fatos.")}}
}