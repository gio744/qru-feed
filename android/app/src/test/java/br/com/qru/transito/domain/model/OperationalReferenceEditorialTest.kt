package br.com.qru.transito.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationalReferenceEditorialTest {
    private val evidence = OperationalEvidence("https://www.gov.br/transportes/", "art. 165")
    private val statement = SourcedStatement("Constatação documentada", listOf(evidence))

    private fun reference(
        status: OperationalReviewStatus = OperationalReviewStatus.APPROVED,
        reviewer: String = "Revisor técnico",
        reviewedAt: String = "2026-10-20",
        unresolved: List<String> = emptyList(),
        whenApplicable: List<SourcedStatement> = listOf(statement)
    ) = OperationalReference(
        code = "757-90",
        whenApplicable = whenApplicable,
        review = OperationalReview(status, reviewer, reviewedAt, unresolved)
    )

    @Test fun completeReviewedReferenceHasNoEditorialIssues() {
        assertTrue(reference().editorialIssues().isEmpty())
    }

    @Test fun pendingReviewIsNeverImplicitlyApproved() {
        assertTrue(reference(status = OperationalReviewStatus.PENDING)
            .editorialIssues().contains("review_pending"))
    }

    @Test fun missingReviewerAndDateBlockEditorialReadiness() {
        val issues = reference(reviewer = "", reviewedAt = "").editorialIssues()
        assertTrue(issues.contains("missing_reviewer"))
        assertTrue(issues.contains("missing_review_date"))
    }

    @Test fun unresolvedIssuesBlockEditorialReadiness() {
        assertTrue(reference(unresolved = listOf("Conferir norma vigente"))
            .editorialIssues().contains("unresolved_issues"))
    }

    @Test fun missingApplicabilityBlocksEditorialReadiness() {
        assertTrue(reference(whenApplicable = emptyList())
            .editorialIssues().contains("missing_applicability"))
    }

    @Test fun missingEvidenceBlocksEditorialReadiness() {
        val ref = reference(whenApplicable = listOf(SourcedStatement("Texto sem fonte", emptyList())))
        assertTrue(ref.editorialIssues().contains("incomplete_evidence"))
    }

    @Test fun invalidCodeIsRejected() {
        assertTrue(OperationalReference(code = "165-A", whenApplicable = listOf(statement),
            review = OperationalReview(OperationalReviewStatus.APPROVED, "Revisor", "2026-10-20"))
            .editorialIssues().contains("invalid_code"))
    }

    @Test fun measureRequiresConditionsAndTraceableDeadline() {
        val ref = reference().copy(measures = listOf(AdministrativeMeasure(
            kind = AdministrativeMeasureKind.REMOVAL,
            rule = statement,
            deadline = OperationalDeadline(0, statement)
        )))
        val issues = ref.editorialIssues()
        assertTrue(issues.contains("missing_measure_conditions"))
        assertTrue(issues.contains("invalid_deadline"))
    }

    @Test fun completeMeasureCanPassEditorialChecks() {
        val ref = reference().copy(measures = listOf(AdministrativeMeasure(
            kind = AdministrativeMeasureKind.RETENTION,
            rule = statement,
            conditions = listOf(statement)
        )))
        assertFalse(ref.editorialIssues().isNotEmpty())
    }
}
