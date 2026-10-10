package br.com.qru.transito

import br.com.qru.transito.data.packageinstaller.Ed25519Verifier
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.*
import org.junit.Test

class Ed25519VerifierTest {
    @Test fun acceptsSignedBytesAndRejectsTamperedBytes() {
        val provider = BouncyCastleProvider()
        val key = KeyPairGenerator.getInstance("Ed25519", provider).generateKeyPair()
        val payload = "Fiscalização QRU".toByteArray(Charsets.UTF_8)
        val signer = Signature.getInstance("Ed25519", provider)
        signer.initSign(key.private)
        signer.update(payload)
        val signature = Base64.getEncoder().encodeToString(signer.sign())
        val verifier = Ed25519Verifier(Base64.getEncoder().encodeToString(key.public.encoded))
        assertTrue(verifier.verify(payload, signature))
        assertFalse(verifier.verify("alterado".toByteArray(), signature))
        assertFalse(verifier.verify(payload, "invalid"))
    }
    @Test fun rejectsInvalidPublicKey() {
        assertFalse(Ed25519Verifier("invalid").verify(byteArrayOf(1), "invalid"))
    }
}
