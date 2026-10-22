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

    @Test fun officialHostCheckRejectsAlternatePortsFragmentsAndUntrustedHosts() {
        val rejected = listOf(
            "https://www.planalto.gov.br:443/ctb",
            "https://www.planalto.gov.br/ctb#alterado",
            "https://planalto.gov.br/ctb",
            "https://www.gov.br.evil.example/norma",
            "https://www.gov.br@evil.example/norma",
            "https://www.camara.leg.br:8443/norma",
            "javascript:alert(1)",
            "https://www.gov.br\\\\evil.example/norma"
        )
        rejected.forEach { url ->
            assertTrue("Untrusted source accepted: $url",
                !OperationalFichePackage.isTrustedOfficialSource(url))
        }
    }

    @Test fun tamperedDigestCannotActivateEvenWellFormedPackage() {
        val original = """{"schemaVersion":1,"items":[]}"""
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(original.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val modified = original.replace("[]", "[{}]")
        assertTrue(OperationalFichePackage.approvedByCodeWithDigest(
            modified, setOf("672-61"), digest
        ).isEmpty())
    }

    @Test fun deadlineEvidenceMustUseTrustedOfficialHost() {
        val official = "https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm"
        val fake = "https://www.planalto.gov.br.evil.example/ctb"
        fun evidence(url: String) = """{"url":"$url","locator":"Art. 270"}"""
        fun statement(url: String) = """{"text":"Texto de teste","evidence":[${evidence(url)}]}"""
        val json = """{
            "schemaVersion":1,
            "items":[{
                "code":"672-61",
                "whenApplicable":[${statement(official)}],
                "exceptions":[${statement(official)}],
                "measures":[{
                    "kind":"RETENTION",
                    "rule":${statement(official)},
                    "conditions":[${statement(official)}],
                    "deadline":{
                        "maximumDays":1,
                        "legalBasis":${statement(fake)}
                    }
                }],
                "review":{
                    "status":"APPROVED",
                    "reviewer":"Revisor de teste",
                    "reviewedAt":"2026-01-01",
                    "unresolvedIssues":[]
                }
            }]
        }""".trimIndent()
        assertTrue(OperationalFichePackage.approvedByCode(json,setOf("672-61")).isEmpty())
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
