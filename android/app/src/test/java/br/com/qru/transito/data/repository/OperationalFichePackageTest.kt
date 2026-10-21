package br.com.qru.transito.data.repository

import org.junit.Assert.assertTrue
import org.junit.Test

class OperationalFichePackageTest {
    @Test fun officialHostCheckRejectsImpersonation() {
        assertTrue(OperationalFichePackage.isTrustedOfficialSource(
            "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm"
        ))
        assertTrue(!OperationalFichePackage.isTrustedOfficialSource(
            "https://www.planalto.gov.br.evil.example/documento"
        ))
        assertTrue(!OperationalFichePackage.isTrustedOfficialSource(
            "https://www.planalto.gov.br@evil.example/documento"
        ))
        assertTrue(!OperationalFichePackage.isTrustedOfficialSource(
            "http://www.planalto.gov.br/documento"
        ))
    }

    @Test fun unexpectedPackageMetadataFailsClosed() {
        assertTrue(OperationalFichePackage.approvedByCode(
            """{"schemaVersion":1,"items":[],"forceApproved":true}""",
            setOf("672-61")
        ).isEmpty())
    }

    @Test fun bundledPackageDigestMatchesVersionedAsset() {
        val json = "{\n  \"schemaVersion\": 1,\n  \"items\": []\n}\n"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(json.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertTrue(digest == "0e5e19436438b496cb29a0243523d31a7369bd1228b84cc6779d8ca88eaeec17")
    }

    @Test fun matchingDigestAllowsEmptyPackageWithoutPublishingContent() {
        val json = "{\n  \"schemaVersion\": 1,\n  \"items\": []\n}\n"
        assertTrue(OperationalFichePackage.approvedByCodeWithDigest(
            json,
            setOf("672-61"),
            "0e5e19436438b496cb29a0243523d31a7369bd1228b84cc6779d8ca88eaeec17"
        ).isEmpty())
    }

    @Test fun invalidDigestNeverActivatesEditorialPackage() {
        assertTrue(OperationalFichePackage.approvedByCodeWithDigest(
            """{"schemaVersion":1,"items":[]}""",
            setOf("672-61"),
            "incorrect"
        ).isEmpty())
        assertTrue(OperationalFichePackage.approvedByCodeWithDigest(
            """{"schemaVersion":1,"items":[]}""",
            setOf("672-61"),
            "0".repeat(64)
        ).isEmpty())
    }

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
