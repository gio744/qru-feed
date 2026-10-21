package br.com.qru.transito.domain.model

import org.junit.Assert.*
import org.junit.Test

class OperationalReferenceTest {
    private val evidence = OperationalEvidence("https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm", "art. 270, §2º")
    private val statement = SourcedStatement("Condição fundamentada para revisão.", listOf(evidence))
    private fun complete() = OperationalReference(
        code = "672-61",
        whenApplicable = listOf(statement),
        exceptions = listOf(statement),
        review = OperationalReview(OperationalReviewStatus.APPROVED, "Revisor de teste", "2026-10-17")
    )

    @Test fun fillingContentDoesNotApproveIt() {
        val reference = complete().copy(review = OperationalReview())
        assertTrue("review_pending" in reference.editorialIssues())
        assertTrue("missing_reviewer" in reference.editorialIssues())
    }
    @Test fun explicitApprovalDoesNotHideOpenIssues() {
        val reference = complete().let { it.copy(review = it.review.copy(unresolvedIssues = listOf("Conferir vigência"))) }
        assertTrue("unresolved_issues" in reference.editorialIssues())
    }
    @Test fun aMeasureRequiresItsOwnConditions() {
        val reference = complete().copy(measures = listOf(AdministrativeMeasure(AdministrativeMeasureKind.RETENTION, statement)))
        assertTrue("missing_measure_conditions" in reference.editorialIssues())
    }
    @Test fun deadlineRequiresPositiveDaysAndEvidence() {
        val deadline = OperationalDeadline(30, statement.copy(evidence = emptyList()))
        val measure = AdministrativeMeasure(AdministrativeMeasureKind.RETENTION, statement, listOf(statement), deadline)
        assertTrue("invalid_deadline" in complete().copy(measures = listOf(measure)).editorialIssues())
        assertFalse(OperationalDeadline(0, statement).isComplete())
    }
    @Test fun noMeasureCannotCarryARegularizationDeadline() {
        val measure = AdministrativeMeasure(AdministrativeMeasureKind.NONE, statement,
            deadline = OperationalDeadline(15, statement))
        assertTrue("deadline_without_measure" in complete().copy(measures = listOf(measure)).editorialIssues())
    }
    @Test fun everyExceptionNeedsTraceableEvidence() {
        val reference = complete().copy(exceptions = listOf(statement.copy(evidence = emptyList())))
        assertTrue("incomplete_evidence" in reference.editorialIssues())
    }
    @Test fun rejectsMalformedOrCredentialBearingEvidence() {
        listOf("http://example.org", "not a URL", "https://user@example.org").forEach {
            assertFalse(OperationalEvidence(it, "artigo").isComplete())
        }
        assertFalse(evidence.copy(locator = "").isComplete())
    }
    @Test fun structurallyCompleteReviewIsOnlyStructurallyComplete() {
        assertTrue(complete().editorialIssues().isEmpty())
    }
}
