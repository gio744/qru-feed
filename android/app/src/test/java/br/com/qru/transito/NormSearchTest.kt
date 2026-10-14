package br.com.qru.transito
import br.com.qru.transito.ui.matchesNorm
import org.junit.Assert.*
import org.junit.Test
class NormSearchTest {
 @Test fun numberAcceptsDottedAndPlainForms() {
  for(q in listOf("1031","1.031","1031/2026","resolução 1.031"))
   assertTrue(q,matchesNorm("Resolução 1.031/2026","Álcool",q))
  assertFalse(matchesNorm("Resolução 1.022/2026","Quadriciclos","1031"))
 }
 @Test fun subjectsIgnoreAccentsButRequireEveryQueryTerm() {
  assertTrue(matchesNorm("Resolução 1.012/2024","Estacionamentos específicos e sinalização","estacionamentos sinalizacao"))
  assertFalse(matchesNorm("Resolução 1.012/2024","Estacionamentos específicos e sinalização","estacionamentos álcool"))
  assertTrue(matchesNorm("Resolução 1.013/2024","Sistemas de livre passagem (free flow)","free flow"))
  assertTrue(matchesNorm("Resolução 1.023/2026","Veículos de uso fora de estrada",""))
 }
}
