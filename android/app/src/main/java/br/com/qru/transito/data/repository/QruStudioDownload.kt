package br.com.qru.transito.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Verified private download; never publishes a partial or mismatched file. */
object QruStudioDownload {
    suspend fun fetch(context: Context, video: QruVideo): Boolean = withContext(Dispatchers.IO) {
        if (!QruVideoCatalog.trustedMediaUrl(video.videoUrl)) return@withContext false
        val target = QruStudioOffline.destination(context, video)
        if (QruStudioIntegrity.matches(target, video.sha256)) return@withContext true
        val temp = File(target.parentFile, "${video.id}.partial")
        var connection: HttpURLConnection? = null
        try {
            if (temp.exists()) temp.delete()
            connection = (URL(video.videoUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "video/mp4")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext false
            if (connection.contentLengthLong > 200L * 1024 * 1024) return@withContext false
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(65536)
                    var bytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        bytes += count
                        if (bytes > 200L * 1024 * 1024) throw IllegalStateException("Video too large")
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (!QruStudioIntegrity.matches(temp, video.sha256)) return@withContext false
            if (target.exists() && !target.delete()) return@withContext false
            temp.renameTo(target) && QruStudioIntegrity.matches(target, video.sha256)
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
            if (temp.exists()) temp.delete()
        }
    }
}
