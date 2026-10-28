package br.com.qru.transito.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract checks against the real Studio catalog parser. */
class QruStudioCatalogContractTest {
    @Test fun emptyCatalogDoesNotPublishPlaceholderVideos() {
        assertTrue(QruVideoCatalog.published("""{"schemaVersion":1,"items":[]}""").isEmpty())
    }

    @Test fun incompleteEditorialEntryIsNotPublished() {
        val incomplete = """{"schemaVersion":1,"items":[{"id":"example","title":"Example"}]}"""
        assertTrue(QruVideoCatalog.published(incomplete).isEmpty())
    }

    @Test fun untrustedMediaHostCannotBeUsedForDownload() {
        assertTrue(!QruVideoCatalog.trustedMediaUrl("https://media.qru.app.br.evil.example/video.mp4"))
        assertTrue(!QruVideoCatalog.trustedMediaUrl("http://media.qru.app.br/video.mp4"))
        assertTrue(QruVideoCatalog.trustedMediaUrl("https://media.qru.app.br/video.mp4"))
    }

    @Test fun unsupportedCatalogSchemaIsRejected() {
        assertEquals(0, QruVideoCatalog.published("""{"schemaVersion":2,"items":[]}""").size)
    }
}
