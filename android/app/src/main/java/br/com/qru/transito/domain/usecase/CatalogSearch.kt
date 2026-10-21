package br.com.qru.transito.domain.usecase

import java.text.Normalizer
import java.util.Locale
import br.com.qru.transito.domain.model.SearchResult

data class IndexedReference(val result: SearchResult, val terms: String)
object CatalogSearch {
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    private val stopWords = setOf("art", "artigo", "artigos", "o", "a", "os", "as", "de", "do", "da", "dos", "das", "e", "com", "sem", "um", "uma", "que", "para", "por", "no", "na", "ao", "condutor", "veiculo", "dirigir", "dirigindo")
    // Keep inciso order legal/numeric, rather than alphabetical title order.
    private fun romanValue(value: String): Int {
        val values = mapOf('i' to 1, 'v' to 5, 'x' to 10, 'l' to 50, 'c' to 100, 'd' to 500, 'm' to 1000)
        var total = 0
        var previous = 0
        for (letter in value.reversed()) {
            val current = values[letter] ?: return Int.MAX_VALUE
            total += if (current < previous) -current else current
            previous = current
        }
        return total
    }
    private fun articleOrder(article: String?): Pair<Int, Int> {
        val normalized = normalize(article.orEmpty())
        val match = Regex("""^\\s*(\\d+)(?:\\s*[,–-]\\s*([ivxlcdm]+))?""").find(normalized)
            ?: return Int.MAX_VALUE to Int.MAX_VALUE
        val number = match.groupValues[1].toIntOrNull() ?: Int.MAX_VALUE
        val inciso = match.groupValues[2].takeIf { it.isNotEmpty() }?.let(::romanValue) ?: 0
        return number to inciso
    }
    fun search(entries: List<IndexedReference>, query: String): List<SearchResult> {
        val normalized = normalize(query)
        val tokens = normalized.split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() && it !in stopWords }.distinct()
        if (tokens.isEmpty()) return emptyList()
        val compact = tokens.joinToString("")
        val genericTyreQuery = tokens == listOf("pneu")
        val articleQuery = normalized.replace(Regex("^(?:artigo|art)\\.?\\s*"), "").trim()
        val articleParts = articleQuery.split(Regex("[^a-z0-9-]+")).filter { it.isNotBlank() }
        val incisoQuery = articleParts.size == 2 && articleParts[0].matches(Regex("[0-9]+(?:-[a-z])?")) && articleParts[1].matches(Regex("[ivxlcdm]+"))
        val exactIncisoKey = articleParts.joinToString(",")
        val numericQuery = articleQuery.matches(Regex("[0-9]+(?:\\s*-\\s*[a-z])?"))
        val numericKey = articleQuery.replace(Regex("[^a-z0-9]"), "")
        return entries.mapNotNull { entry ->
            val result = entry.result
            val code = normalize(result.code.orEmpty()).replace(Regex("[^a-z0-9]"), "")
            val title = normalize(result.title)
            val text = normalize("${result.title} ${result.article.orEmpty()} ${result.code.orEmpty()} ${entry.terms}")
            val matches = tokens.count { text.contains(it) }
            val exactCode = code.isNotBlank() && code == compact
            val resultParts = normalize(result.article.orEmpty()).split(Regex("[^a-z0-9-]+")).filter { it.isNotBlank() }
            val exactInciso = incisoQuery && resultParts.joinToString(",") == exactIncisoKey
            val exactArticle = numericQuery && normalize(result.article.orEmpty()).substringBefore(',')
                .replace(Regex("[^a-z0-9]"), "") == numericKey
            if (numericQuery && !exactCode && !exactArticle) null
            else if (!exactCode && matches < (tokens.size + 1) / 2) null
            else (if (exactCode) 1000 else if (exactInciso) 800 else if (exactArticle) 300 else 0) +
                (if (genericTyreQuery && code == "67261") 150 else 0) +
                matches * 20 + tokens.count { title.contains(it) } * 5 to result
        }.sortedWith(compareByDescending<Pair<Int, SearchResult>> { it.first }.thenBy { articleOrder(it.second.article).first }.thenBy { articleOrder(it.second.article).second }.thenBy { it.second.title })
            .take(50).map { it.second }
    }
}
