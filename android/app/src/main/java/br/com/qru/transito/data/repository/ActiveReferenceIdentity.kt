package br.com.qru.transito.data.repository

import br.com.qru.transito.domain.model.SearchResult

/**
 * Prevents stale search results from authorizing an operational fiche.
 * The identity must still be present in the active release, not merely in search history.
 */
internal object ActiveReferenceIdentity {
    fun matches(current: SearchResult, selected: SearchResult, activeVersion: String? = null): Boolean =
        current.stableKey == selected.stableKey &&
            current.code != null && current.code == selected.code &&
            current.releaseVersion == selected.releaseVersion &&
            (activeVersion == null || current.releaseVersion == activeVersion) &&
            current.reviewStatus == selected.reviewStatus &&
            current.source == selected.source &&
            current.article == selected.article
}
