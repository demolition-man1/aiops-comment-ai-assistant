from typing import Any


def replace_products(conn: Any, rows: list[dict[str, Any]]) -> int:
    if not rows:
        return 0
    sql = """
        insert into biz_product
        (product_id, seller_id, category_name, category_name_en, avg_price, avg_freight,
         order_count, review_count, avg_score, negative_rate, create_time, update_time)
        values
        (%(product_id)s, %(seller_id)s, %(category_name)s, %(category_name_en)s, %(avg_price)s,
         %(avg_freight)s, %(order_count)s, %(review_count)s, %(avg_score)s, %(negative_rate)s, now(), now())
        on duplicate key update
            seller_id = values(seller_id),
            category_name = values(category_name),
            category_name_en = values(category_name_en),
            avg_price = values(avg_price),
            avg_freight = values(avg_freight),
            order_count = values(order_count),
            review_count = values(review_count),
            avg_score = values(avg_score),
            negative_rate = values(negative_rate),
            update_time = now()
    """
    with conn.cursor() as cursor:
        cursor.executemany(sql, rows)
    return len(rows)
