package br.com.qru.transito.data.repository

import android.content.Context
import java.io.File

/**
 * Never expose downloaded media for playback before verifying its pinned digest.
 * Files remain in app-private storage. The caller may play only a non-null result.
 */
object QruStudioOffline {
    private val safeId = Regex("[a-z0-9][a-z0-9-]{2,79}")

    fun destination(context: Context, video: QruVideo): File {
        require(safeId.matches(video.id))
        val directory = File(context.filesDir, "studio")
        require(directory.isDirectory || directory.mkdirs())
        return File(directory, "${video.id}.mp4")
    }

    fun verifiedFile(context: Context, video: QruVideo): File? =
        runCatching { destination(context, video) }
            .getOrNull()
            ?.takeIf { QruStudioIntegrity.matches(it, video.sha256) }

    fun deleteUnverified(context: Context, video: QruVideo): Boolean {
        val file = runCatching { destination(context, video) }.getOrNull() ?: return false
        if (!file.exists() || QruStudioIntegrity.matches(file, video.sha256)) return false
        return file.delete()
    }
}
