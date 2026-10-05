package br.com.qru.transito.domain.usecase
import br.com.qru.transito.data.repository.LegalRepository
class SearchLegalContent(private val repo:LegalRepository){suspend operator fun invoke(q:String)=repo.search(q)}