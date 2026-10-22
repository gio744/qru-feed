package br.com.qru.transito.data.repository

import com.google.gson.JsonParser
import java.net.URI
import java.util.Locale

/**
 * Editorially controlled catalog for QRU Studio.
 * A video can only be surfaced after review, with an owned HTTPS download URL.
 * YouTube URLs are discovery links, never download sources.
 */
data class QruVideo(
    val id: String,
    val title: String,
    val description: String,
    val legalReference: String,
    val reviewedBy: String,
    val reviewedAt: String,
    val videoUrl: String,
    val youtubeUrl: String? = null,
    val durationSeconds: Int
)

object QruVideoCatalog {
    private val idPattern = Regex("[a-z0-9][a-z0-9-]{2,79}")
    private val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")

    internal fun trustedMediaUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        val host = uri.host?.lowercase(Locale.ROOT)
        uri.scheme == "https" && uri.rawUserInfo == null &&
            uri.port == -1 && uri.rawFragment == null &&
            host != null && (host == "qru.app.br" || host.endsWith(".qru.app.br")) &&
            !uri.rawPath.isNullOrBlank()
    }.getOrDefault(false)

    internal fun trustedYoutubeUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        val host = uri.host?.lowercase(Locale.ROOT)
        uri.scheme == "https" && uri.rawUserInfo == null &&
            uri.port == -1 && uri.rawFragment == null &&
            host in setOf("www.youtube.com", "youtube.com", "youtu.be")
    }.getOrDefault(false)

    fun published(json: String): List<QruVideo> = runCatching {
        val root = JsonParser.parseString(json).asJsonObject
        require(root.keySet() == setOf("schemaVersion", "items"))
        require(root.get("schemaVersion").asInt == 1)
        val entries = root.getAsJsonArray("items")
        require(entries.size() <= 1000)
        val ids = mutableSetOf<String>()
        entries.map { entry ->
            val item = entry.asJsonObject
            require(item.keySet() == setOf(
                "id", "title", "description", "legalReference",
                "reviewedBy", "reviewedAt", "videoUrl", "durationSeconds"
            ) || item.keySet() == setOf(
                "id", "title", "description", "legalReference",
                "reviewedBy", "reviewedAt", "videoUrl", "durationSeconds", "youtubeUrl"
            ))
            fun required(key: String): String = item.get(key).asString.trim()
                .also { require(it.isNotEmpty() && it.length <= 500) }
            val id = required("id")
            require(idPattern.matches(id) && ids.add(id))
            val date = required("reviewedAt")
            require(datePattern.matches(date))
            java.time.LocalDate.parse(date)
            val media = required("videoUrl")
            require(trustedMediaUrl(media))
            val youtube = item.get("youtubeUrl")?.asString
            require(youtube == null || trustedYoutubeUrl(youtube))
            val duration = item.get("durationSeconds").asInt
            require(duration in 1..7200)
            QruVideo(id, required("title"), required("description"),
                required("legalReference"), required("reviewedBy"), date,
                media, youtube, duration)
        }
    }.getOrDefault(emptyList())
}
