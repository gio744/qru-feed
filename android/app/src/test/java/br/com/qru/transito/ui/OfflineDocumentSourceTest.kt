package br.com.qru.transito.ui

import com.google.gson.Gson
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class OfflineDocumentSourceTest {
 private val source="https://www.gov.br/transportes/manual.pdf"
 private val doc=OfflinePdfDocument(file="manual.pdf",title="Original",source=source,sha256="a".repeat(64),pageCount=830)
 private fun resolve(url:String)=resolveOfflineDocument(listOf(doc),url)

 @Test fun opensExactPage() {
  val target=resolve("$source#page=543")!!
  assertEquals(543,target.page)
  assertEquals("manual.pdf",target.document.file)
 }
 @Test fun noFragmentOpensFirstPage() { assertEquals(1,resolve(source)!!.page) }
 @Test fun lastPageIsAllowed() { assertEquals(830,resolve("$source#page=830")!!.page) }
 @Test fun explicitHttpsPortAndHostCaseAreEquivalent() {
  assertEquals(1,resolve("https://WWW.GOV.BR:443/transportes/manual.pdf")!!.page)
 }
 @Test fun doesNotSubstituteBaseManualForAnotherPublication() {
  assertNull(resolve("https://www.gov.br/transportes/newer-resolution.pdf#page=543"))
 }
 @Test fun rejectsUntrustedOrAmbiguousUrls() {
  listOf("http://www.gov.br/transportes/manual.pdf", "https://www.gov.br.evil.test/transportes/manual.pdf",
   "https://www.gov.br@evil.test/transportes/manual.pdf", "https://user@www.gov.br/transportes/manual.pdf",
   "https://www.gov.br:8443/transportes/manual.pdf", "$source?version=other", "not a URL").forEach { assertNull(it,resolve(it)) }
 }
 @Test fun rejectsInvalidPageInsteadOfOpeningWrongPage() {
  listOf("0","831","-1","1.5","abc","999999999999999999999999").forEach { assertNull(it,resolve("$source#page=$it")) }
  assertNull(resolve("$source#nameddest=other"))
  assertNull(resolve("$source#page=1&zoom=200"))
 }
 @Test fun pathsAndFileNamesRemainCaseSensitive() { assertNull(resolve(source.replace("manual.pdf","Manual.pdf"))) }
 @Test fun invalidManifestEntryCannotResolve() {
  assertNull(resolveOfflineDocument(listOf(doc.copy(file="../manual.pdf")),source))
  assertNull(resolveOfflineDocument(listOf(doc.copy(sha256="bad")),source))
  assertNull(resolveOfflineDocument(listOf(doc.copy(pageCount=0)),source))
 }
 @Test fun bundledManifestResolvesEveryOriginalAndFinalPage() {
  val docs=Gson().fromJson(File("src/main/assets/norms/pdf-catalog.json").readText(),Array<OfflinePdfDocument>::class.java).toList()
  assertTrue("Catalog must contain documents",docs.isNotEmpty())
  docs.forEach { entry ->
   assertEquals(entry.file,resolveOfflineDocument(docs,entry.source)!!.document.file)
   assertEquals(entry.pageCount,resolveOfflineDocument(docs,"${entry.source}#page=${entry.pageCount}")!!.page)
  }
  assertEquals("Manuais",docs.single { it.file=="mbvt20222.pdf" }.category)
  assertTrue(docs.filter { it.file!="mbvt20222.pdf" }.all { it.category=="Resoluções" })
 }
 @Test fun consultationDraftStaysExternal() {
  assertNull(resolve("https://www.gov.br/participamaisbrasil/blob/baixar/6392"))
 }
 @Test fun rejectsAmbiguousManifestMappings() {
  val conflicting=doc.copy(file="another.pdf",title="Duplicate source")
  assertNull(resolveOfflineDocument(listOf(doc,conflicting),source))
  assertNotNull(resolveOfflineDocument(listOf(doc),source))
 }

}
