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
    private fun samplePackage(status: String = "APPROVED", text: String = "Condição operacional revisada."): String {
        val item = mapOf(
            "code" to "672-61",
            "whenApplicable" to listOf(mapOf("text" to text, "evidence" to listOf(
                mapOf("url" to "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm", "locator" to "Art. 270")
            ))),
            "exceptions" to listOf(mapOf("text" to "Exceção revisada.", "evidence" to listOf(
                mapOf("url" to "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm", "locator" to "Art. 270")
            ))),
            "review" to mapOf("status" to status, "reviewer" to "Revisor de teste",
                "reviewedAt" to "2026-10-17", "unresolvedIssues" to emptyList<String>())
        )
        return com.google.gson.Gson().toJson(mapOf("schemaVersion" to 1, "items" to listOf(item)))
    }

    private fun sha256(json: String): String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(json.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    @Test fun packageNeverActivatesWithInactiveCodeOrWrongDigest() {
        val json = samplePackage()
        assertTrue(OperationalFichePackage.approvedByCodeWithDigest(json, setOf("672-62"), sha256(json))
            .isEmpty())
        assertTrue(OperationalFichePackage.approvedByCodeWithDigest(json, setOf("672-61"), "0".repeat(64))
            .isEmpty())
    }

    @Test fun pendingAndPlaceholderContentNeverActivateEvenWithValidDigest() {
        listOf(samplePackage(status = "PENDING"), samplePackage(text = "RASCUNHO para revisão."))
            .forEach { json ->
                assertTrue(OperationalFichePackage.approvedByCodeWithDigest(json, setOf("672-61"), sha256(json))
                    .isEmpty())
            }
    }

    @Test fun deadlineLegalBasisMustUseTrustedOfficialHost() {
        val evidence = mapOf("url" to "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm",
            "locator" to "Art. 270")
        val fakeEvidence = mapOf("url" to "https://example.com/lei",
            "locator" to "Art. 270")
        val statement = mapOf("text" to "Regra revisada.", "evidence" to listOf(evidence))
        val item = mapOf(
            "code" to "672-61",
            "whenApplicable" to listOf(statement),
            "exceptions" to listOf(statement),
            "measures" to listOf(mapOf(
                "kind" to "RETENTION",
                "rule" to statement,
                "conditions" to listOf(statement),
                "deadline" to mapOf("maximumDays" to 1,
                    "legalBasis" to mapOf("text" to "Prazo fundamentado.", "evidence" to listOf(fakeEvidence)))
            )),
            "review" to mapOf("status" to "APPROVED", "reviewer" to "Revisor de teste",
                "reviewedAt" to "2026-10-17", "unresolvedIssues" to emptyList<String>())
        )
        val json = com.google.gson.Gson().toJson(mapOf("schemaVersion" to 1, "items" to listOf(item)))
        assertTrue(OperationalFichePackage.approvedByCodeWithDigest(json, setOf("672-61"), sha256(json))
            .isEmpty())
    }

}
