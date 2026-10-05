from contextlib import contextmanager
import psycopg
from .config import settings

@contextmanager
def connection():
    with psycopg.connect(settings.database_url) as conn:
        yield conn

def ping_db() -> bool:
    try:
        with connection() as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT 1")
                return cur.fetchone()[0] == 1
    except Exception:
        return False
