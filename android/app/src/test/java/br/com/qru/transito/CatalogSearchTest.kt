package br.com.qru.transito

import br.com.qru.transito.domain.model.SearchResult
import br.com.qru.transito.domain.usecase.CatalogSearch
import br.com.qru.transito.domain.usecase.IndexedReference
import org.junit.Assert.*
import org.junit.Test

class CatalogSearchTest {
    private val entries=listOf(
        IndexedReference(SearchResult("alcool","Influência de álcool","165","516-91","BR","draft"),"bafometro alcool etilometro"),
        IndexedReference(SearchResult("recusa","Recusa de procedimento","165-A","757-90","BR","draft"),"recusa bafometro"),
        IndexedReference(SearchResult("cnh","CNH vencida","162, V","504-50","BR","draft"),"carteira vencida habilitacao"),
        IndexedReference(SearchResult("pneu","Conjunto roda-pneu","230, X","660-20","BR","draft"),"roda pneu tamanho")
    )
    @Test fun articleDoesNotConfuse165With165A(){assertEquals(listOf("alcool"),CatalogSearch.search(entries,"artigo 165").map{it.stableKey})}
    @Test fun hyphenatedArticleKeepsLetter(){assertEquals(listOf("recusa"),CatalogSearch.search(entries,"165-A").map{it.stableKey})}
    @Test fun codeWithOrWithoutSeparatorFindsSameEntry(){for(q in listOf("504-50","50450"))assertEquals("cnh",CatalogSearch.search(entries,q).first().stableKey)}
    @Test fun accentsDoNotBlockPracticalTerms(){assertEquals(CatalogSearch.search(entries,"bafometro"),CatalogSearch.search(entries,"bafômetro"))}
    @Test fun narrativeFindsReference(){assertEquals("cnh",CatalogSearch.search(entries,"condutor dirigindo com cnh vencida").first().stableKey)}
    @Test fun unknownOrEmptySearchDoesNotReturnAll(){assertTrue(CatalogSearch.search(entries,"").isEmpty());assertTrue(CatalogSearch.search(entries,"abacaxi").isEmpty())}
}
