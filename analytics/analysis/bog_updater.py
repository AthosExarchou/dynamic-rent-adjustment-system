"""
Bank of Greece Dataset Updater.
Handles periodic downloading and caching of the apartment price index dataset.
"""

# Imported libraries
from __future__ import annotations
import re
import json
import logging
import datetime
from pathlib import Path
from typing import Optional, Dict, Any, Tuple
import requests
import pandas as pd

logger = logging.getLogger("dras.bog_updater")

BOG_API_URL = "https://opendata.bankofgreece.gr/api.aspx/getDatasetDetails?parentDatasetId=5"
BOG_BASE_URL = "https://www.bankofgreece.gr"
DEFAULT_CHECK_INTERVAL_DAYS = 14

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"

METADATA_FILE = DATA_DIR / "bog_dataset_metadata.json"
ACTIVE_XLS_FILE = DATA_DIR / "bank_of_greece_apartment_index.xls"
NORMALIZED_CSV_FILE = DATA_DIR / "bank_of_greece_apartment_index.csv"


def load_metadata(meta_path: Optional[Path] = None) -> Dict[str, Any]:
    """Loads existing dataset synchronization metadata from disk."""
    path = meta_path or METADATA_FILE
    if path.exists():
        try:
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            logger.warning(f"Could not read metadata file {path}: {e}")
    return {}


def save_metadata(metadata: Dict[str, Any], meta_path: Optional[Path] = None) -> None:
    """Saves dataset synchronization metadata to disk."""
    path = meta_path or METADATA_FILE
    path.parent.mkdir(parents=True, exist_ok=True)
    temp_path = path.with_suffix(".tmp")
    with open(temp_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2, default=str)
    temp_path.replace(path)


def is_ttl_expired(metadata: Dict[str, Any], interval_days: int) -> bool:
    """Checks whether the cache duration has elapsed since the last API check."""
    last_checked = metadata.get("last_checked_at")
    if not last_checked:
        return True
    try:
        dt = datetime.datetime.fromisoformat(last_checked)
        elapsed = datetime.datetime.now() - dt
        return elapsed >= datetime.timedelta(days=interval_days)
    except Exception:
        return True


def parse_quarter_period(frequency_period: str) -> str:
    """Parses a quarter period string into 'YYYY-QN' format."""
    if not frequency_period:
        return "UNKNOWN"

    s = frequency_period.strip()

    # Format: YYYY-QN  (e.g. "2026-Q4")
    m = re.match(r"^(20\d{2})-Q([1-4])$", s, re.IGNORECASE)
    if m:
        return f"{m.group(1)}-Q{m.group(2)}"

    # Format: QN YYYY  (e.g. "Q2 2026")
    m = re.match(r"^Q([1-4])\s+(20\d{2})$", s, re.IGNORECASE)
    if m:
        return f"{m.group(2)}-Q{m.group(1)}"

    # Format: Greek "Nο Τρίμηνο YYYY" - quarter digit is the LEADING digit before optional ordinal suffix
    # Anchored at start so it cannot match a digit inside the year component.
    m = re.match(r"^([1-4])[οo°.]?\s", s)
    year_m = re.search(r"(20\d{2})", s)
    if m and year_m:
        return f"{year_m.group(1)}-Q{m.group(1)}"

    # Format: English ordinal "Nth Quarter YYYY"  (e.g. "1st Quarter 2026", "3rd Quarter 2025")
    m = re.match(r"^([1-4])(?:st|nd|rd|th)\s+Quarter\s+(20\d{2})$", s, re.IGNORECASE)
    if m:
        return f"{m.group(2)}-Q{m.group(1)}"

    logger.warning(
        "Unrecognized FrequencyPeriod format: %r - returning raw value. "
        "Add an explicit pattern to parse_quarter_period() if this format is valid.",
        frequency_period,
    )
    return s


