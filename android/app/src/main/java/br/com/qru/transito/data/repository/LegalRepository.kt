package br.com.qru.transito.data.repository

import br.com.qru.transito.data.local.QruDatabase
import br.com.qru.transito.domain.model.SearchResult
import br.com.qru.transito.domain.usecase.CatalogSearch
import br.com.qru.transito.domain.usecase.IndexedReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LegalRepository(private val db: QruDatabase, private val bundled: BundledCatalog) {
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList()
        else if (db.legalReleaseDao().active() == null) bundled.search(query)
        else CatalogSearch.search(db.legalDao().activeItems().map {
            IndexedReference(SearchResult(it.stableKey, it.title, it.article, it.code, it.jurisdiction, it.releaseVersion,
                it.source, it.reviewStatus, it.appliesFrom, it.appliesUntil), it.searchText)
        }, query)
    }
    /**
     * Resolve eligibility against the current release, not a possibly stale search result.
     * A bundled reference is eligible only when no database release is active.
     */
    suspend fun isActiveReference(result: SearchResult): Boolean = withContext(Dispatchers.IO) {
        val active = db.legalReleaseDao().active()
        if (active == null) {
            bundled.all().any { it.stableKey == result.stableKey && it.code == result.code }
        } else {
            db.legalDao().activeItems().any {
                it.stableKey == result.stableKey && it.code == result.code &&
                    it.releaseVersion == active.version
            }
        }
    }

    suspend fun activeVersion() = db.legalReleaseDao().active()?.version
}
