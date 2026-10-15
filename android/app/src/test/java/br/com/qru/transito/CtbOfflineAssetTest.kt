package br.com.qru.transito
import com.google.gson.JsonParser
import java.io.File
import java.util.Base64
import java.util.zip.GZIPInputStream
import org.junit.Assert.*
import org.junit.Test
class CtbOfflineAssetTest {
 @Test fun excludesInfractionChapterAndPreservesCriminalAndGeneralText() {
  val bytes=Base64.getMimeDecoder().decode(File("src/main/assets/ctb/chapters.json.gz.b64").readText())
  val doc=GZIPInputStream(bytes.inputStream()).bufferedReader().use { JsonParser.parseString(it.readText()).asJsonObject }
  val chapters=doc.getAsJsonArray("chapters").map { it.asJsonObject }
  assertEquals(21,chapters.size)
  assertTrue(chapters.none { it["id"].asString=="CAPÍTULO XV" })
  val crime=chapters.single { it["id"].asString=="CAPÍTULO XIX" }
  val text=crime.getAsJsonArray("paragraphs").joinToString { it.asString }
  assertTrue(text.contains("Art. 291."))
  assertTrue(text.contains("Art. 306."))
  assertTrue(text.contains("Art. 312-B."))
  val general=chapters.filter { it["id"].asString!="CAPÍTULO XIX" }.joinToString { it.toString() }
  assertTrue(general.contains("Art. 1"))
  assertTrue(general.contains("Art. 257."))
  assertTrue(general.contains("Art. 270."))
  assertTrue(general.contains("Art. 271."))
  assertFalse(text.contains("\uFFFD"))
  assertTrue(doc["sourceSha256"].asString.matches(Regex("[a-f0-9]{64}")))
 }
}
