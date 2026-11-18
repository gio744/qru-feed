package br.com.qru.transito

import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Regression guard: identity verification must never silently publish operational guidance. */
class OperationalApprovalGateTest {
    @Test fun priorityAlcoholAndRefusalReferencesRemainUnderReview() {
        val root = JsonParser.parseString(File("src/main/assets/initial-index.json").readText()).asJsonObject
        val items = root.getAsJsonArray("items").map { it.asJsonObject }
        val codes = setOf("516-91", "516-92", "757-91", "757-92")
        val selected = items.filter { it.get("code").asString in codes }
        assertEquals("All four priority references must be present", codes, selected.map { it.get("code").asString }.toSet())
        selected.forEach { item ->
            assertFalse("Not operationally approved: ${item.get("code").asString}", item.get("ready").asBoolean)
            assertTrue("Review status required", item.get("review_status").asString.isNotBlank())
            assertTrue("Official source required", item.get("source").asString.startsWith("https://"))
        }
    }

    @Test fun editorialPackageMustNotPublishUnapprovedGuidance() {
        val json = JsonParser.parseString(
            File("src/main/assets/operational/approved-fiches.json").readText()
        ).asJsonObject
        assertEquals(1, json.get("schemaVersion").asInt)
        assertEquals("No operational fiche has been approved for publication", 0, json.getAsJsonArray("items").size())
    }
}
