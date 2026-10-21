package br.com.qru.transito.data.repository

import br.com.qru.transito.domain.model.SearchResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveReferenceIdentityTest {
    private val current = SearchResult(
        stableKey = "ctb-230-xviii", title = "Pneu",
        article = "230 XVIII", code = "672-61", jurisdiction = "BR",
        releaseVersion = "v2", source = "CTB", reviewStatus = "Revisado"
    )

    @Test fun matchingCurrentReferenceIsEligible() {
        assertTrue(ActiveReferenceIdentity.matches(current, current, "v2"))
    }

    @Test fun previousReleaseIsNeverEligible() {
        assertFalse(ActiveReferenceIdentity.matches(current, current.copy(releaseVersion = "v1"), "v2"))
        assertFalse(ActiveReferenceIdentity.matches(current, current, "v3"))
    }

    @Test fun alteredArticleOrOfficialSourceIsRejected() {
        assertFalse(ActiveReferenceIdentity.matches(current, current.copy(article = "230 XVII"), "v2"))
        assertFalse(ActiveReferenceIdentity.matches(current, current.copy(source = "Outra norma"), "v2"))
    }

    @Test fun alteredCodeOrReviewStateIsRejected() {
        assertFalse(ActiveReferenceIdentity.matches(current, current.copy(code = "999-99"), "v2"))
        assertFalse(ActiveReferenceIdentity.matches(current, current.copy(reviewStatus = "Pendente"), "v2"))
    }

    @Test fun changedStableKeyOrMissingCodeIsRejected() {
        assertFalse(ActiveReferenceIdentity.matches(current, current.copy(stableKey = "outra"), "v2"))
        assertFalse(ActiveReferenceIdentity.matches(current.copy(code = null), current.copy(code = null), "v2"))
    }
}
