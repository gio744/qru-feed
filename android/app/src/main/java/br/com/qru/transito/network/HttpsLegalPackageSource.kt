package br.com.qru.transito.network
import br.com.qru.transito.data.packageinstaller.StagedPackage
import br.com.qru.transito.data.staging.PackageStagingStore
import br.com.qru.transito.sync.LegalPackageSource

class HttpsLegalPackageSource(
    private val api:LegalReleaseApi,
    private val staging:PackageStagingStore
):LegalPackageSource{
    override suspend fun fetchCandidate():StagedPackage?{
        val response=api.currentPackage()
        if(response.code()==404)return null
        if(!response.isSuccessful)throw IllegalStateException("Legal package HTTP ${response.code()}")
        val body=response.body() ?: throw IllegalStateException("Empty legal package")
        val fp=response.headers()["X-QRU-Fingerprint"] ?: throw IllegalStateException("Missing fingerprint")
        val sig=response.headers()["X-QRU-Signature"] ?: throw IllegalStateException("Missing signature")
        val bytes=body.bytes()
        val stage=StagedPackage(bytes,fp,sig)
        staging.write(stage)
        return staging.read()
    }
}
