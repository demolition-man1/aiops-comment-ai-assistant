from collections.abc import Iterator
from contextlib import contextmanager
from typing import Any

from app.config import settings


def _connect():
    try:
        import pymysql
    except ImportError as exc:
        raise RuntimeError("PyMySQL is not installed. Run `pip install -r requirements.txt`.") from exc

    return pymysql.connect(
        host=settings.mysql_host,
        port=settings.mysql_port,
        user=settings.mysql_user,
        password=settings.mysql_password,
        database=settings.mysql_database,
        charset="utf8mb4",
        autocommit=False,
        cursorclass=pymysql.cursors.DictCursor,
    )


@contextmanager
def get_conn() -> Iterator[Any]:
    conn = _connect()
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
