package br.com.qru.transito

import br.com.qru.transito.domain.model.SearchResult
import br.com.qru.transito.domain.usecase.CatalogSearch
import br.com.qru.transito.domain.usecase.IndexedReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression fixtures are synthetic: no legal conclusion or current-law validation. */
class QruSearchRegressionMatrixTest {
    private fun ref(key: String, article: String, terms: String = "") =
        IndexedReference(SearchResult(key, key, article, key, "BR", "test"), terms)

    private val catalog = listOf(
        ref("230-v", "230, V", "licenciamento"),
        ref("230-xviii", "230, XVIII", "pneu roda"),
        ref("165", "165", "alcool"),
        ref("165-a", "165-A", "recusa bafometro"),
        ref("252-iii", "252, III"),
        ref("244-x", "244, X"),
        ref("231-viii", "231, VIII")
    )

    @Test fun exactIncisosDoNotCollide() {
        for ((query, key) in listOf(
            "230 V" to "230-v", "230, XVIII" to "230-xviii",
            "252 III" to "252-iii", "244 X" to "244-x",
            "231 VIII" to "231-viii"
        )) assertEquals(listOf(key), CatalogSearch.search(catalog, query).map { it.stableKey })
    }

    @Test fun article165DoesNotInclude165A() {
        assertEquals(listOf("165"), CatalogSearch.search(catalog, "165").map { it.stableKey })
        assertEquals(listOf("165-a"), CatalogSearch.search(catalog, "165-A").map { it.stableKey })
    }

    @Test fun tyreAndWheelPreferDirectReference() {
        for (query in listOf("pneu", "roda"))
            assertEquals("230-xviii", CatalogSearch.search(catalog, query).first().stableKey)
    }

    @Test fun emptyAndUnknownNeverInventResults() {
        assertTrue(CatalogSearch.search(catalog, "").isEmpty())
        assertTrue(CatalogSearch.search(catalog, "termo-inexistente-xyz").isEmpty())
    }

    @Test fun diacriticsAreNormalizedConsistently() {
        assertEquals(CatalogSearch.search(catalog, "álcool"), CatalogSearch.search(catalog, "alcool"))
    }
}
