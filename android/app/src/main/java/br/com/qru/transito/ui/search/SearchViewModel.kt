package br.com.qru.transito.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.qru.transito.domain.model.SearchResult
import br.com.qru.transito.domain.usecase.SearchLegalContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "", val loading: Boolean = false,
    val results: List<SearchResult> = emptyList(), val message: String = "Faça uma consulta à base disponível."
)
class SearchViewModel(private val search: SearchLegalContent) : ViewModel() {
    private val mutableState = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = mutableState
    private var searchJob: Job? = null
    fun query(value: String) {
        searchJob?.cancel()
        mutableState.value = mutableState.value.copy(query = value, loading = false, results = emptyList())
    }
    fun run() {
        searchJob?.cancel()
        val query = mutableState.value.query.trim()
        if (query.isBlank()) {
            mutableState.value = mutableState.value.copy(loading = false, results = emptyList(), message = "Digite ou fale uma consulta.")
            return
        }
        searchJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, results = emptyList())
            try {
                val results = search(query)
                mutableState.value = mutableState.value.copy(loading = false, results = results,
                    message = if (results.isEmpty()) "Nenhum conteúdo disponível corresponde à consulta."
                    else "Confira os requisitos da ficha e os fatos constatados.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, results = emptyList(),
                    message = "Não foi possível consultar a base. Tente novamente.")
            }
        }
    }
}
