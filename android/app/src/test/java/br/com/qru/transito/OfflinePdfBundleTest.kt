package br.com.qru.transito
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test
class OfflinePdfBundleTest {
 @Test fun originalPdfIsBundledAndMatchesDocumentFingerprint() {
  val metadata=JsonParser.parseString(File("src/main/assets/norms/resolucao10312026.json").readText()).asJsonObject
  val pdf=File("build/generated/normAssets/norms/resolucao10312026.pdf")
  assertTrue("Offline PDF was not included by build",pdf.isFile)
  val bytes=pdf.readBytes()
  assertTrue(String(bytes.take(5).toByteArray(),Charsets.US_ASCII)=="%PDF-")
  val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
  assertEquals(metadata["pdfSha256"].asString,digest)
 }
}
