package br.com.qru.transito.sync
import br.com.qru.transito.data.packageinstaller.StagedPackage
interface LegalPackageSource{
    suspend fun fetchCandidate():StagedPackage?
}
