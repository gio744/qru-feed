package br.com.qru.transito.sync
import kotlinx.coroutines.CancellationException
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.qru.transito.QruApplication
import br.com.qru.transito.data.packageinstaller.*

class LegalUpdateWorker(ctx:Context,params:WorkerParameters):CoroutineWorker(ctx,params){
    override suspend fun doWork():Result{
        val app=applicationContext as QruApplication
        // The configured HTTPS source supplies signed candidates.
        // Never activate bytes merely because download succeeded.
        val stage=try{app.legalPackageSource.fetchCandidate()}catch(e:CancellationException){throw e}catch(_:Exception){return Result.retry()}
        if(stage==null)return Result.success()
        val result=try{app.safeLegalPackageInstaller.validateAndActivate(stage)}catch(e:CancellationException){throw e}catch(_:Exception){return Result.retry()}
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
