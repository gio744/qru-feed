package br.com.qru.transito
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.*
import org.junit.Test
class OfflineNormAssetTest {
 @Test fun bundledNormRetainsDocumentIdentityAndAllExtractedPages() {
  val doc=JsonParser.parseString(File("src/main/assets/norms/resolucao10312026.json").readText()).asJsonObject
  val pages=doc.getAsJsonArray("pages")
  assertEquals(20,pages.size())
  assertTrue(pages.all { it.asString.isNotBlank() })
  assertTrue(pages[0].asString.contains("1.031"))
  assertTrue(pages[0].asString.contains("17 DE AGOSTO DE 2026"))
  assertTrue(pages.joinToString { it.asString }.contains("ANEXO"))
  assertTrue(doc["pdfSha256"].asString.matches(Regex("[a-f0-9]{64}")))
  assertTrue(doc["source"].asString.startsWith("https://www.gov.br/"))
 }
}
