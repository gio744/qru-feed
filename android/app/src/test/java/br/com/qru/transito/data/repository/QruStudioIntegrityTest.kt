package br.com.qru.transito.data.repository

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class QruStudioIntegrityTest {
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    @Test fun acceptsExactDownloadedBytes() {
        val bytes = "QRU Studio validated video bytes".toByteArray()
        val file = File.createTempFile("qru-studio-", ".mp4")
        try {
            file.writeBytes(bytes)
            assertTrue(QruStudioIntegrity.matches(file, sha256(bytes)))
            assertTrue(QruStudioIntegrity.matches(file, sha256(bytes).uppercase()))
        } finally {
            file.delete()
        }
    }

    @Test fun rejectsTamperedAndIncompleteDownloads() {
        val bytes = "approved original".toByteArray()
        val file = File.createTempFile("qru-studio-", ".mp4")
        try {
            file.writeBytes("modified original".toByteArray())
            assertFalse(QruStudioIntegrity.matches(file, sha256(bytes)))
            file.writeBytes(byteArrayOf())
            assertFalse(QruStudioIntegrity.matches(file, sha256(bytes)))
        } finally {
            file.delete()
        }
    }

    @Test fun rejectsMissingFilesAndMalformedChecksums() {
        val file = File.createTempFile("qru-studio-", ".mp4")
        file.delete()
        assertFalse(QruStudioIntegrity.matches(file, "a".repeat(64)))
        assertFalse(QruStudioIntegrity.matches(file, "not-a-sha256"))
    }
}
