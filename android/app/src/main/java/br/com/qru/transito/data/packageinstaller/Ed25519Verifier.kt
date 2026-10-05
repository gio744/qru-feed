package br.com.qru.transito.data.packageinstaller
import android.util.Base64
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.Security
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

class Ed25519Verifier(private val publicKeyX509Base64:String){
    private fun provider():String{
        val name="BC"
        if(Security.getProvider(name)==null) Security.addProvider(BouncyCastleProvider())
        return name
    }
    fun verify(payload:ByteArray,signatureBase64:String):Boolean = try{
        val p=provider()
        val keyBytes=Base64.decode(publicKeyX509Base64,Base64.DEFAULT)
        val key=KeyFactory.getInstance("Ed25519",p).generatePublic(X509EncodedKeySpec(keyBytes))
        val sig=Signature.getInstance("Ed25519",p)
        sig.initVerify(key);sig.update(payload)
        sig.verify(Base64.decode(signatureBase64,Base64.DEFAULT))
    }catch(_:Exception){false}
}
