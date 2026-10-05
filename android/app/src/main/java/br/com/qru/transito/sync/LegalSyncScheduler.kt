package br.com.qru.transito.sync
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object LegalSyncScheduler{
    fun schedule(context:Context){
        val constraints=Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val work=PeriodicWorkRequestBuilder<LegalUpdateWorker>(12,TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "qru-legal-update",ExistingPeriodicWorkPolicy.KEEP,work
        )
    }
}
