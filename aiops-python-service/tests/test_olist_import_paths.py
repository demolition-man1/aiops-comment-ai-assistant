from pathlib import Path

import pytest

from app.services.olist_import_service import resolve_import_data_path


def test_resolve_import_data_path_maps_a_windows_child_path_to_the_mounted_directory(tmp_path: Path) -> None:
    mounted_root = tmp_path / "local-import"
    dataset = mounted_root / "olist-brazilian-ecommerce"
    dataset.mkdir(parents=True)

    resolved = resolve_import_data_path(
        r"Z:\demo-root\olist-brazilian-ecommerce",
        host_root="Z:/demo-root",
        mounted_root=mounted_root,
    )

    assert resolved == dataset


def test_resolve_import_data_path_does_not_map_a_path_outside_the_configured_host_root(tmp_path: Path) -> None:
    mounted_root = tmp_path / "local-import"
    (mounted_root / "olist-brazilian-ecommerce").mkdir(parents=True)

    with pytest.raises(FileNotFoundError, match="dataPath does not exist"):
        resolve_import_data_path(
            r"Z:\other\olist-brazilian-ecommerce",
            host_root="Z:/demo-root",
            mounted_root=mounted_root,
        )


def test_resolve_import_data_path_rejects_parent_traversal_outside_the_mounted_directory(tmp_path: Path) -> None:
    mounted_root = tmp_path / "local-import"
    (tmp_path / "outside" / "olist-brazilian-ecommerce").mkdir(parents=True)

    with pytest.raises(FileNotFoundError, match="dataPath does not exist"):
        resolve_import_data_path(
            r"Z:\demo-root\..\outside\olist-brazilian-ecommerce",
            host_root="Z:/demo-root",
            mounted_root=mounted_root,
        )
