package br.com.qru.transito
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.*
import org.junit.Test
class OfflineNormAssetTest {
 @Test fun bundledNormRetainsIdentityBodyAndImageOnlyPagePositions() {
  val doc=JsonParser.parseString(File("src/main/assets/norms/resolucao10312026.json").readText()).asJsonObject
  val pages=doc.getAsJsonArray("pages")
  assertEquals(20,pages.size())
  // These annex pages are images in the source. Keep their positions, not fabricated text.
  assertEquals((11..18).toList(),pages.mapIndexedNotNull { i,p -> if(p.asString.isBlank()) i else null })
  assertTrue(pages[0].asString.contains("1.031"))
  assertTrue(pages[0].asString.contains("17 DE AGOSTO DE 2026"))
  val body=pages.take(5).joinToString { it.asString }
  for(article in 1..16) {
   val suffix=if(article<10) "º" else "."
   assertTrue("Missing article $article",body.contains("Art. $article$suffix"))
  }
  assertTrue(doc["pdfSha256"].asString.matches(Regex("[a-f0-9]{64}")))
  assertTrue(doc["source"].asString.startsWith("https://www.gov.br/"))
 }
}
