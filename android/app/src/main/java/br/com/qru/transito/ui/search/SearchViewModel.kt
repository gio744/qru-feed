package br.com.qru.transito.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.qru.transito.domain.usecase.SearchLegalContent

class SearchViewModel(search: SearchLegalContent) : ViewModel() {
    private val coordinator = SearchCoordinator(viewModelScope, { query -> search(query) })
    val state = coordinator.state
    fun query(value: String) = coordinator.query(value)
    fun run() = coordinator.run()
}
