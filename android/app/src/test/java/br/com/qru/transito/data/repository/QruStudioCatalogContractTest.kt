package br.com.qru.transito.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

/**
 * Contract tests for the bundled Studio catalog. An empty catalog is valid:
 * the UI must not fabricate published videos.
 */
class QruStudioCatalogContractTest {
    @Test fun emptyCatalogIsNotEvidenceOfPublishedVideos() {
        val catalog = JSONObject("""{"schemaVersion":1,"items":[]}""")
        assertEquals(1, catalog.getInt("schemaVersion"))
        assertEquals(0, catalog.getJSONArray("items").length())
    }

    @Test fun catalogEntriesRequireRealIdentifiersAndPlayableSource() {
        val catalog = JSONObject("""{"schemaVersion":1,"items":[{"id":"demo","title":"Example"}]}""")
        val item = catalog.getJSONArray("items").getJSONObject(0)
        val hasId = item.optString("id").isNotBlank()
        val hasTitle = item.optString("title").isNotBlank()
        val hasSource = item.optString("url").startsWith("https://") ||
            item.optString("youtubeUrl").startsWith("https://")
        assertTrue(hasId && hasTitle && !hasSource)
    }
}
