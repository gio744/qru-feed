package br.com.qru.transito.domain.usecase

import br.com.qru.transito.domain.model.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogSearchCompactIncisoTest {
    private val entries = listOf(
        IndexedReference(SearchResult("a", "Pneu em desacordo", "230, XVIII", "672-61", "BR", "test"), "pneu roda"),
        IndexedReference(SearchResult("b", "Outro inciso", "230, XVII", "671-80", "BR", "test"), "outro"),
        IndexedReference(SearchResult("c", "Outro artigo", "231, XVIII", "999-99", "BR", "test"), "outro")
    )

    @Test fun compactIncisoReturnsOnlyRequestedArticleAndInciso() {
        for (query in listOf("230XVIII", "230xviii", "art.230XVIII", "230 XVIII", "230, XVIII")) {
            assertEquals("Failed for: $query", listOf("a"), CatalogSearch.search(entries, query).map { it.stableKey })
        }
    }

    @Test fun compactOtherIncisoDoesNotReturnXVIII() {
        assertEquals(listOf("b"), CatalogSearch.search(entries, "230XVII").map { it.stableKey })
    }

    @Test fun numericInfractionCodeStillWorks() {
        assertEquals(listOf("a"), CatalogSearch.search(entries, "672-61").map { it.stableKey })
    }
}
