package br.com.qru.transito
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test
class NormPdfCatalogTest {
 @Test fun everyRegisteredDocumentIsBundledAndVerified() {
  val catalog=JsonParser.parseString(File("src/main/assets/norms/pdf-catalog.json").readText()).asJsonArray
  assertTrue("Offline catalog must not be empty", catalog.size() >= 14)
  val names=mutableSetOf<String>()
  val titles=mutableSetOf<String>()
  for(entry in catalog) {
   val doc=entry.asJsonObject
   val name=doc["file"].asString
   assertTrue("Duplicate PDF filename: $name",names.add(name))
   val title=doc["title"].asString
   assertTrue("Duplicate catalog title: $title",titles.add(title))
   assertTrue("Non-official source for $title",doc["source"].asString.startsWith("https://www.gov.br/"))
   assertTrue("Invalid SHA-256 metadata for $title",doc["sha256"].asString.matches(Regex("[a-f0-9]{64}")))
   assertTrue(doc["pageCount"].asInt>0)
   val pdf=File("build/generated/normAssets/norms/$name")
   assertTrue("Missing $name",pdf.isFile)
   val digest=MessageDigest.getInstance("SHA-256").digest(pdf.readBytes()).joinToString("") { "%02x".format(it) }
   assertEquals(name,doc["sha256"].asString,digest)
  }
 }
}
