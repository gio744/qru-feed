package br.com.qru.transito.data.local
import androidx.room.*
@Entity(tableName="legal_items")
data class LegalItemEntity(@PrimaryKey val stableKey:String,val title:String,val searchText:String,val article:String?,val code:String?,val jurisdiction:String,val releaseVersion:String,val active:Boolean=true)
@Entity(tableName="legal_releases_local",indices=[Index(value=["active"])])
data class LegalReleaseEntity(@PrimaryKey val version:String,val fingerprint:String,val jurisdiction:String,val installedAt:Long,val active:Boolean)
@Dao interface LegalDao{
 @Query("""SELECT * FROM legal_items WHERE active=1 AND (lower(title) LIKE '%'||lower(:q)||'%' OR lower(searchText) LIKE '%'||lower(:q)||'%' OR lower(COALESCE(article,'')) LIKE '%'||lower(:q)||'%' OR lower(COALESCE(code,'')) LIKE '%'||lower(:q)||'%') ORDER BY title LIMIT 30""")
 suspend fun search(q:String):List<LegalItemEntity>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertItems(v:List<LegalItemEntity>)
 @Query("UPDATE legal_items SET active=0") suspend fun deactivateAll()
}
@Dao interface LegalReleaseDao{
 @Query("SELECT * FROM legal_releases_local WHERE active=1 LIMIT 1") suspend fun active():LegalReleaseEntity?
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsert(v:LegalReleaseEntity)
 @Query("UPDATE legal_releases_local SET active=0") suspend fun deactivateAll()
}