from typing import Any


def replace_sellers(conn: Any, rows: list[dict[str, Any]]) -> int:
    if not rows:
        return 0
    sql = """
        insert into biz_seller
        (seller_id, seller_city, seller_state, product_count, order_count, avg_score,
         negative_rate, create_time, update_time)
        values
        (%(seller_id)s, %(seller_city)s, %(seller_state)s, %(product_count)s, %(order_count)s,
         %(avg_score)s, %(negative_rate)s, now(), now())
        on duplicate key update
            seller_city = values(seller_city),
            seller_state = values(seller_state),
            product_count = values(product_count),
            order_count = values(order_count),
            avg_score = values(avg_score),
            negative_rate = values(negative_rate),
            update_time = now()
    """
    with conn.cursor() as cursor:
        cursor.executemany(sql, rows)
    return len(rows)