def query_bog_api(timeout: int = 15) -> Tuple[Optional[Dict[str, Any]], Optional[str]]:
    """
    Queries Bank of Greece Open Data API for Dataset 5 file list.
    Returns: (latest_file_entry, error_message)
    """
    headers = {
        "User-Agent": "DRAS-Analytics/1.0 (Dynamic Rent Adjustment System; Research)",
        "Accept": "application/json, text/plain, */*",
        "Content-Type": "application/json; charset=utf-8"
    }
    try:
        response = requests.get(BOG_API_URL, headers=headers, timeout=timeout)
        response.raise_for_status()
        data = response.json()
        files = data.get("ODSItemFiles", [])
        if not files:
            return None, "Bank of Greece API returned empty file list (ODSItemFiles)"

        # Filter for valid Excel files only (.xls, .xlsx)
        excel_files = [f for f in files if str(f.get("Name", "")).lower().endswith((".xls", ".xlsx"))]
        if not excel_files:
            return None, "Bank of Greece API returned no Excel (.xls, .xlsx) files"

        # Prefer English version (_en_*.xls or Language == 2), sorted by PublicationDate or ContentDate descending
        english_files = [f for f in excel_files if f.get("Language") == 2 or "_en_" in f.get("Name", "")]
        candidate_files = english_files if english_files else excel_files
        candidate_files.sort(
            key=lambda x: (x.get("PublicationDate", 0) or 0, x.get("ContentDate", 0) or 0),
            reverse=True
        )
        return candidate_files[0], None
    except Exception as e:
        return None, f"Network or parsing error querying Bank of Greece API: {e}"


_REQUIRED_XLS_COLUMNS = [
    "Reference_year",
    "Reference_quarter",
    "New_index_of_apartment_prices_by_geographical_area_Athens",
]
_ATHENS_COLUMN = "New_index_of_apartment_prices_by_geographical_area_Athens"
_VALID_QUARTERS = {1, 2, 3, 4}
_MIN_QUARTERLY_RECORDS = 20


def validate_downloaded_xls(
    temp_path: Path,
    required_columns: list[str] = _REQUIRED_XLS_COLUMNS,
    athens_column: str = _ATHENS_COLUMN,
    min_records: int = _MIN_QUARTERLY_RECORDS,
) -> None:
    """Validates a downloaded Bank of Greece Excel file before it replaces the active dataset.

    Raises:
        ValueError: with a descriptive message if any validation step fails.
    """
    engine = "xlrd" if temp_path.suffix.lower() == ".xls" else "openpyxl"
    try:
        df = pd.read_excel(temp_path, engine=engine)
    except Exception as exc:
        raise ValueError(
            f"Downloaded file is not a readable Excel workbook: {exc}"
        ) from exc

    # Required columns must be present
    missing_cols = [c for c in required_columns if c not in df.columns]
    if missing_cols:
        raise ValueError(
            f"Downloaded workbook is missing required columns: {missing_cols}. "
            f"Found columns: {df.columns.tolist()}"
        )

    # Coerce numeric columns and check minimum valid observation count
    clean_df = df.copy()
    for col in required_columns:
        clean_df[col] = pd.to_numeric(clean_df[col], errors="coerce")
    clean_df = clean_df.dropna(subset=required_columns).copy()

    # Ensure price index observations are strictly positive and finite
    clean_df = clean_df[clean_df[athens_column] > 0].copy()

    if len(clean_df) < min_records:
        raise ValueError(
            f"Dataset contains only {len(clean_df)} valid numeric observations "
            f"(minimum required: {min_records})."
        )

    # Verify Reference_quarter values
    clean_df["Quarter"] = clean_df["Reference_quarter"].astype(int)
    quarters_seen = set(clean_df["Quarter"])
    invalid_quarters = quarters_seen - _VALID_QUARTERS
    if invalid_quarters:
        raise ValueError(
            f"Downloaded workbook contains invalid Reference_quarter values: {invalid_quarters}. "
            "Expected only values in {1, 2, 3, 4}."
        )

    # Verify contiguous quarterly series without missing interior quarters
    clean_df["Year"] = clean_df["Reference_year"].astype(int)
    clean_df["Month"] = (clean_df["Quarter"] - 1) * 3 + 1
    clean_df["Date"] = pd.to_datetime(
        clean_df["Year"].astype(str) + "-" + clean_df["Month"].astype(str).str.zfill(2) + "-01"
    )
    clean_df = clean_df.sort_values("Date").drop_duplicates(subset=["Date"]).reset_index(drop=True).set_index("Date")

    ts = clean_df[athens_column].astype(float).asfreq("QS-JAN")
    missing_quarters = ts[ts.isna()].index.tolist()
    if missing_quarters:
        missing_str = ", ".join(str(q.date()) for q in missing_quarters)
        raise ValueError(
            f"Downloaded workbook has {len(missing_quarters)} missing quarter(s) in time series: {missing_str}"
        )

    logger.info(
        "Validation passed: %d records, %d valid observations, quarters %s, range %s to %s.",
        len(df),
        len(clean_df),
        sorted(quarters_seen),
        clean_df.index[0].date(),
        clean_df.index[-1].date(),
    )


