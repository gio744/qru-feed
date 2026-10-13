package br.com.qru.transito

import br.com.qru.transito.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test

class HabilitationGuideTest {
    @Test fun missingFactsDoNotProduceAnOffenceReference() {
        val review = HabilitationGuide.review(GuideFacts())
        assertFalse(review.dateValid)
        assertTrue(review.referenceCodes.isEmpty())
    }
    @Test fun impossibleCalendarDateRemainsUnconfirmed() {
        assertFalse(HabilitationGuide.review(GuideFacts(date="2026-02-30")).dateValid)
    }
    @Test fun completedSuspensionAndPendingRecyclingRemainDistinctFromOngoingSuspension() {
        val review = HabilitationGuide.review(GuideFacts("2026-10-13",
            SuspensionState.COMPLETED, RecyclingState.PENDING, ExpiryState.CURRENT))
        assertTrue(review.dateValid)
        assertTrue(review.referenceCodes.contains("774-92"))
        assertFalse(review.referenceCodes.contains("502-92"))
    }
    @Test fun combinedConditionsKeepAllReferencesWithoutDuplicates() {
        val review = HabilitationGuide.review(GuideFacts("2026-10-13",
            SuspensionState.ONGOING, RecyclingState.PENDING, ExpiryState.OVER_THIRTY))
        assertEquals(listOf("502-92","774-92","504-50"), review.referenceCodes)
        assertTrue(review.checks.any { it.contains("279/2026") })
    }
}
