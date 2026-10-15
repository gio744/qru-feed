package br.com.qru.transito
import br.com.qru.transito.ui.matchesCtbParagraph
import org.junit.Assert.*
import org.junit.Test
class CtbSearchTest {
 @Test fun articleNumberDoesNotMatchPrefixOrBodyCitation() {
  for(q in listOf("30","art. 30","Artigo 30")) {
   assertTrue(q,matchesCtbParagraph("Art. 30. Texto",q))
   assertFalse(q,matchesCtbParagraph("Art. 300. Texto",q))
   assertFalse(q,matchesCtbParagraph("Art. 301. Texto",q))
   assertFalse(q,matchesCtbParagraph("Art. 10. Conforme o art. 30.",q))
  }
 }
 @Test fun letterSuffixIsExactAndCaseInsensitive() {
  assertTrue(matchesCtbParagraph("Art. 312-B. Texto","312-b"))
  assertFalse(matchesCtbParagraph("Art. 312. Texto","312-B"))
  assertFalse(matchesCtbParagraph("Art. 312-B. Texto","312"))
  assertTrue(matchesCtbParagraph("Art. 1º Texto","1"))
 }
 @Test fun practicalWordsStillIgnoreAccents() {
  assertTrue(matchesCtbParagraph("Condução sob influência de álcool","alcool"))
  assertFalse(matchesCtbParagraph("Condução sob influência de álcool","pneu"))
 }
}
