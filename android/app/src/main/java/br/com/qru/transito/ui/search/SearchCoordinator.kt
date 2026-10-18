package br.com.qru.transito.ui.search

import br.com.qru.transito.domain.model.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "", val loading: Boolean = false,
    val results: List<SearchResult> = emptyList(), val message: String = "Faça uma consulta à base disponível."
)

/** Called on the UI thread. Results are published only by the latest request. */
internal class SearchCoordinator(
    private val scope: CoroutineScope,
    private val search: suspend (String) -> List<SearchResult>,
    private val debounce: suspend () -> Unit = { delay(180) }
) {
    private val mutableState = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = mutableState
    private var searchJob: Job? = null
    private var requestId = 0L

    fun query(value: String) = submit(value, immediate = false)
    fun run() = submit(mutableState.value.query, immediate = true)

    private fun submit(value: String, immediate: Boolean) {
        val id = ++requestId
        searchJob?.cancel()
        searchJob = null
        val query = value.trim()
        if (query.isBlank()) {
            mutableState.value = SearchUiState(query = value, message = "Digite ou fale uma consulta.")
            return
        }
        mutableState.value = SearchUiState(query = value, loading = true, message = "")
        searchJob = scope.launch {
            try {
                if (!immediate) debounce()
                val results = search(query)
                if (id == requestId) mutableState.value = mutableState.value.copy(
                    loading = false, results = results,
                    message = if (results.isEmpty()) "Nenhum conteúdo disponível corresponde à consulta."
                    else "Confira os requisitos da ficha e os fatos constatados."
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (id == requestId) mutableState.value = mutableState.value.copy(
                    loading = false, results = emptyList(),
                    message = "Não foi possível consultar a base. Tente novamente."
                )
            }
        }
    }
}
