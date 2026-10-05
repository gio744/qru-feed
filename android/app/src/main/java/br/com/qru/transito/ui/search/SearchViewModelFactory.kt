package br.com.qru.transito.ui.search
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.qru.transito.domain.usecase.SearchLegalContent
class SearchViewModelFactory(private val useCase:SearchLegalContent):ViewModelProvider.Factory{
 @Suppress("UNCHECKED_CAST")
 override fun <T:ViewModel> create(modelClass:Class<T>):T=SearchViewModel(useCase) as T
}
