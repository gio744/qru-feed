package br.com.qru.transito.data.packageinstaller

import androidx.room.withTransaction
import br.com.qru.transito.data.local.*
import org.json.JSONObject
import java.security.MessageDigest

data class InstallResult(val installed: Boolean, val version: String?, val reason: String?)
class LegalPackageInstaller(private val db: QruDatabase) {
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
    private fun requiredString(item: JSONObject, name: String): String {
        val value = item.get(name)
        require(value is String && value.isNotBlank())
        return value
    }
    private fun optionalString(item: JSONObject, name: String): String? {
        if (!item.has(name)) return null
        val value = item.get(name)
        require(value is String)
        return value.ifBlank { null }
    }
    suspend fun install(raw: ByteArray, expected: String): InstallResult {
        val fingerprint = sha256(raw)
        if (!fingerprint.equals(expected, true)) return InstallResult(false, null, "FINGERPRINT_MISMATCH")
        val packageJson = try { JSONObject(String(raw, Charsets.UTF_8)) }
            catch (_: Exception) { return InstallResult(false, null, "INVALID_JSON") }
        val version: String
        val jurisdiction: String
        val array = packageJson.optJSONArray("items")
        try {
            require(packageJson.get("schema_version") == 1 && array != null)
            version = requiredString(packageJson, "content_version")
            jurisdiction = requiredString(packageJson, "jurisdiction")
        } catch (_: Exception) { return InstallResult(false, null, "INVALID_STRUCTURE") }
        val items = mutableListOf<LegalItemEntity>()
        val keys = mutableSetOf<String>()
        try {
            for (index in 0 until array!!.length()) {
                val item = array.getJSONObject(index)
                val key = requiredString(item, "stable_key")
                val title = requiredString(item, "title")
                require(keys.add(key))
                items += LegalItemEntity(key, title, optionalString(item, "search_text") ?: title,
                    optionalString(item, "article"), optionalString(item, "code"), jurisdiction, version, true)
            }
        } catch (_: Exception) { return InstallResult(false, null, "INVALID_ITEM") }
        db.withTransaction {
            db.legalDao().deactivateAll()
            db.legalReleaseDao().deactivateAll()
            db.legalDao().insertItems(items)
            db.legalReleaseDao().upsert(LegalReleaseEntity(version, fingerprint, jurisdiction, System.currentTimeMillis(), true))
        }
        return InstallResult(true, version, null)
    }
}
