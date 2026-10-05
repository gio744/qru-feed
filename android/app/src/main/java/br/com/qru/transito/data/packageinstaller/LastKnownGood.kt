package br.com.qru.transito.data.packageinstaller
import android.content.Context

class LastKnownGood(context:Context){
    private val p=context.getSharedPreferences("qru_legal_state",Context.MODE_PRIVATE)
    fun record(version:String,fingerprint:String){
        p.edit().putString("lkg_version",version).putString("lkg_fingerprint",fingerprint).apply()
    }
    fun version():String?=p.getString("lkg_version",null)
    fun fingerprint():String?=p.getString("lkg_fingerprint",null)
}
