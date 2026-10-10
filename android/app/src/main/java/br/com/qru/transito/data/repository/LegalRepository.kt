package br.com.qru.transito.data.repository

import br.com.qru.transito.data.local.QruDatabase
import br.com.qru.transito.domain.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LegalRepository(private val db: QruDatabase, private val bundled: BundledCatalog) {
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList()
        else if (db.legalReleaseDao().active() == null) bundled.search(query)
        else db.legalDao().search(query.trim()).map {
            SearchResult(it.stableKey, it.title, it.article, it.code, it.jurisdiction, it.releaseVersion)
        }
    }
    suspend fun activeVersion() = db.legalReleaseDao().active()?.version
}
