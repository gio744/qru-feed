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

@RunWith(AndroidJUnit4::class)
class ReaderAndNewsUiTest {
 @get:Rule val ui=createEmptyComposeRule()

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
   ui.onNodeWithText("QUAL A SITUAÇÃO").assertIsDisplayed()
   ui.onNodeWithText("Novidades do QRU").assertDoesNotExist()
  }
 }
}
