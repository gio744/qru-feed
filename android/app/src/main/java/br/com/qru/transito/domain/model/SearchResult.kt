package br.com.qru.transito.domain.model

data class SearchResult(
    val stableKey: String, val title: String, val article: String?, val code: String?,
    val jurisdiction: String, val releaseVersion: String,
    val source: String? = null, val reviewStatus: String = "Validação final pendente",
    val appliesFrom: String = "", val appliesUntil: String = ""
)
