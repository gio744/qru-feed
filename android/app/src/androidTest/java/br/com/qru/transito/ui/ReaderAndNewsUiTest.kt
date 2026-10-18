package br.com.qru.transito.ui

import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.qru.transito.MainActivity
import br.com.qru.transito.ui.theme.QruTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.test.StandardTestDispatcher

@OptIn(ExperimentalTestApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ReaderAndNewsUiTest {
 // Queue coroutine resumptions instead of recomposing on a PDF I/O worker.
 @get:Rule val ui=createEmptyComposeRule(effectContext=StandardTestDispatcher())

 private fun waitForPage(page:Int) {
  ui.waitUntil(60_000) {
   ui.onAllNodesWithContentDescription("Página $page do PDF original").fetchSemanticsNodes().isNotEmpty()
  }
 }
 @Test fun pinchScrollAndFichaLimitsWorkTogether() {
  ActivityScenario.launch(MainActivity::class.java).use { scenario ->
   scenario.onActivity { activity ->
    activity.setContent { QruTheme {
     OfflinePdfReader(
      expectedHash="5214bc8cfa3c8d1b5acde7c34c3c27f2d06ea1ba1589f5bbfab5ae041e06b1b2",
      assetName="mbvt20222.pdf",title="Ficha de teste",pageCount=830,
      initialPage=599,pageRange=599..601,onDismiss={}
     )
    } }
   }
   waitForPage(599)
   ui.onNodeWithContentDescription("Página 599 do PDF original").performTouchInput {
    val left=Offset(center.x-40f,60f)
    val right=Offset(center.x+40f,60f)
    down(0,left);down(1,right)
    for(step in 1..10) {
     moveTo(0,left-Offset(step*8f,0f))
     moveTo(1,right+Offset(step*8f,0f))
    }
    up(0);up(1)
   }
   ui.waitForIdle()
   ui.onNodeWithText("1.0×").assertDoesNotExist()
   ui.onNodeWithTag("pdf_zoom_reset").performClick()
   ui.onNodeWithText("1.0×").assertIsDisplayed()
   repeat(10) {
    ui.onNodeWithTag("pdf_pages").performTouchInput { swipeUp() }
    ui.waitForIdle()
   }
   ui.onNodeWithText("3 / 3 ▾").assertIsDisplayed()
   ui.onNodeWithContentDescription("Página 602 do PDF original").assertDoesNotExist()
   ui.onNodeWithText("PRÓXIMA").assertIsNotEnabled()
   ui.onNodeWithText("PRIMEIRA").performClick()
   waitForPage(599)
   ui.onNodeWithText("1 / 3 ▾").assertIsDisplayed()
  }
 }
 @Test fun dismissedNewsStaysDismissedAfterActivityRecreation() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val prefs=context.getSharedPreferences("qru_news",0)
  prefs.edit().clear().commit()
  ActivityScenario.launch(MainActivity::class.java).use { scenario ->
   ui.onNodeWithText("Novidades do QRU").assertIsDisplayed()
   ui.onNodeWithText("CONTINUAR").performClick()
   ui.waitForIdle()
   assertTrue(prefs.getStringSet("seen_publications",emptySet()).orEmpty().isNotEmpty())
   scenario.recreate()
   ui.waitUntil(10_000) {
    ui.onAllNodesWithText("QUAL A SITUAÇÃO").fetchSemanticsNodes().isNotEmpty()
   }
   ui.waitForIdle()
   ui.onNodeWithText("Novidades do QRU").assertDoesNotExist()
  }
 }

 @Test fun automaticSearchOpensOnlyItsFichaAndKeepsQueryOnReturn() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  context.getSharedPreferences("qru_news",0).edit()
   .putStringSet("seen_publications",setOf(CurrentQruNews.id)).commit()
  ActivityScenario.launch(MainActivity::class.java).use {
   ui.onNodeWithTag("search_query").performTextReplacement("pneu")
   ui.waitUntil(10_000) {
    ui.onAllNodesWithText("672-61",substring=true).fetchSemanticsNodes().isNotEmpty()
   }
   ui.onAllNodesWithTag("reference_card").onFirst().assertTextContains("672-61",substring=true)
   androidx.test.espresso.Espresso.closeSoftKeyboard()
   ui.onNodeWithTag("search_results").performScrollToIndex(1)
   ui.onAllNodesWithTag("reference_card").onFirst().performClick()
   ui.waitUntil(10_000) {
    ui.onAllNodesWithText("ABRIR FICHA").fetchSemanticsNodes().isNotEmpty()
   }
   ui.onNodeWithText("ABRIR FICHA").performClick()
   waitForPage(599)
   ui.onNodeWithText("1 / 3 ▾").assertIsDisplayed()
   ui.onNodeWithContentDescription("Página 1 do PDF original").assertDoesNotExist()
   androidx.test.espresso.Espresso.pressBack()
   ui.onNodeWithText("ABRIR FICHA").assertExists()
   androidx.test.espresso.Espresso.pressBack()
   ui.onNodeWithTag("search_results").performScrollToIndex(0)
   ui.onNodeWithTag("search_query").assertTextContains("pneu")
   ui.onAllNodesWithTag("reference_card").onFirst().assertTextContains("672-61",substring=true)
   ui.onNodeWithTag("search_query").performTextReplacement("230,v")
   ui.waitUntil(10_000) {
    ui.onAllNodesWithText("659-92",substring=true).fetchSemanticsNodes().isNotEmpty()
   }
   ui.onAllNodesWithTag("reference_card").onFirst().assertTextContains("659-92",substring=true)
  }
 }

 @Test fun situationShortcutKeepsMainSearchIndependent() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  context.getSharedPreferences("qru_news",0).edit()
   .putStringSet("seen_publications",setOf(CurrentQruNews.id)).commit()
  ActivityScenario.launch(MainActivity::class.java).use {
   ui.onNodeWithTag("search_query").performTextReplacement("licenciamento")
   ui.waitUntil(10_000) {
    ui.onAllNodesWithText("659-92",substring=true).fetchSemanticsNodes().isNotEmpty()
   }
   androidx.test.espresso.Espresso.closeSoftKeyboard()
   ui.onNodeWithTag("tab_SITUACOES").performClick()
   ui.onNodeWithTag("situation_pneu").performScrollTo().performClick()
   ui.waitUntil(10_000) {
    ui.onAllNodesWithText("Referências para: pneu").fetchSemanticsNodes().isNotEmpty() &&
     ui.onAllNodesWithTag("situations_loading").fetchSemanticsNodes().isEmpty()
   }
   ui.onNodeWithTag("situations_results").performScrollToIndex(1)
   ui.onAllNodesWithTag("reference_card").onFirst().assertTextContains("672-61",substring=true)
   ui.onAllNodesWithTag("reference_card").onFirst().performClick()
   ui.waitUntil(10_000) {
    ui.onAllNodesWithText("ABRIR FICHA").fetchSemanticsNodes().isNotEmpty()
   }
   androidx.test.espresso.Espresso.pressBack()
   ui.onNodeWithTag("tab_BUSCA").performClick()
   ui.onNodeWithTag("search_results").performScrollToIndex(0)
   ui.onNodeWithTag("search_query").assertTextContains("licenciamento")
   ui.onAllNodesWithTag("reference_card").onFirst().assertTextContains("659-92",substring=true)
   ui.onNodeWithTag("tab_SITUACOES").performClick()
   ui.onAllNodesWithTag("reference_card").onFirst().assertTextContains("672-61",substring=true)
  }
 }
}
