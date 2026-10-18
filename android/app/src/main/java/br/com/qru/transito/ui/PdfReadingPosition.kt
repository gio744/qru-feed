package br.com.qru.transito.ui

/** Explicit source links take precedence over the last library reading position. */
internal fun initialPdfPage(requested: Int?, saved: Int, pageCount: Int): Int {
    require(pageCount > 0)
    return requested?.coerceIn(1, pageCount) ?: saved.takeIf { it in 1..pageCount } ?: 1
}
