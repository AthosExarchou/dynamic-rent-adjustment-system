"""Normalization helpers for images downloaded by the offline scrapers."""

# Imported Libraries
from pathlib import Path


def to_app_image_paths(raw_paths, base_dir: Path) -> list[str]:
    """Convert pipe-separated offline paths into paths served by /api/images.

    The scrapers store absolute or relative filesystem paths in the CSV column
    image_paths_local. The application resolves paths relative to the analytics
    directory.
    """
    if not isinstance(raw_paths, str) or not raw_paths.strip():
        return []

    base = base_dir.resolve()
    result: list[str] = []

    for part in raw_paths.split("|"):
        part = part.strip()
        if not part or part.lower() in {"nan", "none", "null"}:
            continue

        path = Path(part).expanduser()
        if not path.is_absolute():
            path = base / path

        try:
            relative = path.resolve().relative_to(base)
        except (ValueError, OSError):
            continue

        result.append(relative.as_posix())

    return result