def convert_xls_to_clean_csv(xls_path: Path, csv_path: Path) -> pd.DataFrame:
    """Reads Excel file using xlrd or openpyxl and saves a normalized CSV copy."""
    engine = "xlrd" if xls_path.suffix.lower() == ".xls" else "openpyxl"
    df = pd.read_excel(xls_path, engine=engine)
    csv_path.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(csv_path, index=False)
    logger.info(f"Exported clean CSV dataset to {csv_path} ({len(df)} records)")
    return df


def check_and_update_bog_dataset(
    target_dir: Optional[Path] = None,
    check_interval_days: int = DEFAULT_CHECK_INTERVAL_DAYS,
    force: bool = False,
    timeout: int = 15
) -> Dict[str, Any]:
    """
    Checks the Bank of Greece API for newly published quarterly apartment price indices.
    If the TTL has not expired and force is False, uses the existing local cache.
    If a new quarter is published, downloads the updated file and refreshes local caches.

    Returns:
        dict: Sync report containing status, active_file, latest_quarter, and metadata.
    """
    data_dir = target_dir or DATA_DIR
    meta_path = data_dir / "bog_dataset_metadata.json"
    active_csv = data_dir / "bank_of_greece_apartment_index.csv"

    # Find existing cached dataset (supports .xlsx and .xls)
    cached_file = None
    for candidate in [
        data_dir / "bank_of_greece_apartment_index.xlsx",
        data_dir / "bank_of_greece_apartment_index.xls",
    ]:
        if candidate.exists():
            cached_file = candidate
            break

    metadata = load_metadata(meta_path)
    current_time_str = datetime.datetime.now().isoformat()

    fallback_file = cached_file or (data_dir / "bank_of_greece_apartment_index.xls")

    # Check TTL
    if not force and cached_file is not None and not is_ttl_expired(metadata, check_interval_days):
        logger.info(
            f"Bank of Greece dataset checked recently ({metadata.get('last_checked_at')}). "
            f"Within TTL ({check_interval_days} days). Using cached dataset."
        )
        return {
            "status": "cached_within_ttl",
            "active_file": cached_file,
            "csv_file": active_csv if active_csv.exists() else None,
            "latest_quarter": metadata.get("latest_available_period", "UNKNOWN"),
            "publication_date": metadata.get("latest_publication_date", "UNKNOWN"),
            "metadata": metadata,
            "message": "Local cached Bank of Greece dataset is within the TTL interval."
        }

    # Query Bank of Greece API
    logger.info("Connecting to Bank of Greece Open Data API to check for new quarters...")
    latest_item, error = query_bog_api(timeout=timeout)

    if error or not latest_item:
        logger.warning(f"Bank of Greece API check failed: {error}")
        if fallback_file.exists():
            logger.info(f"Gracefully falling back to local cached dataset: {fallback_file.name}")
            return {
                "status": "offline_fallback",
                "active_file": fallback_file,
                "csv_file": active_csv if active_csv.exists() else None,
                "latest_quarter": metadata.get("latest_available_period", "UNKNOWN"),
                "publication_date": metadata.get("latest_publication_date", "UNKNOWN"),
                "metadata": metadata,
                "message": f"Bank of Greece API unavailable ({error}). Fallback to local cache succeeded."
            }
        else:
            raise RuntimeError(f"Bank of Greece API failed and no local fallback exists: {error}")

    # Analyze latest release
    file_name = latest_item.get("Name", "")
    ext = Path(file_name).suffix.lower() if file_name and Path(file_name).suffix else ".xls"
    active_dataset_file = data_dir / f"bank_of_greece_apartment_index{ext}"

    rel_path = latest_item.get("Path", "/OpenDataSetFilesALL/DOAM")
    download_url = f"{BOG_BASE_URL}{rel_path}/{file_name}"
    pub_timestamp = latest_item.get("PublicationDate")
    pub_date_str = "UNKNOWN"
    if isinstance(pub_timestamp, (int, float)):
        try:
            pub_date_str = datetime.datetime.fromtimestamp(pub_timestamp / 1000).date().isoformat()
        except (OverflowError, OSError, ValueError):
            pub_date_str = "UNKNOWN"
    elif isinstance(pub_timestamp, str):
        pub_date_str = pub_timestamp[:10]

    quarter_label = parse_quarter_period(latest_item.get("FrequencyPeriod", ""))

    stored_file_name = metadata.get("latest_file_name")
    is_new_quarter = (stored_file_name != file_name) or (not active_dataset_file.exists())

    if not is_new_quarter and not force:
        # Update last_checked timestamp in metadata
        metadata["last_checked_at"] = current_time_str
        metadata["check_interval_days"] = check_interval_days
        save_metadata(metadata, meta_path)
        logger.info(f"Local Bank of Greece dataset is already up to date ({quarter_label}, {file_name}).")
        return {
            "status": "already_up_to_date",
            "active_file": active_dataset_file if active_dataset_file.exists() else fallback_file,
            "csv_file": active_csv if active_csv.exists() else None,
            "latest_quarter": quarter_label,
            "publication_date": pub_date_str,
            "metadata": metadata,
            "message": f"Bank of Greece dataset is already at latest release ({quarter_label})."
        }

    # Download new release
    logger.info(f"Downloading new Bank of Greece release: {file_name} from {download_url}...")
    temp_download = data_dir / f".tmp_download_{file_name}"
    try:
        resp = requests.get(download_url, stream=True, timeout=30)
        resp.raise_for_status()
        with open(temp_download, "wb") as f:
            for chunk in resp.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)

        # Validate before replacing existing active file
        try:
            validate_downloaded_xls(temp_download)
        except ValueError as validation_error:
            temp_download.unlink(missing_ok=True)
            raise ValueError(
                f"Downloaded file failed validation: {validation_error}"
            ) from validation_error

        temp_download.replace(active_dataset_file)
        logger.info(f"Successfully saved new dataset to {active_dataset_file.name}")

        # If format changed between .xls and .xlsx, remove the old alternate file
        other_ext = ".xlsx" if ext == ".xls" else ".xls"
        old_format_file = data_dir / f"bank_of_greece_apartment_index{other_ext}"
        if old_format_file.exists():
            try:
                old_format_file.unlink()
            except Exception:
                pass

        # Convert to normalized CSV and capture record count without re-reading the file
        records_count = None
        try:
            df_exported = convert_xls_to_clean_csv(active_dataset_file, active_csv)
            records_count = len(df_exported)
        except Exception as e:
            logger.warning(f"Could not export normalized CSV: {e}")

        # Update metadata
        metadata = {
            "last_checked_at": current_time_str,
            "latest_available_period": quarter_label,
            "latest_file_name": file_name,
            "latest_publication_date": pub_date_str,
            "download_url": download_url,
            "check_interval_days": check_interval_days,
            "records_count": records_count,
        }
        save_metadata(metadata, meta_path)

        return {
            "status": "updated",
            "active_file": active_dataset_file,
            "csv_file": active_csv if active_csv.exists() else None,
            "latest_quarter": quarter_label,
            "publication_date": pub_date_str,
            "metadata": metadata,
            "message": f"Successfully updated Bank of Greece dataset to {quarter_label} ({file_name})."
        }
    except Exception as e:
        logger.error(f"Download or save failed: {e}")
        if temp_download.exists():
            try:
                temp_download.unlink()
            except Exception:
                pass
        if fallback_file.exists():
            logger.info("Using existing local dataset as fallback.")
            return {
                "status": "offline_fallback",
                "active_file": fallback_file,
                "csv_file": active_csv if active_csv.exists() else None,
                "latest_quarter": metadata.get("latest_available_period", "UNKNOWN"),
                "publication_date": metadata.get("latest_publication_date", "UNKNOWN"),
                "metadata": metadata,
                "message": f"Failed to download new release ({e}). Using existing local cache."
            }
        raise


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
    result = check_and_update_bog_dataset(force=True)
    print("\n--- Bank of Greece Dataset Update Result ---")
    print(f"Status: {result['status']}")
    print(f"Active File: {result['active_file']}")
    print(f"Latest Quarter: {result['latest_quarter']}")
    print(f"Publication Date: {result['publication_date']}")
    print(f"Message: {result['message']}")
