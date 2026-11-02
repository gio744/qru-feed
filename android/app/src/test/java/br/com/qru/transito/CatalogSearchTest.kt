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
    @Test fun exact230IncisoVReturnsAllMatchingBreakdownsOnly() {
        val references = listOf(
            IndexedReference(SearchResult("a", "Licenciamento A", "230, V", "659-91", "BR", "draft"), "licenciamento"),
            IndexedReference(SearchResult("b", "Licenciamento B", "230, V", "659-92", "BR", "draft"), "licenciamento"),
            IndexedReference(SearchResult("c", "Outro inciso", "230, XVIII", "672-61", "BR", "draft"), "230 V licenciamento")
        )
        for (query in listOf("230, V", "230 V", "art. 230, V")) {
            assertEquals(setOf("659-91", "659-92"),
                CatalogSearch.search(references, query).mapNotNull { it.code }.toSet())
        }
    }
    @Test fun bundled230IncisoVContainsBothRealOfflineFiches() {
        val catalog = bundledEntries()
        val expected = setOf("659-91", "659-92")
        assertEquals(expected, catalog.filter { it.result.article == "230, V" }
            .mapNotNull { it.result.code }.toSet())
        for (query in listOf("230, V", "230 V", "art. 230, V")) {
            assertEquals(expected, CatalogSearch.search(catalog, query)
                .mapNotNull { it.code }.toSet())
        }
    }
    @Test fun fullArticle230KeepsLegalIncisoOrderAndExcludesOtherArticles() {
        val results = CatalogSearch.search(bundledEntries(), "art. 230")
        assertTrue("O artigo 230 deve ter desdobramentos", results.size > 20)
        assertTrue("A busca por artigo não deve misturar outros artigos",
            results.all { it.article?.substringBefore(',')?.trim() == "230" })
        val romanOrder = mapOf(
            "I" to 1, "II" to 2, "III" to 3, "IV" to 4, "V" to 5,
            "VI" to 6, "VII" to 7, "VIII" to 8, "IX" to 9, "X" to 10,
            "XI" to 11, "XII" to 12, "XIII" to 13, "XIV" to 14,
            "XV" to 15, "XVI" to 16, "XVII" to 17, "XVIII" to 18,
            "XIX" to 19, "XX" to 20, "XXI" to 21, "XXII" to 22,
            "XXIII" to 23
        )
        val order = results.map { romanOrder[it.article?.substringAfter(',')?.trim()] ?: Int.MAX_VALUE }
        assertEquals("Incisos devem aparecer em ordem jurídica, não alfabética",
            order.sorted(), order)
    }
    @Test fun articleDoesNotConfuse165With165A(){assertEquals(listOf("alcool"),CatalogSearch.search(entries,"artigo 165").map{it.stableKey})}
    @Test fun hyphenatedArticleKeepsLetter(){assertEquals(listOf("recusa"),CatalogSearch.search(entries,"165-A").map{it.stableKey})}
    @Test fun exactCodeStillMatchesWhenItsDigitsResembleArticleAndInciso() {
        val references = listOf(
            IndexedReference(SearchResult("coded", "Código operacional", "230, XVIII", "230-V", "BR", "draft"), "referencia"),
            IndexedReference(SearchResult("article", "Artigo e inciso", "230, V", "659-92", "BR", "draft"), "referencia")
        )
        assertEquals(listOf("coded", "article"), CatalogSearch.search(references, "230 V").map { it.stableKey })
    }
    @Test fun codeWithOrWithoutSeparatorFindsSameEntry(){for(q in listOf("504-50","50450"))assertEquals("cnh",CatalogSearch.search(entries,q).first().stableKey)}
    @Test fun accentsDoNotBlockPracticalTerms(){assertEquals(CatalogSearch.search(entries,"bafometro"),CatalogSearch.search(entries,"bafômetro"))}
    @Test fun narrativeFindsReference(){assertEquals("cnh",CatalogSearch.search(entries,"condutor dirigindo com cnh vencida").first().stableKey)}
    @Test fun unknownOrEmptySearchDoesNotReturnAll(){assertTrue(CatalogSearch.search(entries,"").isEmpty());assertTrue(CatalogSearch.search(entries,"abacaxi").isEmpty())}

    @Test fun exactTyreArticleAndIncisoFindsItsOwnFiche() {
        val catalog = bundledEntries()
        for (query in listOf("230 XVIII", "230, XVIII", "art. 230, XVIII", "672-61")) {
            assertEquals("672-61", CatalogSearch.search(catalog, query).first().code)
        }
    }

    @Test fun bundledTyreSearchDoesNotReturnUnrelatedEquipment() {
        val catalog=bundledEntries()
        assertEquals("672-61",CatalogSearch.search(catalog,"pneu").first().code)
        assertEquals("664-50",CatalogSearch.search(catalog,"66450").first().code)
        assertEquals("664-50",CatalogSearch.search(catalog,"230, X").first().code)
        val codes=CatalogSearch.search(catalog,"pneu").mapNotNull { it.code }.toSet()
        assertTrue(codes.containsAll(setOf("661-02","664-50","672-61")))
        assertEquals(setOf("661-02","664-50","672-61","663-71","663-72"),codes)
    }

    @Test fun alcoholRefusalCatalogKeepsNewCodesAndLabelsLegacyAsHistorical() {
        val json=JsonParser.parseString(File("src/main/assets/initial-index.json").readText()).asJsonObject
        val refusal=json.getAsJsonArray("items").map { it.asJsonObject }
            .filter { it.get("article").asString=="165-A" }
        val byCode=refusal.associateBy { it.get("code").asString }
        assertTrue(byCode.keys.containsAll(setOf("757-91","757-92")))
        assertTrue(byCode["757-91"]!!.get("title").asString.contains("álcool"))
        assertTrue(byCode["757-92"]!!.get("title").asString.contains("substância"))
        assertTrue(byCode["757-90"]!!.get("title").asString.contains("anterior"))
        assertEquals("757-91",CatalogSearch.search(bundledEntries(),"75791").first().code)
        assertEquals("757-92",CatalogSearch.search(bundledEntries(),"75792").first().code)
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
        assertEquals(setOf("745-50","746-30","747-10","657-20"),
            CatalogSearch.search(bundledEntries(),"radar").mapNotNull { it.code }.toSet())
    }
    @Test fun refusalTransitionMetadataKeepsHistoricalAndNewFichesDistinct() {
        val root=JsonParser.parseString(File("src/main/assets/initial-index.json").readText()).asJsonObject
        val items=root.getAsJsonArray("items").map { it.asJsonObject }
        val byCode=items.filter { it.get("code").asString in setOf("757-90","757-91","757-92") }
            .associateBy { it.get("code").asString }
        assertEquals(setOf("757-90","757-91","757-92"),byCode.keys)
        val previous=byCode.getValue("757-90")
        assertEquals("2026-10-16",previous.get("applies_until").asString)
        assertTrue(previous.get("title").asString.contains("anterior",ignoreCase=true))
        for(code in listOf("757-91","757-92")) {
            val current=byCode.getValue(code)
            assertEquals("2026-10-17",current.get("applies_from").asString)
            assertEquals("",current.get("applies_until").asString)
            assertFalse(current.get("search_text").asString.contains("vigência futura",ignoreCase=true))
            assertFalse(current.get("ready").asBoolean)
        }
        assertFalse(previous.get("ready").asBoolean)
    }

    @Test fun refusalTransitionDirectCodeSearchNeverSubstitutesAnotherFiche() {
        val catalog=bundledEntries()
        for(code in listOf("757-90","757-91","757-92")) {
            val results=CatalogSearch.search(catalog,code)
            assertEquals("Consulta direta deve preservar o código $code",
                listOf(code),results.mapNotNull { it.code })
            assertEquals("165-A",results.single().article)
        }
        val refusalArticle=CatalogSearch.search(catalog,"artigo 165-A")
        assertTrue(refusalArticle.any { it.code=="757-90" })
        assertTrue(refusalArticle.any { it.code=="757-91" })
        assertTrue(refusalArticle.any { it.code=="757-92" })
        assertTrue("Art. 165-A não deve misturar art. 165",
            refusalArticle.all { it.article=="165-A" })
    }

    @Test fun exactInfractionCodeDoesNotReturnSiblingCodesWithSharedPrefix() {
        val references=listOf(
            IndexedReference(SearchResult("old","Recusa anterior","165-A","757-90","BR","test"),"recusa bafometro"),
            IndexedReference(SearchResult("alcohol","Recusa álcool","165-A","757-91","BR","test"),"recusa bafometro"),
            IndexedReference(SearchResult("substance","Recusa substância","165-A","757-92","BR","test"),"recusa substancia")
        )
        for(code in listOf("757-90","757-91","757-92")) {
            assertEquals(listOf(code),CatalogSearch.search(references,code).mapNotNull { it.code })
            assertEquals(listOf(code),CatalogSearch.search(references,code.replace("-","")).mapNotNull { it.code })
            assertEquals(listOf(code),CatalogSearch.search(references,code.replace("-"," - ")).mapNotNull { it.code })
        }
        assertEquals(setOf("757-90","757-91","757-92"),
            CatalogSearch.search(references,"recusa").mapNotNull { it.code }.toSet())
    }

}
