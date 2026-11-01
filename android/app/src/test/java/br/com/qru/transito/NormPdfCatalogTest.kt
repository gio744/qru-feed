package br.com.qru.transito
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test
class NormPdfCatalogTest {
 @Test fun everyRegisteredDocumentIsBundledAndVerified() {
  val catalog=JsonParser.parseString(File("src/main/assets/norms/pdf-catalog.json").readText()).asJsonArray
  assertTrue("Catalog must contain at least one document",catalog.size()>0)
  val names=mutableSetOf<String>()
  for(entry in catalog) {
   val doc=entry.asJsonObject
   val name=doc["file"].asString
   assertTrue(names.add(name))
   assertTrue(doc["pageCount"].asInt>0)
   val pdf=File("build/generated/normAssets/norms/$name")
   assertTrue("Missing $name",pdf.isFile)
   assertTrue("PDF too small: $name",pdf.length()>100L)
   assertEquals("Invalid PDF header: $name","%PDF-",pdf.inputStream().use { String(it.readNBytes(5), Charsets.US_ASCII) })
   val digest=MessageDigest.getInstance("SHA-256").digest(pdf.readBytes()).joinToString("") { "%02x".format(it) }
   assertEquals(name,doc["sha256"].asString,digest)
  }
 }
 @Test fun pendingOfficialResolutionsStayConsistentWithOfflineCatalog() {
  val catalog=JsonParser.parseString(File("src/main/assets/norms/pdf-catalog.json").readText()).asJsonArray
  val pending=JsonParser.parseString(File("src/main/assets/norms/pending-official-2026.json").readText()).asJsonObject
  val titles=catalog.map { it.asJsonObject["title"].asString }.toSet()
  val items=pending["items"].asJsonArray
  for (item in items) {
   val entry=item.asJsonObject
   val title="Resolução " + "%,d".format(java.util.Locale.US,entry["number"].asInt).replace(',', '.') + "/" + entry["year"].asInt
   val validation=entry["validation"].asString
   if (validation.startsWith("catalogada_")) {
    assertTrue("Marked catalogued but absent: $title",title in titles)
   } else if (validation.contains("integracao_offline_pendentes")) {
    assertFalse("Marked pending but already catalogued: $title",title in titles)
   }
  }
 }

 @Test fun indexedDocumentsMatchTheirPinnedPdfAndPageCount() {
  val catalog=JsonParser.parseString(File("src/main/assets/norms/pdf-catalog.json").readText()).asJsonArray
  val indexed=catalog.map { it.asJsonObject }.filter { it.has("textIndex") }
  assertTrue("At least one verified text index is expected",indexed.isNotEmpty())
  for (doc in indexed) {
   val indexName=doc["textIndex"].asString
   assertTrue("Unsafe text index name: $indexName",Regex("[A-Za-z0-9]+[.]json").matches(indexName))
   val indexFile=File("src/main/assets/norms/$indexName")
   assertTrue("Missing index: $indexName",indexFile.isFile)
   val index=JsonParser.parseString(indexFile.readText()).asJsonObject
   assertEquals("Wrong PDF for $indexName",doc["sha256"].asString,index["pdfSha256"].asString)
   assertEquals("Wrong page count for $indexName",doc["pageCount"].asInt,index["pages"].asJsonArray.size())
   assertTrue("No searchable text in $indexName",index["pages"].asJsonArray.any { it.asString.isNotBlank() })
  }
 }

 @Test fun resolutionsAreUniqueAndListedChronologically() {
  val catalog=JsonParser.parseString(File("src/main/assets/norms/pdf-catalog.json").readText()).asJsonArray
  val resolutions=catalog.map { it.asJsonObject["title"].asString }
   .filter { it.startsWith("Resolução ") }
  val keys=resolutions.map { title ->
   val match=Regex("""Resolução ([0-9.]+)/([0-9]{4})""").matchEntire(title)
    ?: error("Invalid resolution title: $title")
   val number=match.groupValues[1].replace(".","").toInt()
   val year=match.groupValues[2].toInt()
   year to number
  }
  assertEquals("Duplicate resolution numbers",keys.size,keys.toSet().size)
  assertEquals("Resolutions must be chronological",keys.sortedWith(compareBy<Pair<Int,Int>> { it.first }.thenBy { it.second }),keys)
 }

}
