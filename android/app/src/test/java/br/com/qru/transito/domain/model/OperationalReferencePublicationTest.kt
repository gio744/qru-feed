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

    @Test fun referenceOutsideActiveCatalogCannotPublish() {
        assertFalse(OperationalReferencePublication.approvedForCatalog(approved(), "672-61", emptySet()))
        assertFalse(OperationalReferencePublication.approvedForCatalog(approved(), "672-61", setOf("659-92")))
        assertTrue(OperationalReferencePublication.approvedForCatalog(approved(), "672-61", setOf("672-61")))
    }

    @Test fun approvedLookupExcludesInactivePendingAndDuplicateFiches() {
        val valid = approved()
        val pending = approved().copy(code = "659-92", review = OperationalReview())
        val lookup = OperationalReferencePublication.approvedByCode(
            listOf(valid, pending), setOf("672-61", "659-92")
        )
        assertTrue(lookup.keys == setOf("672-61"))
        assertTrue(OperationalReferencePublication.approvedByCode(
            listOf(valid, valid), setOf("672-61")
        ).isEmpty())
    }

    @Test fun duplicatePackagesForSameCodeNeverPublishByAccident() {
        val item = approved()
        assertTrue(OperationalReferencePublication.findApproved(listOf(item), "672-61", setOf("672-61")) != null)
        assertTrue(OperationalReferencePublication.findApproved(listOf(item, item), "672-61", setOf("672-61")) == null)
    }

    @Test fun missingOrInactiveCodeNeverResolves() {
        assertTrue(OperationalReferencePublication.findApproved(listOf(approved()), "659-92", setOf("659-92")) == null)
        assertTrue(OperationalReferencePublication.findApproved(listOf(approved()), "672-61", emptySet()) == null)
    }

    @Test fun structurallyApprovedReferenceCanPassGateForOwnCode() {
        assertTrue(OperationalReferencePublication.approvedForCode(approved(), "672-61"))
    }
}
