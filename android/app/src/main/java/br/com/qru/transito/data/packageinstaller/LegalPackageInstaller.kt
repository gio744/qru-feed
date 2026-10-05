package br.com.qru.transito.data.packageinstaller
import androidx.room.withTransaction
import br.com.qru.transito.data.local.*
import org.json.JSONObject
import java.security.MessageDigest
data class InstallResult(val installed:Boolean,val version:String?,val reason:String?)
class LegalPackageInstaller(private val db:QruDatabase){
 private fun sha256(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}
 suspend fun install(raw:ByteArray,expected:String):InstallResult{
  val fp=sha256(raw); if(!fp.equals(expected,true)) return InstallResult(false,null,"FINGERPRINT_MISMATCH")
  val o=try{JSONObject(String(raw,Charsets.UTF_8))}catch(e:Exception){return InstallResult(false,null,"INVALID_JSON")}
  val ver=o.optString("content_version"); val jur=o.optString("jurisdiction"); val a=o.optJSONArray("items")
  if(o.optInt("schema_version",-1)!=1||ver.isBlank()||jur.isBlank()||a==null)return InstallResult(false,null,"INVALID_STRUCTURE")
  val items=mutableListOf<LegalItemEntity>()
  try{for(i in 0 until a.length()){val x=a.getJSONObject(i);items+=LegalItemEntity(x.getString("stable_key"),x.getString("title"),x.optString("search_text",x.getString("title")),x.optString("article").ifBlank{null},x.optString("code").ifBlank{null},jur,ver,true)}}catch(e:Exception){return InstallResult(false,null,"INVALID_ITEM")}
  db.withTransaction{db.legalDao().deactivateAll();db.legalReleaseDao().deactivateAll();db.legalDao().insertItems(items);db.legalReleaseDao().upsert(LegalReleaseEntity(ver,fp,jur,System.currentTimeMillis(),true))}
  return InstallResult(true,ver,null)
 }
}