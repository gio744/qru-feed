package br.com.qru.transito.data.repository
import br.com.qru.transito.data.local.QruDatabase
import br.com.qru.transito.domain.model.SearchResult
class LegalRepository(private val db:QruDatabase){
 suspend fun search(q:String)=if(q.isBlank()) emptyList() else db.legalDao().search(q.trim()).map{SearchResult(it.stableKey,it.title,it.article,it.code,it.jurisdiction,it.releaseVersion)}
 suspend fun activeVersion()=db.legalReleaseDao().active()?.version
}