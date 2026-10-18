package br.com.qru.transito.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfReadingPositionTest {
    @Test fun libraryResumesLastReadPage() {
        assertEquals(700, initialPdfPage(null, 700, 830))
    }
    @Test fun referenceAlwaysOpensItsOwnPage() {
        assertEquals(599, initialPdfPage(599, 700, 830))
        assertEquals(1, initialPdfPage(1, 700, 830))
    }
    @Test fun corruptStoredPageStartsAtBeginning() {
        listOf(-1, 0, 831, Int.MAX_VALUE).forEach {
            assertEquals(1, initialPdfPage(null, it, 830))
        }
    }
    @Test fun explicitPageKeepsExistingBoundsBehavior() {
        assertEquals(1, initialPdfPage(-1, 20, 830))
        assertEquals(830, initialPdfPage(999, 20, 830))
    }
    @Test fun onePageDocumentAlwaysStartsAtOne() {
        assertEquals(1, initialPdfPage(null, 1, 1))
        assertEquals(1, initialPdfPage(null, 2, 1))
    }
    @Test(expected = IllegalArgumentException::class)
    fun documentWithoutPagesCannotBeOpened() {
        initialPdfPage(null, 1, 0)
    }
}
