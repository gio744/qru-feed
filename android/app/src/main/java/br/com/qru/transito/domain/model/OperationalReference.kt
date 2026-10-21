package br.com.qru.transito.domain.model

/**
 * Editorial working model. Never derives approval from the presence of content.
 * Importing, signing and activating packages remain separate publication steps.
 */
data class OperationalReference(
    val code: String,
    val whenApplicable: List<SourcedStatement> = emptyList(),
    val exceptions: List<SourcedStatement> = emptyList(),
    val observations: List<SourcedStatement> = emptyList(),
    val measures: List<AdministrativeMeasure> = emptyList(),
    val review: OperationalReview = OperationalReview()
) {
    fun editorialIssues(): List<String> = buildList {
        if (!Regex("[0-9]{3}-[0-9]{2}").matches(code)) add("invalid_code")
        if (whenApplicable.isEmpty()) add("missing_applicability")
        if (review.status != OperationalReviewStatus.APPROVED) add("review_pending")
        if (review.reviewer.isBlank()) add("missing_reviewer")
        if (review.reviewedAt.isBlank()) add("missing_review_date")
        else {
            val date = try { java.time.LocalDate.parse(review.reviewedAt) }
                catch (_: Exception) { null }
            if (date == null || date.isAfter(java.time.LocalDate.now()))
                add("invalid_review_date")
        }
        if (review.unresolvedIssues.isNotEmpty()) add("unresolved_issues")
        val statements = whenApplicable + exceptions + observations +
            measures.flatMap { listOf(it.rule) + it.conditions }
        if (statements.any { it.text.isBlank() || it.evidence.isEmpty() ||
                    it.evidence.any { source -> !source.isComplete() } }) add("incomplete_evidence")
        if (measures.any { it.kind != AdministrativeMeasureKind.NONE && it.conditions.isEmpty() })
            add("missing_measure_conditions")
        if (measures.any { it.deadline != null && !it.deadline.isComplete() })
            add("invalid_deadline")
        if (measures.any { it.systemDependency != null && it.systemDependency.isBlank() })
            add("empty_system_dependency")
        if (measures.any { it.kind == AdministrativeMeasureKind.NONE && it.deadline != null })
            add("deadline_without_measure")
    }
}

data class SourcedStatement(val text: String, val evidence: List<OperationalEvidence>)
data class OperationalEvidence(val url: String, val locator: String) {
    fun isComplete(): Boolean = try {
        val uri = java.net.URI(url)
        uri.scheme == "https" && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && locator.isNotBlank()
    } catch (_: Exception) { false }
}
enum class AdministrativeMeasureKind { RETENTION, REMOVAL, NONE }
data class AdministrativeMeasure(
    val kind: AdministrativeMeasureKind,
    val rule: SourcedStatement,
    val conditions: List<SourcedStatement> = emptyList(),
    val deadline: OperationalDeadline? = null,
    val systemDependency: String? = null
)
data class OperationalDeadline(val maximumDays: Int, val legalBasis: SourcedStatement) {
    fun isComplete() = maximumDays > 0 && legalBasis.text.isNotBlank() &&
        legalBasis.evidence.isNotEmpty() && legalBasis.evidence.all { it.isComplete() }
}
enum class OperationalReviewStatus { PENDING, APPROVED }
data class OperationalReview(
    val status: OperationalReviewStatus = OperationalReviewStatus.PENDING,
    val reviewer: String = "",
    val reviewedAt: String = "",
    val unresolvedIssues: List<String> = emptyList()
)
