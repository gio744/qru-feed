package br.com.qru.transito.data.packageinstaller

import java.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

class Ed25519Verifier(private val publicKeyX509Base64: String) {
    // Android already registers a reduced provider named BC. Use our bundled instance.
    private val provider = BouncyCastleProvider()
    fun verify(payload: ByteArray, signatureBase64: String): Boolean = try {
        val keyBytes = Base64.getDecoder().decode(publicKeyX509Base64)
        val key = KeyFactory.getInstance("Ed25519", provider)
            .generatePublic(X509EncodedKeySpec(keyBytes))
        val signature = Signature.getInstance("Ed25519", provider)
        signature.initVerify(key)
        signature.update(payload)
        signature.verify(Base64.getDecoder().decode(signatureBase64))
    } catch (_: Exception) { false }
}
