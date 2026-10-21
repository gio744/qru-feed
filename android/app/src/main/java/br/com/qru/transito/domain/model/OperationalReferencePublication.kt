package br.com.qru.transito.domain.model

/**
 * Publication boundary for operational guidance. Search index availability and PDF access
 * must never be treated as evidence of editorial approval.
 */
object OperationalReferencePublication {
    fun approvedForDisplay(reference: OperationalReference?): Boolean =
        reference != null && reference.editorialIssues().isEmpty()

    fun approvedForCode(reference: OperationalReference?, requestedCode: String?): Boolean =
        requestedCode != null && reference?.code == requestedCode && approvedForDisplay(reference)
}
