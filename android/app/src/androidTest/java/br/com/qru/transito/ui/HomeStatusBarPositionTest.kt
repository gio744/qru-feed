package br.com.qru.transito.ui

import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.qru.transito.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression for HOME overlapping the Android status/notification bar.
 * Device test: must run on an emulator or physical Android device.
 */
@RunWith(AndroidJUnit4::class)
class HomeStatusBarPositionTest {
 @get:Rule val ui = createEmptyComposeRule()

 @Test fun homeButtonStaysBelowStatusBarAndReturnsToSearch() {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  context.getSharedPreferences("qru_news", 0).edit()
   .putStringSet("seen_publications", setOf(CurrentQruNews.id)).commit()
  ActivityScenario.launch(MainActivity::class.java).use {
   val resources = context.resources
   val id = resources.getIdentifier("status_bar_height", "dimen", "android")
   val statusBarHeight = if (id > 0) resources.getDimensionPixelSize(id) else 0
   // Exercise each secondary tab and the Q central, not just CTB.
   listOf("tab_CTB", "tab_NORMAS", "tab_SITUACOES", "tab_NOVIDADES", "tab_BUSCA").forEach { tab ->
    ui.onNodeWithTag(tab).performClick()
    val home = ui.onNodeWithTag("home_top_left")
    val bounds = home.fetchSemanticsNode().layoutInfo.coordinates.boundsInWindow()
    assertTrue(
     "HOME overlaps status bar in $tab: top=${bounds.top}, statusBarHeight=$statusBarHeight",
     bounds.top >= statusBarHeight - 1f
    )
    home.performClick()
    ui.onNodeWithTag("search_query").assertExists()
   }
  }
 }
}
