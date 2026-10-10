package br.com.qru.transito.domain.usecase

import java.text.Normalizer
import java.util.Locale
import br.com.qru.transito.domain.model.SearchResult

data class IndexedReference(val result: SearchResult, val terms: String)
object CatalogSearch {
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    private val stopWords = setOf("art", "artigo", "artigos", "o", "a", "os", "as", "de", "do", "da", "dos", "das", "e", "com", "sem", "um", "uma", "que", "para", "por", "no", "na", "ao", "condutor", "veiculo", "dirigir", "dirigindo")
    fun search(entries: List<IndexedReference>, query: String): List<SearchResult> {
        val normalized = normalize(query)
        val tokens = normalized.split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() && it !in stopWords }.distinct()
        if (tokens.isEmpty()) return emptyList()
        val compact = tokens.joinToString("")
        val articleQuery = normalized.replace(Regex("^(?:artigo|art)\\.?\\s*"), "").trim()
        val numericQuery = articleQuery.matches(Regex("[0-9]+(?:\\s*-\\s*[a-z])?"))
        val numericKey = articleQuery.replace(Regex("[^a-z0-9]"), "")
        return entries.mapNotNull { entry ->
            val result = entry.result
            val code = normalize(result.code.orEmpty()).replace(Regex("[^a-z0-9]"), "")
            val title = normalize(result.title)
            val text = normalize("${result.title} ${result.article.orEmpty()} ${result.code.orEmpty()} ${entry.terms}")
            val matches = tokens.count { text.contains(it) }
            val exactCode = code.isNotBlank() && code == compact
            val exactArticle = numericQuery && normalize(result.article.orEmpty()).substringBefore(',')
                .replace(Regex("[^a-z0-9]"), "") == numericKey
            if (numericQuery && !exactCode && !exactArticle) null
            else if (!exactCode && matches < (tokens.size + 1) / 2) null
            else (if (exactCode) 1000 else if (exactArticle) 300 else 0) +
                matches * 20 + tokens.count { title.contains(it) } * 5 to result
        }.sortedWith(compareByDescending<Pair<Int, SearchResult>> { it.first }.thenBy { it.second.title })
            .take(50).map { it.second }
    }
}
