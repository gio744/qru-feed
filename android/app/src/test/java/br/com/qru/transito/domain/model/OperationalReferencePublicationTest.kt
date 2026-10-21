package br.com.qru.transito.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationalReferencePublicationTest {
    private val source = OperationalEvidence("https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm", "art. 230")
    private val statement = SourcedStatement("Exemplo fictício para teste estrutural", listOf(source))
    private fun approved() = OperationalReference(
        code = "672-61",
        whenApplicable = listOf(statement),
        exceptions = listOf(statement),
        review = OperationalReview(OperationalReviewStatus.APPROVED, "Revisor técnico", "2026-10-20")
    )

    @Test fun missingReferenceNeverPublishes() {
        assertFalse(OperationalReferencePublication.approvedForDisplay(null))
        assertFalse(OperationalReferencePublication.approvedForCode(null, "672-61"))
    }

    @Test fun draftNeverPublishesEvenWithCompleteText() {
        assertFalse(OperationalReferencePublication.approvedForDisplay(
            approved().copy(review = OperationalReview())
        ))
    }

    @Test fun wrongCodeCannotPublishAsAnotherInfraction() {
        assertFalse(OperationalReferencePublication.approvedForCode(approved(), "659-92"))
        assertFalse(OperationalReferencePublication.approvedForCode(approved(), null))
    }

    @Test fun structurallyApprovedReferenceCanPassGateForOwnCode() {
        assertTrue(OperationalReferencePublication.approvedForCode(approved(), "672-61"))
    }
}
