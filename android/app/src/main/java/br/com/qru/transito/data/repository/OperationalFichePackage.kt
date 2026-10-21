package br.com.qru.transito.data.repository

import br.com.qru.transito.domain.model.OperationalReference
import br.com.qru.transito.domain.model.OperationalReferencePublication
import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * Strict, versioned asset reader. Invalid packages fail closed rather than
 * publishing incomplete or unreviewed traffic-enforcement guidance.
 */
object OperationalFichePackage {
    internal fun isTrustedOfficialSource(url: String): Boolean = runCatching {
        val uri = java.net.URI(url)
        val host = uri.host?.lowercase(java.util.Locale.ROOT)
        uri.scheme == "https" && uri.rawUserInfo == null &&
            uri.port == -1 && uri.rawFragment == null &&
            host in setOf("www.gov.br", "www.planalto.gov.br", "www.camara.leg.br")
    }.getOrDefault(false)

    /**
     * Explicit package activation requires a known expected SHA-256 digest.
     * This prevents accidental activation of modified asset bytes.
     */
    fun approvedByCodeWithDigest(
        json: String,
        activeCodes: Set<String>,
        expectedSha256: String
    ): Map<String, OperationalReference> {
        if (!Regex("[a-fA-F0-9]{64}").matches(expectedSha256)) return emptyMap()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(json.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        if (!digest.equals(expectedSha256, ignoreCase = true)) return emptyMap()
        return approvedByCode(json, activeCodes)
    }

    fun approvedByCode(json: String, activeCodes: Set<String>): Map<String, OperationalReference> =
        runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            require(root.get("schemaVersion").asInt == 1) { "Unsupported editorial package version" }
            require(root.keySet() == setOf("schemaVersion", "items")) {
                "Unexpected editorial package fields"
            }
            val items = root.getAsJsonArray("items")
            require(items.size() <= 10000) { "Package exceeds item limit" }
            val gson = Gson()
            val references = items.map { element ->
                require(element.isJsonObject)
                gson.fromJson(element, OperationalReference::class.java)
                    ?: error("Null operational reference")
            }
            // An invalid entry invalidates the whole package, avoiding partial publication.
            require(references.all { reference ->
                reference.editorialIssues().isEmpty() &&
                    (reference.whenApplicable + reference.exceptions + reference.observations +
                        reference.measures.flatMap { listOf(it.rule) + it.conditions })
                        .flatMap { it.evidence }
                        .all { evidence -> isTrustedOfficialSource(evidence.url) }
            }) { "Unapproved editorial content or unsupported source host" }
            require(references.map { it.code }.distinct().size == references.size) { "Duplicate code" }
            OperationalReferencePublication.approvedByCode(references, activeCodes)
        }.getOrDefault(emptyMap())
}
