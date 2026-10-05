package br.com.qru.transito.sync
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.qru.transito.QruApplication
import br.com.qru.transito.data.packageinstaller.*

class LegalUpdateWorker(ctx:Context,params:WorkerParameters):CoroutineWorker(ctx,params){
    override suspend fun doWork():Result{
        val app=applicationContext as QruApplication
        // Network source is injected in the next integration milestone.
        // Never activate bytes merely because download succeeded.
        val source=app.legalPackageSource
        val stage=try{source.fetchCandidate()}catch(_:Exception){return Result.retry()}
        if(stage==null)return Result.success()
        val result=app.safeLegalPackageInstaller.validateAndActivate(stage)
        if(result.activated){
            LastKnownGood(applicationContext).record(result.activeVersion ?: return Result.failure(),stage.fingerprint)
            return Result.success()
        }
        // Signature/fingerprint/structure failure is not retried blindly.
        return when(result.reason){
            "FINGERPRINT_MISMATCH","SIGNATURE_INVALID","INVALID_JSON","INVALID_STRUCTURE","INVALID_ITEM" -> Result.failure()
            else -> Result.retry()
        }
    }
}
