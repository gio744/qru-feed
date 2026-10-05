package br.com.qru.transito.data.local
import androidx.room.*
@Database(entities=[LegalItemEntity::class,LegalReleaseEntity::class],version=2,exportSchema=true)
abstract class QruDatabase:RoomDatabase(){abstract fun legalDao():LegalDao;abstract fun legalReleaseDao():LegalReleaseDao}