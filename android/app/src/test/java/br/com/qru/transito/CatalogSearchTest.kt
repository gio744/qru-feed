package br.com.qru.transito

import br.com.qru.transito.domain.model.SearchResult
import br.com.qru.transito.domain.usecase.CatalogSearch
import br.com.qru.transito.domain.usecase.IndexedReference
import org.junit.Assert.*
import org.junit.Test
import com.google.gson.JsonParser
import java.io.File

class CatalogSearchTest {
    private val entries=listOf(
        IndexedReference(SearchResult("alcool","Influência de álcool","165","516-91","BR","draft"),"bafometro alcool etilometro"),
        IndexedReference(SearchResult("recusa","Recusa de procedimento","165-A","757-90","BR","draft"),"recusa bafometro"),
        IndexedReference(SearchResult("cnh","CNH vencida","162, V","504-50","BR","draft"),"carteira vencida habilitacao"),
        IndexedReference(SearchResult("pneu","Conjunto roda-pneu","230, X","660-20","BR","draft"),"roda pneu tamanho")
    )
    @Test fun exactArticleAndIncisoComesBeforeRelatedReferences(){
        val related = listOf(
            IndexedReference(SearchResult("outro","A referência relacionada","230, XVIII","672-61","BR","draft"),"230 V licenciamento"),
            IndexedReference(SearchResult("licenciamento","Veículo sem licenciamento","230, V","659-92","BR","draft"),"licenciamento")
        )
        for(q in listOf("230,V","230, V","art. 230, V","ARTIGO 230 v"))
            assertEquals("licenciamento",CatalogSearch.search(related,q).first().stableKey)
    }
    @Test fun articleDoesNotConfuse165With165A(){assertEquals(listOf("alcool"),CatalogSearch.search(entries,"artigo 165").map{it.stableKey})}
    @Test fun hyphenatedArticleKeepsLetter(){assertEquals(listOf("recusa"),CatalogSearch.search(entries,"165-A").map{it.stableKey})}
    @Test fun codeWithOrWithoutSeparatorFindsSameEntry(){for(q in listOf("504-50","50450"))assertEquals("cnh",CatalogSearch.search(entries,q).first().stableKey)}
    @Test fun accentsDoNotBlockPracticalTerms(){assertEquals(CatalogSearch.search(entries,"bafometro"),CatalogSearch.search(entries,"bafômetro"))}
    @Test fun narrativeFindsReference(){assertEquals("cnh",CatalogSearch.search(entries,"condutor dirigindo com cnh vencida").first().stableKey)}
    @Test fun unknownOrEmptySearchDoesNotReturnAll(){assertTrue(CatalogSearch.search(entries,"").isEmpty());assertTrue(CatalogSearch.search(entries,"abacaxi").isEmpty())}

    @Test fun bundledTyreSearchDoesNotReturnUnrelatedEquipment() {
        val catalog=bundledEntries()
        assertEquals("672-61",CatalogSearch.search(catalog,"pneu").first().code)
        assertEquals("664-50",CatalogSearch.search(catalog,"66450").first().code)
        assertEquals("664-50",CatalogSearch.search(catalog,"230, X").first().code)
        val codes=CatalogSearch.search(catalog,"pneu").mapNotNull { it.code }.toSet()
        assertTrue(codes.containsAll(setOf("661-02","664-50","672-61")))
        assertEquals(setOf("661-02","664-50","672-61","663-71","663-72"),codes)
    }

    private fun bundledEntries(): List<IndexedReference> {
        val json=JsonParser.parseString(File("src/main/assets/initial-index.json").readText()).asJsonObject
        return json.getAsJsonArray("items").map { element ->
            val item=element.asJsonObject
            IndexedReference(SearchResult(item.get("stable_key").asString,item.get("title").asString,
                item.get("article").asString,item.get("code").asString,"BR","test"),item.get("search_text").asString)
        }

    }
    @Test fun practicalParkingTermsDoNotMatchUnrelatedParkingReferences() {
        val catalog=bundledEntries()
        for((query,code) in listOf("garagem" to "546-00","hidrante" to "543-60","rotativo" to "554-12"))
            assertEquals(listOf(code),CatalogSearch.search(catalog,query).map { it.code })
    }

    @Test fun racingSearchDoesNotReturnSeatBeltOrAccidentReferences() {
        assertEquals(listOf("524-00"),CatalogSearch.search(bundledEntries(),"racha").map { it.code })
    }
    @Test fun seatBeltSearchKeepsRacingAndDangerousManoeuvresOut() {
        val codes=CatalogSearch.search(bundledEntries(),"cinto").mapNotNull { it.code }
        assertTrue(codes.containsAll(listOf("518-51","518-52")))
        assertFalse(codes.any { it in setOf("524-00","527-41","527-42","528-20") })
    }
    @Test fun skiddingQueryPrioritizesItsDirectReference() {
        assertEquals("527-42",CatalogSearch.search(bundledEntries(),"derrapagem").first().code)
    }

    @Test fun directCirculationTermsPrioritizeTheirOwnReferences() {
        val catalog=bundledEntries()
        for((query,code) in listOf("rodízio" to "574-62","marcha ré" to "582-70","ordem agente" to "583-50"))
            assertEquals(code,CatalogSearch.search(catalog,query).first().code)
    }

    @Test fun specificOvertakingAndTurnQueriesPrioritizeDirectReferences() {
        val catalog=bundledEntries()
        for((query,code) in listOf("linha amarela contínua" to "596-70",
            "ultrapassagem passagem nível" to "591-62","retorno túnel" to "600-95"))
            assertEquals(code,CatalogSearch.search(catalog,query).first().code)
    }

    @Test fun roundaboutSearchOnlyReturnsItsReference() {
        val catalog=bundledEntries()
        for(query in listOf("rotatória","rotatoria"))
            assertEquals(listOf("617-32"),CatalogSearch.search(catalog,query).map { it.code })
    }
    @Test fun radarSearchDoesNotReturnPriorityOrPropertyAccessReferences() {
        assertEquals(setOf("745-50","746-30","747-10"),
            CatalogSearch.search(bundledEntries(),"radar").mapNotNull { it.code }.toSet())
    }
}
