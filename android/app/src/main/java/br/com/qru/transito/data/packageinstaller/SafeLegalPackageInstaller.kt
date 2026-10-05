package br.com.qru.transito.data.packageinstaller
import br.com.qru.transito.data.local.QruDatabase
import java.security.MessageDigest

data class SafeInstallResult(val activated:Boolean,val reason:String,val activeVersion:String?)

class SafeLegalPackageInstaller(
    private val db:QruDatabase,
    private val verifier:Ed25519Verifier,
    private val installer:LegalPackageInstaller
){
    private fun sha256(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}

    suspend fun validateAndActivate(stage:StagedPackage):SafeInstallResult{
        val current=db.legalReleaseDao().active()?.version
        if(!sha256(stage.payload).equals(stage.fingerprint,true))
            return SafeInstallResult(false,"FINGERPRINT_MISMATCH",current)
        if(!verifier.verify(stage.payload,stage.signature))
            return SafeInstallResult(false,"SIGNATURE_INVALID",current)

        // LegalPackageInstaller performs structure validation and atomic DB switch.
        val result=installer.install(stage.payload,stage.fingerprint)
        return if(result.installed)
            SafeInstallResult(true,"ACTIVATED",result.version)
        else SafeInstallResult(false,result.reason ?: "INSTALL_FAILED",current)
    }
}
