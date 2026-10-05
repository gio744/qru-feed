from pathlib import Path
import psycopg
from app.config import settings

MIGRATIONS = Path(__file__).resolve().parents[1] / "database" / "migrations"

with psycopg.connect(settings.database_url) as conn:
    with conn.cursor() as cur:
        cur.execute("""CREATE TABLE IF NOT EXISTS schema_migrations(
            filename text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now()
        )""")
        conn.commit()
        for path in sorted(MIGRATIONS.glob("*.sql")):
            cur.execute("SELECT 1 FROM schema_migrations WHERE filename=%s", (path.name,))
            if cur.fetchone():
                continue
            cur.execute(path.read_text(encoding="utf-8"))
            cur.execute("INSERT INTO schema_migrations(filename) VALUES(%s)", (path.name,))
            conn.commit()
            print("Applied", path.name)
