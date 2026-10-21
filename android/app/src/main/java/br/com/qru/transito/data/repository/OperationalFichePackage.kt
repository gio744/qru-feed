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

    fun approvedByCode(json: String, activeCodes: Set<String>): Map<String, OperationalReference> =
        runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            require(root.get("schemaVersion").asInt == 1) { "Unsupported editorial package version" }
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
