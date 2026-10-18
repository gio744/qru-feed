package br.com.qru.transito.ui

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ReferencePageRangeTest {
 private val docs=Gson().fromJson(File("src/main/assets/norms/pdf-catalog.json").readText(),Array<OfflinePdfDocument>::class.java).toList()
 private val ranges=Gson().fromJson(File("src/main/assets/norms/reference-page-ranges.json").readText(),Array<ReferencePageRange>::class.java).toList()
 private val source=docs.single { it.file=="mbvt20222.pdf" }.source+"#page=599"

 @Test fun tyreReferenceOpensOnlyItsThreePages() {
  val target=resolveOfflineReference(docs,ranges,source,"672-61")!!
  assertEquals(599..601,target.range)
 }
 @Test fun wrongCodeOrSourceAnchorDoesNotOpenAnotherFicha() {
  assertNull(resolveOfflineReference(docs,ranges,source,"659-92"))
  assertNull(resolveOfflineReference(docs,ranges,source.replace("599","600"),"672-61"))
  assertNull(resolveOfflineReference(docs,ranges,source.substringBefore("#"),"672-61"))
 }
 @Test fun staleOrInvalidRangeFailsClosed() {
  val range=ranges.single { it.code=="672-61" }
  assertNull(resolveOfflineReference(docs,listOf(range.copy(sha256="a".repeat(64))),source,"672-61"))
  assertNull(resolveOfflineReference(docs,listOf(range.copy(lastPage=831)),source,"672-61"))
  assertNull(resolveOfflineReference(docs,listOf(range,range),source,"672-61"))
 }
 @Test fun allBundledReferenceRangesResolveAgainstExactCandidateSource() {
  val items=JsonParser.parseString(File("src/main/assets/initial-index.json").readText()).asJsonObject["items"].asJsonArray
  val covered=items.filter { resolveOfflineReference(docs,ranges,it.asJsonObject["source"].asString,it.asJsonObject["code"].asString)!=null }
  assertEquals(416,covered.size)
  assertEquals(416,ranges.size)
  assertEquals(setOf("757-90","551-71"),items.filterNot { it in covered }.map { it.asJsonObject["code"].asString }.toSet())
 }
 @Test fun windowConvertsOnlyItsOwnPages() {
  val window=PdfPageWindow(599,601,830)
  assertEquals(3,window.count)
  assertEquals(598,window.documentIndex(0))
  assertEquals(600,window.documentIndex(2))
  assertEquals(2,window.listIndex(601))
 }
 @Test(expected=IllegalArgumentException::class) fun cannotScrollPastFicha() { PdfPageWindow(599,601,830).documentIndex(3) }
 @Test(expected=IllegalArgumentException::class) fun cannotJumpToAnotherFicha() { PdfPageWindow(599,601,830).listIndex(602) }
 @Test(expected=IllegalArgumentException::class) fun cannotOpenRangeOutsideDocument() { PdfPageWindow(829,831,830) }
 @Test fun completeDocumentKeepsAllPagesAvailable() {
  val window=PdfPageWindow(1,830,830)
  assertEquals(830,window.count)
  assertEquals(829,window.documentIndex(829))
 }
 @Test fun startupNewsSourceMatchesBundledPublication() {
  val document=docs.single { it.file==CurrentQruNews.sourceFile }
  assertEquals(document.sha256,CurrentQruNews.sourceHash)
  assertEquals(document.pageCount,CurrentQruNews.sourcePages)
 }
}
