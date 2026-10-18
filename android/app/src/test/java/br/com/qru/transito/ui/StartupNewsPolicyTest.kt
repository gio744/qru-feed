package br.com.qru.transito.ui

import org.junit.Assert.*
import org.junit.Test

class StartupNewsPolicyTest {
 @Test fun newPublicationIsShown() { assertTrue(shouldShowStartupNews("new",emptySet())) }
 @Test fun acknowledgedPublicationIsNotShownAgain() {
  val seen=seenNewsAfterDismissal(emptySet(),"current",true)
  assertFalse(shouldShowStartupNews("current",seen))
 }
 @Test fun aNewPublicationStillAppears() {
  val seen=seenNewsAfterDismissal(emptySet(),"previous",true)
  assertTrue(shouldShowStartupNews("new",seen))
 }
 @Test fun uncheckingAllowsAnotherSessionToShowIt() {
  val seen=seenNewsAfterDismissal(setOf("previous"),"current",false)
  assertEquals(setOf("previous"),seen)
  assertTrue(shouldShowStartupNews("current",seen))
 }
 @Test fun acknowledgmentIsIdempotentAndKeepsHistory() {
  val seen=seenNewsAfterDismissal(setOf("previous","current"),"current",true)
  assertEquals(setOf("previous","current"),seen)
 }
 @Test fun invalidPublicationDoesNotCreateAnAlertOrHistoryEntry() {
  assertFalse(shouldShowStartupNews("",emptySet()))
  assertEquals(setOf("previous"),seenNewsAfterDismissal(setOf("previous"),"",true))
 }
}
