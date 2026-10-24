package br.com.qru.transito.data.repository

import org.junit.Assert.*
import org.junit.Test

class QruVideoCatalogTest {
    private fun packageJson(url: String = "https://media.qru.app.br/videos/recusa.mp4",
        reviewedBy: String = "Revisor técnico") = """
        {"schemaVersion":1,"items":[{
          "id":"recusa-etilometro","title":"Recusa ao etilômetro",
          "description":"Simulação educativa","legalReference":"CTB arts. 165 e 165-A",
          "reviewedBy":"$reviewedBy","reviewedAt":"2026-10-22",
          "videoUrl":"$url","durationSeconds":60,
          "sha256":"${"a".repeat(64)}",
          "youtubeUrl":"https://www.youtube.com/watch?v=abc123"
        }]}
    """.trimIndent()

    @Test fun acceptsReviewedVideoWithOwnedMediaHost() {
        val videos = QruVideoCatalog.published(packageJson())
        assertEquals(1, videos.size)
        assertEquals("recusa-etilometro", videos.first().id)
    }

    @Test fun blocksExternalAndSpoofedDownloadSources() {
        listOf(
            "https://youtube.com/watch?v=abc",
            "https://media.qru.app.br.evil.example/clip.mp4",
            "https://media.qru.app.br@evil.example/clip.mp4",
            "http://media.qru.app.br/clip.mp4",
            "https://media.qru.app.br:8443/clip.mp4",
            "https://media.qru.app.br/clip.mp4#fragment"
        ).forEach { assertTrue(it, QruVideoCatalog.published(packageJson(it)).isEmpty()) }
    }

    @Test fun blocksMissingReviewerAndMalformedPackage() {
        assertTrue(QruVideoCatalog.published(packageJson(reviewedBy = "")).isEmpty())
        assertTrue(QruVideoCatalog.published("""{"schemaVersion":1,"items":[{}]}""").isEmpty())
        assertTrue(QruVideoCatalog.published("""{"schemaVersion":2,"items":[]}""").isEmpty())
    }

    @Test fun rejectsMissingOrInvalidMediaChecksum() {
        val valid = packageJson()
        assertTrue(QruVideoCatalog.published(valid.replace(
            "\"sha256\":\"${"a".repeat(64)}\",", ""
        )).isEmpty())
        assertTrue(QruVideoCatalog.published(valid.replace(
            "a".repeat(64), "not-a-valid-checksum"
        )).isEmpty())
    }

    @Test fun rejectsUntrustedYoutubeLinks() {
        val valid = packageJson()
        assertTrue(QruVideoCatalog.published(valid.replace(
            "https://www.youtube.com/watch?v=abc123",
            "https://www.youtube.com.evil.example/watch?v=abc123"
        )).isEmpty())
    }

    @Test fun emptyCatalogIsSafeUntilEditorialRelease() {
        assertTrue(QruVideoCatalog.published("""{"schemaVersion":1,"items":[]}""").isEmpty())
    }
}
