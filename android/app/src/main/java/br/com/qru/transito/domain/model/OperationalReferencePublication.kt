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

    /**
     * Editorial packages remain drafts until the application explicitly supplies
     * the active catalog. No implicit approval from bundled documents.
     */
    fun approvedByCode(
        references: List<OperationalReference>,
        activeCodes: Set<String>
    ): Map<String, OperationalReference> =
        activeCodes.mapNotNull { code ->
            findApproved(references, code, activeCodes)?.let { code to it }
        }.toMap()

    fun findApproved(
        references: List<OperationalReference>,
        requestedCode: String?,
        activeCodes: Set<String>
    ): OperationalReference? {
        if (requestedCode == null || requestedCode !in activeCodes) return null
        val matching = references.filter { it.code == requestedCode }
        // Ambiguous packages are never silently resolved by list order.
        if (matching.size != 1) return null
        return matching.single().takeIf { approvedForCode(it, requestedCode) }
    }

    fun approvedForCatalog(reference: OperationalReference?, requestedCode: String?, activeCodes: Set<String>): Boolean =
        requestedCode != null && requestedCode in activeCodes && approvedForCode(reference, requestedCode)
}
