from pathlib import Path
import io
import json
from typing import Any

import pandas as pd
import requests

from app.db import get_conn
from app.repositories import comment_repository, product_repository, seller_repository, task_repository
from app.utils.keyword_extractor import extract_keywords
from app.utils.problem_classifier import classify_problem
from app.utils.sentiment_analyzer import sentiment_from_score
from app.utils.text_cleaner import clean_text


REQUIRED_FILES = {
    "reviews": "olist_order_reviews_dataset.csv",
    "items": "olist_order_items_dataset.csv",
    "products": "olist_products_dataset.csv",
    "sellers": "olist_sellers_dataset.csv",
    "category_translation": "product_category_name_translation.csv",
}


def _read_csv(data_dir: Path, name: str) -> pd.DataFrame:
    path = data_dir / name
    if not path.exists():
        raise FileNotFoundError(f"Missing Olist CSV file: {path}")
    return pd.read_csv(path)


def _none_if_nan(value: Any) -> Any:
    if pd.isna(value):
        return None
    return value


def _decimal_or_none(value: Any) -> float | None:
    if pd.isna(value):
        return None
    return round(float(value), 4)


def _mean_or_none(series: pd.Series) -> float | None:
    numeric = pd.to_numeric(series, errors="coerce")
    return _decimal_or_none(numeric.mean())


def _row_value(row: dict[str, Any], *names: str) -> Any:
    for name in names:
        if name in row:
            return row.get(name)
    return None


