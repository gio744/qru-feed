package br.com.qru.transito.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test

class OperationalFichePackageTest {
    @Test fun emptyVersionedPackageLoadsSafely() {
        assertTrue(OperationalFichePackage.approvedByCode(
            """{"schemaVersion":1,"items":[]}""", setOf("672-61")
        ).isEmpty())
    }

    @Test fun unsupportedVersionFailsClosed() {
        assertTrue(OperationalFichePackage.approvedByCode(
            """{"schemaVersion":2,"items":[]}""", setOf("672-61")
        ).isEmpty())
    }

    @Test fun malformedJsonFailsClosed() {
        assertTrue(OperationalFichePackage.approvedByCode("{", setOf("672-61")).isEmpty())
    }

    @Test fun incompleteItemFailsClosed() {
        assertTrue(OperationalFichePackage.approvedByCode(
            """{"schemaVersion":1,"items":[{"code":"672-61"}]}""", setOf("672-61")
        ).isEmpty())
    }

    @Test fun duplicateCodesFailClosed() {
        assertTrue(OperationalFichePackage.approvedByCode(
            """{"schemaVersion":1,"items":[{"code":"672-61"},{"code":"672-61"}]}""",
            setOf("672-61")
        ).isEmpty())
    }
}
