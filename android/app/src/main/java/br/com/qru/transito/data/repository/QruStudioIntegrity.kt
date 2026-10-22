package br.com.qru.transito.data.repository

import java.io.File
import java.security.MessageDigest

/** Verify local media before allowing offline playback. */
object QruStudioIntegrity {
    fun matches(file: File, expectedSha256: String): Boolean = runCatching {
        if (!file.isFile || file.length() == 0L ||
            !Regex("[a-fA-F0-9]{64}").matches(expectedSha256)) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest()
        val expected = expectedSha256.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        MessageDigest.isEqual(actual, expected)
    }.getOrDefault(false)
}
