package br.com.qru.transito.data.repository

import android.content.Context
import org.json.JSONObject
import br.com.qru.transito.domain.model.SearchResult
import br.com.qru.transito.domain.usecase.CatalogSearch
import br.com.qru.transito.domain.usecase.IndexedReference

class BundledCatalog(private val context: Context) {
    private val entries: List<IndexedReference> by lazy {
        val root = context.assets.open("initial-index.json").bufferedReader().use { JSONObject(it.readText()) }
        val items = root.getJSONArray("items")
        List(items.length()) { index ->
            val item = items.getJSONObject(index)
            IndexedReference(SearchResult(item.getString("stable_key"), item.getString("title"),
                item.getString("article"), item.getString("code"), "BR", root.getString("content_version"),
                item.optString("source").ifBlank { null }, item.getString("review_status"),
                item.optString("applies_from"), item.optString("applies_until")), item.optString("search_text"))
        }
    }
    fun search(query: String) = CatalogSearch.search(entries, query)
    fun all(): List<SearchResult> = entries.map { it.result }
}