class OlistImportService:
    def import_csv(self, request: dict[str, Any]) -> dict[str, Any]:
        task_id = int(request.get("taskId") or 0)
        data_path = request.get("dataPath")
        if not data_path:
            return self._import_single_comment_csv(request)

        data_dir = Path(str(data_path))
        if not data_dir.exists():
            raise FileNotFoundError(f"dataPath does not exist: {data_dir}")

        with get_conn() as conn:
            task_repository.update_analysis_task(conn, task_id, "processing", 10)

        reviews = _read_csv(data_dir, REQUIRED_FILES["reviews"])
        items = _read_csv(data_dir, REQUIRED_FILES["items"])
        products = _read_csv(data_dir, REQUIRED_FILES["products"])
        sellers = _read_csv(data_dir, REQUIRED_FILES["sellers"])
        translation = _read_csv(data_dir, REQUIRED_FILES["category_translation"])

        with get_conn() as conn:
            task_repository.update_analysis_task(conn, task_id, "processing", 35)

        order_product_seller = (
            items.groupby(["order_id", "product_id", "seller_id"], as_index=False)
            .agg(price=("price", "mean"), freight_value=("freight_value", "mean"))
        )
        comment_df = reviews.merge(order_product_seller, on="order_id", how="left")

        product_meta = products.merge(translation, on="product_category_name", how="left")
        comment_df = comment_df.merge(
            product_meta[["product_id", "product_category_name", "product_category_name_english"]],
            on="product_id",
            how="left",
        )

        comment_rows = self._build_comment_rows(comment_df)

        with get_conn() as conn:
            if request.get("importMode", "full") == "full":
                with conn.cursor() as cursor:
                    cursor.execute("delete from biz_comment")
                    cursor.execute("delete from biz_product")
                    cursor.execute("delete from biz_seller")
            imported_comments = comment_repository.replace_comments(conn, comment_rows)
            task_repository.update_analysis_task(conn, task_id, "processing", 70)

        product_rows = self._build_product_rows(comment_df)
        seller_rows = self._build_seller_rows(comment_df, sellers)

        with get_conn() as conn:
            product_count = product_repository.replace_products(conn, product_rows)
            seller_count = seller_repository.replace_sellers(conn, seller_rows)
            task_repository.update_analysis_task(conn, task_id, "success", 100)

        return {
            "success": True,
            "importedRows": imported_comments,
            "productCount": product_count,
            "sellerCount": seller_count,
            "message": "Olist CSV import completed",
        }

    def _import_single_comment_csv(self, request: dict[str, Any]) -> dict[str, Any]:
        task_id = int(request.get("taskId") or 0)

        with get_conn() as conn:
            task_repository.update_analysis_task(conn, task_id, "processing", 20)

        comment_df = self._load_single_comment_frame(request)
        comment_df = self._apply_column_mapping(comment_df, request.get("columnMapping"))
        comment_df = self._ensure_comment_columns(comment_df)
        comment_rows = self._build_comment_rows(comment_df)

        with get_conn() as conn:
            if request.get("importMode", "full") == "full":
                with conn.cursor() as cursor:
                    cursor.execute("delete from biz_comment")
                    cursor.execute("delete from biz_product")
                    cursor.execute("delete from biz_seller")
            imported_comments = comment_repository.replace_comments(conn, comment_rows)
            task_repository.update_analysis_task(conn, task_id, "processing", 70)

        product_rows = self._build_product_rows(comment_df)
        seller_rows = self._build_seller_rows(comment_df, pd.DataFrame(columns=["seller_id"]))

        with get_conn() as conn:
            product_count = product_repository.replace_products(conn, product_rows)
            seller_count = seller_repository.replace_sellers(conn, seller_rows)
            task_repository.update_analysis_task(conn, task_id, "success", 100)

        return {
            "success": True,
            "importedRows": imported_comments,
            "productCount": product_count,
            "sellerCount": seller_count,
            "message": "Single comment CSV import completed",
        }

    def _load_single_comment_frame(self, request: dict[str, Any]) -> pd.DataFrame:
        if request.get("sampleData"):
            sample_path = Path(__file__).resolve().parents[1] / "sample_data" / "sample_reviews.csv"
            return pd.read_csv(sample_path)

        file_url = request.get("fileUrl")
        if not file_url:
            raise ValueError("dataPath is required for Olist import, or fileUrl is required for single CSV import")
        response = requests.get(str(file_url), timeout=60)
        response.raise_for_status()
        return pd.read_csv(io.BytesIO(response.content))

    def _apply_column_mapping(self, comment_df: pd.DataFrame, column_mapping: Any) -> pd.DataFrame:
        if not isinstance(column_mapping, dict) or not column_mapping:
            return comment_df

        mapped_df = comment_df.copy()
        for target_column, source_column in column_mapping.items():
            if not target_column or not source_column:
                continue
            target = str(target_column).strip()
            source = str(source_column).strip()
            if not target or not source or source not in mapped_df.columns:
                continue
            if source == target:
                continue
            mapped_df[target] = mapped_df[source]
        return mapped_df

    def _build_comment_rows(self, comment_df: pd.DataFrame) -> list[dict[str, Any]]:
        self._validate_comment_columns(comment_df)
        comment_rows: list[dict[str, Any]] = []
        for row in comment_df.to_dict("records"):
            title = clean_text(_row_value(row, "review_title", "review_comment_title") or "")
            content = clean_text(_row_value(row, "review_content", "review_comment_message") or "")
            clean_content = clean_text(_row_value(row, "clean_content") or f"{title} {content}")
            sentiment, score, is_negative = sentiment_from_score(_row_value(row, "review_score"))
            problem_type = classify_problem(clean_content, is_negative)
            keywords = extract_keywords(clean_content)
            comment_rows.append(
                {
                    "review_id": _none_if_nan(_row_value(row, "review_id")),
                    "order_id": _none_if_nan(_row_value(row, "order_id")),
                    "product_id": _none_if_nan(_row_value(row, "product_id")),
                    "seller_id": _none_if_nan(_row_value(row, "seller_id")),
                    "review_score": int(_row_value(row, "review_score")),
                    "review_title": title,
                    "review_content": content,
                    "clean_content": clean_content,
                    "review_time": _none_if_nan(_row_value(row, "review_time", "review_creation_date")),
                    "sentiment": sentiment,
                    "sentiment_score": score,
                    "keywords": json.dumps(keywords, ensure_ascii=False),
                    "problem_type": problem_type,
                    "is_negative": is_negative,
                }
            )
        return comment_rows

    def _validate_comment_columns(self, comment_df: pd.DataFrame) -> None:
        required = {"product_id", "review_score"}
        missing = sorted(required - set(comment_df.columns))
        if missing:
            raise ValueError("single CSV import requires columns: product_id, review_score")

    def _ensure_comment_columns(self, comment_df: pd.DataFrame) -> pd.DataFrame:
        defaults = {
            "review_id": None,
            "order_id": None,
            "seller_id": None,
            "review_title": "",
            "review_content": "",
            "review_time": None,
            "product_category_name": None,
            "product_category_name_english": None,
            "price": None,
            "freight_value": None,
        }
        for column, default in defaults.items():
            if column not in comment_df.columns:
                comment_df[column] = default
        return comment_df

    def _build_product_rows(self, comment_df: pd.DataFrame) -> list[dict[str, Any]]:
        rows: list[dict[str, Any]] = []
        grouped = comment_df.dropna(subset=["product_id"]).groupby("product_id")
        for product_id, group in grouped:
            seller_id = group["seller_id"].dropna().mode()
            rows.append(
                {
                    "product_id": str(product_id),
                    "seller_id": None if seller_id.empty else str(seller_id.iloc[0]),
                    "category_name": _none_if_nan(group["product_category_name"].dropna().iloc[0])
                    if group["product_category_name"].notna().any()
                    else None,
                    "category_name_en": _none_if_nan(group["product_category_name_english"].dropna().iloc[0])
                    if group["product_category_name_english"].notna().any()
                    else None,
                    "avg_price": _mean_or_none(group["price"]),
                    "avg_freight": _mean_or_none(group["freight_value"]),
                    "order_count": int(group["order_id"].nunique()),
                    "review_count": int(len(group)),
                    "avg_score": _mean_or_none(group["review_score"]),
                    "negative_rate": _decimal_or_none((pd.to_numeric(group["review_score"], errors="coerce") <= 2).mean()),
                }
            )
        return rows

    def _build_seller_rows(self, comment_df: pd.DataFrame, sellers: pd.DataFrame) -> list[dict[str, Any]]:
        rows: list[dict[str, Any]] = []
        seller_meta = sellers.set_index("seller_id").to_dict("index")
        grouped = comment_df.dropna(subset=["seller_id"]).groupby("seller_id")
        for seller_id, group in grouped:
            meta = seller_meta.get(seller_id, {})
            rows.append(
                {
                    "seller_id": str(seller_id),
                    "seller_city": _none_if_nan(meta.get("seller_city")),
                    "seller_state": _none_if_nan(meta.get("seller_state")),
                    "product_count": int(group["product_id"].nunique()),
                    "order_count": int(group["order_id"].nunique()),
                    "avg_score": _mean_or_none(group["review_score"]),
                    "negative_rate": _decimal_or_none((pd.to_numeric(group["review_score"], errors="coerce") <= 2).mean()),
                }
            )
        return rows
