package br.com.qru.transito
import android.app.Application
import androidx.room.Room
import br.com.qru.transito.data.local.QruDatabase
import br.com.qru.transito.data.local.QruMigrations
import br.com.qru.transito.data.repository.LegalRepository
import br.com.qru.transito.data.repository.BundledCatalog
import br.com.qru.transito.domain.usecase.SearchLegalContent
import br.com.qru.transito.data.packageinstaller.*
import br.com.qru.transito.data.staging.PackageStagingStore
import br.com.qru.transito.network.*
import br.com.qru.transito.sync.*

class QruApplication:Application(){
    val database by lazy { Room.databaseBuilder(this,QruDatabase::class.java,"qru.db").addMigrations(QruMigrations.MIGRATION_1_2).build() }
    val bundledCatalog by lazy { BundledCatalog(this) }
    val legalRepository by lazy { LegalRepository(database, bundledCatalog) }
    val searchLegalContent by lazy { SearchLegalContent(legalRepository) }

    private val publicKey by lazy { getString(br.com.qru.transito.R.string.qru_legal_public_key_x509_b64) }
    val safeLegalPackageInstaller by lazy {
        SafeLegalPackageInstaller(database,Ed25519Verifier(publicKey),LegalPackageInstaller(database))
    }
    val legalPackageSource:LegalPackageSource by lazy {
        val api=NetworkFactory.legalApi(getString(br.com.qru.transito.R.string.qru_legal_api_base_url))
        HttpsLegalPackageSource(api,PackageStagingStore(this))
    }
    override fun onCreate(){
        super.onCreate()
        val apiUrl=getString(br.com.qru.transito.R.string.qru_legal_api_base_url)
        if(!apiUrl.contains(".invalid")) LegalSyncScheduler.schedule(this)
    }
}
