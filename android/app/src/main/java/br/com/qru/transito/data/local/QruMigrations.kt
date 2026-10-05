package br.com.qru.transito.data.local
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object QruMigrations{
    val MIGRATION_1_2=object:Migration(1,2){
        override fun migrate(db:SupportSQLiteDatabase){
            db.execSQL("""CREATE TABLE IF NOT EXISTS legal_releases_local (
                version TEXT NOT NULL PRIMARY KEY,
                fingerprint TEXT NOT NULL,
                jurisdiction TEXT NOT NULL,
                installedAt INTEGER NOT NULL,
                active INTEGER NOT NULL
            )""")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_legal_releases_local_active ON legal_releases_local(active)")
        }
    }
}
