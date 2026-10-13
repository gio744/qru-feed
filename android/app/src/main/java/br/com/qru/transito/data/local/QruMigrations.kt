package br.com.qru.transito.data.local
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object QruMigrations{
    val MIGRATION_2_3=object:Migration(2,3){
        override fun migrate(db:SupportSQLiteDatabase){
            db.execSQL("ALTER TABLE legal_items ADD COLUMN source TEXT")
            db.execSQL("ALTER TABLE legal_items ADD COLUMN reviewStatus TEXT NOT NULL DEFAULT 'Validação final pendente'")
            db.execSQL("ALTER TABLE legal_items ADD COLUMN appliesFrom TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE legal_items ADD COLUMN appliesUntil TEXT NOT NULL DEFAULT ''")
        }
    }

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
