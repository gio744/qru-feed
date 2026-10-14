package br.com.qru.transito
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test
class NormPdfCatalogTest {
 @Test fun everyRegisteredDocumentIsBundledAndVerified() {
  val catalog=JsonParser.parseString(File("src/main/assets/norms/pdf-catalog.json").readText()).asJsonArray
  assertEquals(7,catalog.size())
  val names=mutableSetOf<String>()
  for(entry in catalog) {
   val doc=entry.asJsonObject
   val name=doc["file"].asString
   assertTrue(names.add(name))
   assertTrue(doc["pageCount"].asInt>0)
   val pdf=File("build/generated/normAssets/norms/$name")
   assertTrue("Missing $name",pdf.isFile)
   val digest=MessageDigest.getInstance("SHA-256").digest(pdf.readBytes()).joinToString("") { "%02x".format(it) }
   assertEquals(name,doc["sha256"].asString,digest)
  }
 }
}
