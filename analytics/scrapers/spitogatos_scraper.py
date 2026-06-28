"""
Dynamic Rent Adjustment System (DRAS) - Spitogatos Web Scraping Pipeline
-----------------------------------------------------------------------
This module serves as the primary data acquisition engine for the DRAS
platform, automating the collection of residential rental listings from
Spitogatos (spitogatos.gr) for the Athens city center.

To ensure high data fidelity and bypass enterprise anti-bot systems such as
Datadome & Cloudflare. The scraper employs a strict Two-Phase Architecture:

    * Phase 1 (Discovery): Systematically navigates paginated search results,
      extracts preview-level data, and compiles a deduplicated registry of
      unique listing URLs.
    * Phase 2 (Extraction): Iterates through the discovered URLs, visiting
      each individual detail page to extract deep characteristics, full
      descriptions, inferred rental durations, and high-resolution CDN links.

Data Hygiene & Validation:
Prior to export, the dataset passes through a Centralized Validation Layer.
Listings with missing critical values or those falling outside strictly
defined economic bounds (e.g. €/m2, total size) are flagged and filtered
to prevent market outliers from corrupting downstream ARIMA forecasting models.

Image Pipeline & Lifecycle Management:
While metadata and listing survival ("first_seen", "last_seen") are tracked
unconditionally, executing the script in '--full' mode activates the Image
Pipeline. This downloads up to a defined limit of images per listing locally,
employing retry logic, size thresholding, and an automated 30-day Garbage
Collection (GC) policy for stale properties.

Key Features:
- Stealth browser automation (undetected-chromedriver)
- Robust numeric parsing for Greek/European formats
- Data validation with rule-based filtering and reporting
- Incremental dataset updates with deduplication
- Image downloading with retry logic and garbage collection
- Backend integration via chunked API requests
- Configurable execution modes (fast vs full, normal vs weekly)
- Virtual display support for headless environments

Outputs:
- data/spitogatos_athens_listings.csv
    Latest deduplicated snapshot of listings
- data/spitogatos_history.csv / spitogatos_history_weekly.csv
    Aggregated historical price metrics (€/m2)
- data/images/<listing_id>/
    Optional per-listing image storage with metadata tracking
- failed_payloads/
    Failed backend payloads

Usage:
    python spitogatos_scraper.py
        -> Executes in fast mode (default mode scraping only search results).

    python spitogatos_scraper.py --full
        -> Enables full mode with image downloading and local storage.

    python spitogatos_scraper.py --mode weekly
        -> Stores aggregated results in the weekly historical dataset.

    python spitogatos_scraper.py --max-pages 3 --display-mode virtual
        -> Runs in fast mode using a virtual display and scrapes up to 3 pages.

Dependencies:
    selenium
    undetected-chromedriver
    webdriver-manager
    beautifulsoup4
    pandas
    requests

Author: Athos Exarchou
Date: 04.04.2026

Disclaimer:
This pipeline is intended for research and educational purposes only. Users are
responsible for complying with Spitogatos’s Terms of Service and applicable laws.
"""

# Imported Libraries
from webdriver_manager.chrome import ChromeDriverManager
from bs4 import BeautifulSoup
from pathlib import Path
from urllib.parse import urlparse, unquote, urljoin
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.by import By
from typing import List, Dict, Any
from dataclasses import dataclass
from logging.handlers import RotatingFileHandler
import textwrap
import logging
import undetected_chromedriver as uc
import pandas as pd
import requests
import time
import random
import re
import os
import shutil
from constants import PPM2_MIN, PPM2_MAX
import json
import hashlib
import argparse
import datetime as dt
import ast
import platform

# Sleep Tracker
_original_sleep = time.sleep
_total_sleep_time = 0.0

def _tracked_sleep(secs):
    global _total_sleep_time
    if secs > 0:
        try:
            _total_sleep_time += float(secs)
        except (TypeError, ValueError):
            pass
    _original_sleep(secs)

time.sleep = _tracked_sleep

logger = logging.getLogger("dras.scraper")

__version__ = "1.2.1"

# CONFIGURATION
DATA_DIR = Path("data")
DATA_DIR.mkdir(exist_ok=True)
LATEST_CSV = DATA_DIR / "spitogatos_athens_listings.csv"
HISTORY_AGG_CSV = DATA_DIR / "spitogatos_history.csv"
IMAGES_DIR = DATA_DIR / "images"  # Images stored per-listing in subfolders
FAILED_DIR = Path("failed_payloads")
FAILED_DIR.mkdir(exist_ok=True)

VALID_EXTS = {".jpg", ".jpeg", ".webp", ".png"}

# Logic Params
WAIT_SECONDS = 15
MAX_IMAGES = 5  # max number of images downloaded per listing
GRACE_DAYS = 30  # Garbage Collection

# Backend Integration
CHUNK_SIZE = 100
MAX_RETRIES = 3
RETRY_DELAY = 2

# Centralized Validation Rules
VALIDATION_RULES = {
    "price": {"min": 100, "max": 20000},
    "size": {"min": 20, "max": 500},
    "price_per_m2": {"min": PPM2_MIN, "max": PPM2_MAX},
    "floor": {"min": -3, "max": 100},
    "yearBuilt": {"min": 1800, "max": dt.datetime.now().year + 5},
    "bedrooms": {"min": 0, "max": 10},
    "bathrooms": {"min": 0, "max": 5},
}


# Config Object
@dataclass
class ScraperConfig:
    full: bool
    mode: str
    max_pages: int
    display_mode: str

    push_backend: bool
    backend_url: str


# Logging Setup

def setup_logging(debug: bool = False):
    level = logging.DEBUG if debug else logging.INFO
    formatter = logging.Formatter(
        "%(asctime)s.%(msecs)03d | %(levelname)-8s | %(name)s | %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)

    file_handler = RotatingFileHandler(
        "scraper.log",
        maxBytes=5_000_000,
        backupCount=3,
        encoding="utf-8"
    )
    file_handler.setFormatter(formatter)

    logging.basicConfig(
        level=level,
        handlers=[console_handler, file_handler]
    )

    logging.getLogger("WDM").setLevel(logging.WARNING)
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    logging.getLogger("selenium").setLevel(logging.WARNING)


# Utilities

def safe_int(x, default=None):
    try:
        return int(float(x))
    except (TypeError, ValueError):
        return default


def safe_float(x, default=None):
    try:
        return float(x)
    except (TypeError, ValueError):
        return default


def ensure_list(x):
    if isinstance(x, list):
        return x
    if isinstance(x, str):
        try:
            parsed = ast.literal_eval(x)
            return parsed if isinstance(parsed, list) else []
        except Exception:
            return []
    return []


def parse_numeric(x):
    """Numeric parsing for strings such as '1.234,56 €' or '950'."""
    if pd.isna(x):
        return None

    if isinstance(x, (int, float)):
        return float(x)

    s = str(x).strip()
    if s == "":
        return None
    # Removes euro and non-digit except ".", and ","
    s = re.sub(r"[^\d.,\-]", "", s)
    # If both "." and "," exist and "." appears before comma, treats "." as thousand seperator
    if "." in s and "," in s:
        # Converts thousand separators
        if s.rfind(".") < s.rfind(","):
            s = s.replace(".", "").replace(",", ".")
    # If only "," is present and no ".", then comma is decimal
    elif "," in s and "." not in s:
        s = s.replace(",", ".")
    try:
        return float(s)
    except Exception:
        return None


# Helper functions

def parse_money_to_float(text: str) -> float | None:
    """
    Parses Greek/European money strings.
    Examples:
    - "1.200,50" -> 1200.5
    - "1.200"    -> 1200.0
    - "850"      -> 850.0
    - "10,5"     -> 10.5
    """
    if not text:
        return None

    # Removes everything except digits, dots, commas, and minus
    s = re.sub(r"[^\d.,\-]", "", str(text))

    if not s:
        return None

    # Case 1: Both separators exist (e.g. 1.200,50) -> dot is thousand
    if "." in s and "," in s:
        s = s.replace(".", "").replace(",", ".")

    # Case 2: Only comma exists (e.g. 850,50) -> comma is decimal
    elif "," in s:
        s = s.replace(",", ".")

    # Case 3: Only dot exists (e.g. 1.200)
    # In Greek listings, 1.200 is 1200 (thousands), not 1.2 (decimal).
    elif "." in s:
        if re.match(r"^\d{1,3}(\.\d{3})+$", s):
            s = s.replace(".", "")  # strips the dot

    try:
        return float(s)
    except ValueError:
        return None


def parse_floor(floor_str) -> int | None:
    """
    Converts Greek floor strings into normalized ordinal integers.

    Mapping:
    - Υπόγειο / Ημιυπόγειο -> -1
    - Ισόγειο -> 0
    - Ημιώροφος -> 1
    - Numeric floors remain numeric
    """
    if pd.isna(floor_str):
        return None

    s = str(floor_str).strip().lower()

    if not s:
        return None

    # Normalize separators/spaces
    s = re.sub(r"\s+", "", s)

    # Semi-basement / basement
    basement_patterns = [
        "ημ/υπ",
        "ημιυπ",
        "ημιυπογ",
        "υπογ",
        "υπόγ"
    ]

    if any(p in s for p in basement_patterns):
        return -1

    # Ground floor
    ground_patterns = [
        "ισ.",
        "ισογ",
        "ισόγ",
        "ισόγειο"
    ]

    if s in ground_patterns or any(p in s for p in ground_patterns):
        return 0

    # Mezzanine / half-floor
    mezzanine_patterns = [
        "ημ",
        "ημ.",
        "ημι",
        "ημιόρ",
        "ημιορ"
    ]

    if s in mezzanine_patterns or any(p in s for p in mezzanine_patterns):
        return 1

    # Numeric floors
    match = re.search(r"-?\d+", s)

    if match:
        return int(match.group())

    return None


def parse_year_built(year_str) -> int | None:
    """
    Extracts a 4-digit construction year from a string.
    Dynamically allows up to 5 years in the future for off-plan properties.
    """
    if pd.isna(year_str) or not str(year_str).strip():
        return None

    # Search for exactly 4 consecutive digits
    match = re.search(r"(\d{4})", str(year_str))
    if match:
        year = int(match.group(1))

        current_year = dt.datetime.now().year
        if 1800 <= year <= (current_year + 5):
            return year

    return None


def name_for_path(s: str) -> str:
    """String to use as filename/folder."""
    if not isinstance(s, str):
        s = str(s)
    return re.sub(r"[^\w\-. ]", "_", s)[:50].strip()


def filename_from_url(url: str) -> str | None:
    """
    Creates a hashed filename for any URL, but enforces jpg/jpeg/webp.
    Returns a short safe filename.
    """
    if not url:
        return None

    # Does not unquote beyond path
    url_clean = url.split("?")[0]
    parsed = urlparse(url_clean)

    # Decodes path for extension detection
    path = unquote(parsed.path or "")
    ext = os.path.splitext(path)[1].lower()

    if ext not in VALID_EXTS:
        ext = ".jpg"

    digest = hashlib.sha1(url.encode("utf-8")).hexdigest()[:20]
    return f"{digest}{ext}"


def is_valid_image_url(url: str) -> bool:
    """
    Accept only real listing images based on decoded URL path.
    Rejects svg/gif/etc.
    """
    if not url or not isinstance(url, str):
        return False

    try:
        parsed = urlparse(url.split("?")[0])
        decoded_path = unquote(parsed.path).lower()
        # Ensures it's a spitogatos image and has a valid extension
        return "spitogatos" in url and any(ext in decoded_path for ext in VALID_EXTS)
    except Exception:
        return False


def download_image(img_url: str, folder: Path, user_agent: str, referer_url: str) -> str | None:
    """
    Downloads image to 'target_folder' with Retry Logic.
    Returns local path or None.
    """
    try:
        if not is_valid_image_url(img_url):
            return None

        folder.mkdir(parents=True, exist_ok=True)
        fname = filename_from_url(img_url)
        if not fname:
            return None

        local_path = folder / fname

        if local_path.exists():
            return str(local_path)

        headers = {
            "User-Agent": user_agent,
            "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
            "Referer": referer_url  # bypasses hotlink protection on images
        }

        resp = None
        for attempt in range(3):
            try:
                # Uses requests with headers to look like a browser
                resp = requests.get(img_url, headers=headers, timeout=15, stream=True)

                # If success (200), exits Retry loop
                if resp.status_code == 200:
                    break

                # If image isn't found (404) or no permission (403), doesn't retry
                if resp.status_code in [403, 404]:
                    return None

                # If server error (500, 502, etc.), waits and retries
                time.sleep(random.uniform(2, 6))

            except requests.RequestException as e:
                if attempt == 2:
                    logger.warning("Image download failed | url=%s | error=%s", img_url, e)
                    return None
                time.sleep(random.uniform(2, 6))

        if not resp or resp.status_code != 200:
            status = resp.status_code if resp else "N/A"
            logger.warning("Image non-200 response | status=%s | url=%s", status, img_url)
            return None

        # Writes to disk in chunks
        with open(local_path, "wb") as fh:
            for chunk in resp.iter_content(chunk_size=8192):
                if chunk:
                    fh.write(chunk)

        # File must be > small threshold (skips tiny placeholders)
        if local_path.stat().st_size < 2048:
            local_path.unlink(missing_ok=True)
            logger.debug("Image skipped (below size threshold) | url=%s", img_url)
            return None

        return str(local_path)

    except Exception as e:
        logger.exception("Image download critical error | url=%s", img_url)
        return None


def parse_property_page(html_content: str) -> Dict[str, Any]:
    """
    Parses the HTML of a Spitogatos property detail page.
    Extracts the full description, infers the RentalDuration Enum for the Java backend,
    and extracts upscaled high-resolution images.
    """
    soup = BeautifulSoup(html_content, "html.parser")
    detail_data: Dict[str, Any] = {}

    try:
        # Extracts Full Description
        desc_container = soup.select_one(
            ".property__description, .property-description, .description-text, "
            "section.description, .property__description-text, p.description")

        if desc_container:
            more_span = desc_container.select_one(".property__description__more, .read-more")
            if more_span:
                more_span.decompose()

            detail_data["description"] = desc_container.get_text("\n", strip=True)
        else:
            detail_data["description"] = ""

        # Infers Rental Duration
        desc_lower = detail_data["description"].lower()

        short_term_keywords = ["βραχυχρόνια", "short term", "short-term",
                               "airbnb", "ημερήσια", "με τη μέρα"]
        fixed_term_keywords = ["σεζόν", "season", "ακαδημαϊκό", "φοιτητική σεζόν",
                               "ορισμένου χρόνου", "μήνες μόνο"]

        if any(keyword in desc_lower for keyword in short_term_keywords):
            detail_data["rental_duration"] = "SHORT_TERM"
        elif any(keyword in desc_lower for keyword in fixed_term_keywords):
            detail_data["rental_duration"] = "FIXED_TERM"
        else:
            detail_data["rental_duration"] = "LONG_TERM"

        # Extracts Deep Characteristics
        characteristics = {}
        for dl in soup.select("dl.property__details, dl.details, .property__features"):

            dts = dl.find_all("dt")
            dds = dl.find_all("dd")
            if dts and dds and len(dts) == len(dds):
                for dt_el, dd_el in zip(dts, dds):
                    key = dt_el.get_text(strip=True).lower()
                    val = dd_el.get_text(strip=True)
                    characteristics[key] = val
            elif dts or dds:
                logger.warning("Mismatch in <dl> row counts: %d dts vs %d dds", len(dts), len(dds))
                for row in dl.select("div, li"):
                    text = row.get_text(separator=":", strip=True)
                    if ":" in text:
                        parts = text.split(":", 1)
                        key = parts[0].strip().lower()
                        characteristics[key] = parts[1].strip()

        # Extracts quick-info metrics from property info icons
        info_items = soup.select("ul.property__info li")

        for item in info_items:
            item_id = item.get("id", "").lower()
            text = item.get_text(" ", strip=True).lower()

            number_match = re.search(r"\d+", text)
            if not number_match:
                continue

            value = int(number_match.group())

            if "floor" in item_id:
                detail_data.setdefault("floor", value)

            elif "rooms" in item_id or "bedroom" in item_id:
                detail_data.setdefault("bedrooms", value)

            elif "bath" in item_id:
                detail_data.setdefault("bathrooms", value)

        bedroom_keywords = ("υπνοδωμάτ", "υ/δ", "υδ", "δωμάτ", "bedroom", "bedrooms")
        bathroom_keywords = ("μπάν", "λουτρ", "wc", "bathroom", "bathrooms")

        for key, val in characteristics.items():
            key_norm = str(key).lower().strip()
            val_norm = str(val).lower().strip()

            if "κατασκευ" in key_norm:
                detail_data.setdefault("yearBuilt", parse_year_built(val_norm))

            elif "όροφ" in key_norm or "οροφ" in key_norm:
                detail_data.setdefault("floor", parse_floor(val_norm))

            elif "εμβαδ" in key_norm:
                size_match = re.search(r"(\d+(?:[.,]\d+)?)", val_norm)
                if size_match:
                    detail_data.setdefault("Μέγεθος", float(size_match.group(1).replace(",", ".")))

            elif any(k in key_norm for k in bedroom_keywords):
                room_match = re.search(r"\d+", val_norm)
                if room_match:
                    detail_data.setdefault("bedrooms", int(room_match.group()))

            elif any(k in key_norm for k in bathroom_keywords):
                bath_match = re.search(r"\d+", val_norm)
                if bath_match:
                    detail_data.setdefault("bathrooms", int(bath_match.group()))

        # Extracts Full-Size Images & Upscales them
        imgs: List[str] = []

        for img in soup.find_all("img"):
            src = img.get("src") or img.get("data-src")
            if src and "spitogatos" in src:
                # Cleans URL of query parameters to check extension
                src_clean = src.split("?")[0].lower()
                if any(ext in src_clean for ext in VALID_EXTS):
                    # Forces Spitogatos CDN to deliver 1200x900 high-res
                    high_res = re.sub(r'_\d+x\d+\.', '_1200x900.', src)
                    imgs.append(high_res)

        # Deduplicates while preserving order
        detail_data["images"] = list(dict.fromkeys(imgs))

    except Exception as e:
        logger.exception("Failed to parse Spitogatos detail page")

    return detail_data


def handle_cookies(driver, timeout=3) -> bool:
    """
    Returns:
        True  -> cookies accepted or banner absent
        False -> page unstable, blocked, or state uncertain
    """
    logger.debug("Cookies: checking for banner")

    try:
        body = driver.find_element(By.TAG_NAME, "body")

        if not body.text.strip() and len(body.find_elements(By.XPATH, ".//*")) < 5:
            logger.warning("Cookies: body appears empty or incomplete")
            return False

    except Exception as e:
        logger.exception("Cookies: page sanity check failed")
        return False

    # Try known cookie buttons
    cookie_xpath = (
        "//button[contains(normalize-space(), 'ΣΥΜΦΩΝΩ') "
        "or contains(normalize-space(), 'Συμφωνώ')]"
    )
    try:
        cookie_btn = WebDriverWait(driver, timeout).until(
            EC.element_to_be_clickable((By.XPATH, cookie_xpath))
        )

        try:
            cookie_btn.click()
        except Exception:
            driver.execute_script("arguments[0].click();", cookie_btn)

        logger.debug("Cookies: banner accepted via XPath")
        time.sleep(1)
        return True

    except Exception:
        pass

    # Try common CMP IDs
    known_ids = [
        "didomi-notice-agree-button",
        "onetrust-accept-btn-handler",
        "qc-cmp2-agree-button",
    ]

    for btn_id in known_ids:
        try:
            buttons = driver.find_elements(By.ID, btn_id)

            if buttons:
                try:
                    buttons[0].click()
                except Exception:
                    driver.execute_script(
                        "arguments[0].click();",
                        buttons[0]
                    )

                logger.debug("Cookies: banner accepted | button_id=%s", btn_id)
                time.sleep(1)
                return True

        except Exception:
            pass

    # No banner found, decide whether the page should be trusted
    try:

        ready_state = driver.execute_script(
            "return document.readyState"
        )

        if ready_state != "complete":
            logger.warning("Cookies: DOM not fully loaded | readyState=%s", ready_state)
            return False

        time.sleep(1.5)

        delayed_buttons = driver.find_elements(
            By.XPATH, cookie_xpath
        )

        if delayed_buttons:
            logger.debug("Cookies: banner appeared after delay")

            try:
                delayed_buttons[0].click()
            except Exception:
                driver.execute_script(
                    "arguments[0].click();",
                    delayed_buttons[0]
                )

        logger.debug("Cookies: no banner detected, proceeding")
        return True

    except Exception as e:
        logger.exception("Cookies: could not verify page state")
        return False


def load_meta(meta_path: Path) -> dict:
    if meta_path.exists():
        try:
            with open(meta_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}


def save_meta(meta_path: Path, data: dict):
    try:
        with open(meta_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    except Exception as e:
        logger.exception("Meta save error | path=%s", meta_path)


# Validation Gateway
def validate_df_with_report(df: pd.DataFrame) -> pd.DataFrame:
    """
    Applies centralized validation rules and prints a hygiene report.
    Ensures data quality before backend push.
    """
    df = df.copy()

    # Numeric normalization
    df["price"] = pd.to_numeric(df["price"], errors="coerce")
    df["Μέγεθος"] = pd.to_numeric(df["Μέγεθος"], errors="coerce")
    df["price_per_m2"] = pd.to_numeric(df["price_per_m2"], errors="coerce")

    if "floor" in df.columns:
        df["floor"] = pd.to_numeric(df["floor"],errors="coerce").astype("Int64")
    else:
        df["floor"] = pd.Series(pd.NA, index=df.index, dtype="Int64")

    if "yearBuilt" in df.columns:
        df["yearBuilt"] = pd.to_numeric(df["yearBuilt"], errors="coerce")

    if "bedrooms" in df.columns:
        df["bedrooms"] = pd.to_numeric(df["bedrooms"], errors="coerce")

    if "bathrooms" in df.columns:
        df["bathrooms"] = pd.to_numeric(df["bathrooms"], errors="coerce")

    # Image normalization
    if "images" in df.columns:
        df["images"] = df["images"].apply(ensure_list)
    else:
        df["images"] = pd.Series([[] for _ in range(len(df))], index=df.index)

    year_invalid = pd.Series(False, index=df.index)

    if "yearBuilt" in df.columns:
        year_invalid = (
                df["yearBuilt"].notna() &
                ((df["yearBuilt"] < VALIDATION_RULES["yearBuilt"]["min"]) |
                 (df["yearBuilt"] > VALIDATION_RULES["yearBuilt"]["max"]))
        )

    # Validation rules
    reasons = {
        "price_invalid": df["price"].isna() |
                         (df["price"] < VALIDATION_RULES["price"]["min"]) |
                         (df["price"] > VALIDATION_RULES["price"]["max"]),

        "size_invalid": df["Μέγεθος"].isna() |
                        (df["Μέγεθος"] < VALIDATION_RULES["size"]["min"]) |
                        (df["Μέγεθος"] > VALIDATION_RULES["size"]["max"]),

        "ppm2_invalid": df["price_per_m2"].notna() &
                        ((df["price_per_m2"] < VALIDATION_RULES["price_per_m2"]["min"]) |
                         (df["price_per_m2"] > VALIDATION_RULES["price_per_m2"]["max"])),

        "floor_invalid": df["floor"].notna() &
                         ((df["floor"] < VALIDATION_RULES["floor"]["min"]) |
                          (df["floor"] > VALIDATION_RULES["floor"]["max"])),

        "year_invalid": year_invalid,

        "bedrooms_invalid": df["bedrooms"].notna() &
                            ((df["bedrooms"] < VALIDATION_RULES["bedrooms"]["min"]) |
                             (df["bedrooms"] > VALIDATION_RULES["bedrooms"]["max"])),

        "bathrooms_invalid": df["bathrooms"].notna() &
                             ((df["bathrooms"] < VALIDATION_RULES["bathrooms"]["min"]) |
                              (df["bathrooms"] > VALIDATION_RULES["bathrooms"]["max"]))

    }

    # Logging
    logger.info("Validation report:")
    for k, v in reasons.items():
        logger.info("  %-20s %d listings flagged", k, int(v.sum()))

    valid_mask = ~pd.concat(reasons.values(), axis=1).any(axis=1)
    clean_df = df[valid_mask].copy()

    logger.info("Validation complete | valid=%d / total=%d", len(clean_df), len(df))
    return clean_df


# Stealth & Network

def is_blocked(driver):
    """
    Checks if the current page is an anti-bot challenge or block page.
    """
    try:
        title = driver.title.lower()
        if any(x in title for x in [
            "access denied", "security", "just a moment", "pardon our interruption"
        ]):
            return True

        # Checks raw page source for Datadome specific text
        page_source = driver.page_source.lower()
        if "pardon our interruption" in page_source or "super-human speed" in page_source:
            return True

    except Exception as e:
        logger.warning("Error checking block status | error=%s", e)
        return True

    return False


def safe_get(driver, url, retries=3):
    for attempt in range(retries):
        driver.get(url)
        time.sleep(random.uniform(2, 5))

        if not is_blocked(driver):
            return True

        logger.warning("Block detected | attempt=%d/%d | url=%s",
                       attempt + 1, retries, url)
        time.sleep(random.uniform(10, 20))

    return False


def rotate_session(old_driver, display=None):
    """Dynamically rotates the session."""
    logger.info("Rotating session...")
    try:
        old_driver.quit()
    except Exception as e:
        logger.exception("Error closing driver during session rotation")

    time.sleep(random.uniform(5, 10))

    try:
        fresh_options = setup_chrome_options()  # new ChromeOptions object
        new_driver = init_driver(fresh_options)
        new_agent = new_driver.execute_script("return navigator.userAgent;")
        return new_driver, new_agent
    except Exception:
        if display:
            try:
                display.stop()
            except Exception:
                pass
        raise


# Display & Stealth Configurations
def setup_display(config: ScraperConfig):
    """
    Attempts to set up a virtual X display via pyvirtualdisplay.
    Returns the display object if successful, otherwise None.
    """
    needs_virtual = False

    if config.display_mode == "virtual":
        needs_virtual = True
    elif config.display_mode == "auto":
        if platform.system() == "Linux" and "DISPLAY" not in os.environ:
            needs_virtual = True

    if needs_virtual:
        try:
            from pyvirtualdisplay import Display

            display = Display(visible=False, size=(1920, 1080))
            display.start()

            if not display.is_alive():
                raise RuntimeError("Xvfb started but is not alive.")

            logger.info("Virtual display started | mode=%s", config.display_mode)
            return display

        except ImportError:
            raise RuntimeError(
                "pyvirtualdisplay not installed. Install with: pip install pyvirtualdisplay"
            )
        except Exception as e:
            raise RuntimeError(f"Failed to start virtual display: {e}")

    logger.info("Running with native display | mode=%s", config.display_mode)
    return None


def setup_chrome_options():
    """
    Configures Chrome options with randomized User-Agents
    and stealth (headful) execution.
    """
    options = uc.ChromeOptions()
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--disable-background-timer-throttling")
    options.add_argument("--disable-backgrounding-occluded-windows")
    options.add_argument("--disable-blink-features=AutomationControlled")
    options.add_argument("--disable-notifications")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")

    return options


def cleanup_failed_payloads(directory: Path, max_age_days: int = 7, max_files: int = 50):
    """
    Cleans up the FAILED_DIR by removing files older than max_age_days,
    and then keeping only the most recent 'max_files' to prevent directory bloat.
    """
    if not directory.exists():
        return

    now = time.time()
    cutoff = now - (max_age_days * 86400)

    # Deletes files older than max_age_days
    for file in directory.glob("failed_chunk_*.json"):
        try:
            if file.stat().st_mtime < cutoff:
                file.unlink()
        except Exception as e:
            logger.exception("Failed to delete old payload | file=%s", file.name)

    # Keeps only the newest 'max_files'
    files = sorted(
        directory.glob("failed_chunk_*.json"),
        key=lambda f: f.stat().st_mtime,
        reverse=True
    )

    if len(files) > max_files:
        for file in files[max_files:]:
            try:
                file.unlink()
            except Exception as e:
                logger.exception("Failed to delete excess payload | file=%s", file.name)


# Backend data export
def push_to_backend(df: pd.DataFrame, api_url: str):
    """
    Transforms the scraped DataFrame and pushes it to the Spring Boot REST API.
    The Java backend handles mapping the raw Greek strings to Enums.
    """
    logger.info("Backend upload: preparing payload")

    payload = []
    success_count = 0
    fail_count = 0

    for _, row in df.iterrows():

        images = [img for img in ensure_list(row.get("images")) if is_valid_image_url(img)]

        dto = {
            "sourceUrl": row.get("url"),
            "title": str(row.get("title"))[:150],
            "subtitle": str(row.get("subtitle"))[:250] if pd.notna(row.get("subtitle")) else None,
            "description": str(row.get("description"))[:5000] if pd.notna(row.get("description")) else "",

            "price": safe_int(row.get("price")),
            "pricePerM2": safe_int(row.get("price_per_m2")),
            "sizeM2": safe_int(row.get("Μέγεθος")),

            "floor": safe_int(row.get("floor")),
            "yearBuilt": safe_int(row.get("yearBuilt")),
            "bedrooms": safe_int(row.get("bedrooms")),
            "bathrooms": safe_int(row.get("bathrooms")),

            "address": str(row.get("address", ""))[:255],
            "propertyType": str(row.get("Τύπος_ακινήτου", "Ακίνητο")),
            "rentalDuration": str(row.get("Διάρκεια_ενοικίασης", "LONG_TERM")),

            "images": images,
        }

        if dto["price"] is None or dto["sizeM2"] is None:
            continue

        payload.append(dto)

    if not payload:
        logger.warning("Backend upload: no valid listings to send")
        return

    logger.info("Backend upload: sending %d listings in chunks of %d",
                len(payload), CHUNK_SIZE)

    # Chunking
    for i in range(0, len(payload), CHUNK_SIZE):
        chunk = payload[i:i + CHUNK_SIZE]

        # Retry logic
        for attempt in range(1, MAX_RETRIES + 1):
            try:
                response = requests.post(
                    api_url,
                    json=chunk,
                    headers={"Content-Type": "application/json"},
                    timeout=15
                )

                if response.status_code in [200, 201]:
                    success_count += len(chunk)
                    logger.info("Chunk sent | offset=%d | size=%d", i, len(chunk))
                    break
                else:
                    logger.warning("Chunk failed | status=%d | offset=%d | size=%d",
                                   response.status_code, i, len(chunk))

            except Exception as e:
                logger.exception("Chunk request error | offset=%d", i)

            # Backoff before retry
            if attempt < MAX_RETRIES:
                sleep_time = RETRY_DELAY * attempt
                logger.info("Retrying chunk | delay=%ds | attempt=%d/%d",
                            sleep_time, attempt, MAX_RETRIES)
                time.sleep(sleep_time)
            else:
                fail_count += len(chunk)

                timestamp = dt.datetime.now().strftime("%Y%m%d_%H%M%S")
                fname = FAILED_DIR / f"failed_chunk_{timestamp}.json"

                with open(fname, "w", encoding="utf-8") as f:
                    json.dump(chunk, f, ensure_ascii=False, indent=2)

                logger.error("Chunk permanently failed | offset=%d | size=%d | saved_to=%s",
                             i, len(chunk), fname)

    logger.info("Backend upload complete | success=%d | failed=%d", success_count, fail_count)

    # Cleanup old failed payloads
    cleanup_failed_payloads(FAILED_DIR, max_age_days=7, max_files=50)


def init_driver(options):
    """
    Initializes and returns a fresh undetected_chromedriver session.
    """
    logger.info("Initializing stealth browser")

    # Looks for a system-installed chromedriver
    system_driver_path = shutil.which("chromedriver")

    if system_driver_path:
        try:
            logger.info("Using system chromedriver | path=%s", system_driver_path)
            return uc.Chrome(
                options=options,
                driver_executable_path=system_driver_path
            )
        except Exception as e:
            logger.warning("System chromedriver failed, falling back to ChromeDriverManager")
    else:
        logger.info("No system chromedriver found in PATH, falling back to ChromeDriverManager")

    # Fallback: Dynamic Download
    try:
        logger.info("Downloading chromedriver via ChromeDriverManager")
        path = ChromeDriverManager().install()

        return uc.Chrome(
            options=options,
            driver_executable_path=path
        )
    except Exception as e2:
        raise RuntimeError(f"CRITICAL: Driver initialization failed: {e2}")


# Main Scraper Pipeline
def run_scraper(config: ScraperConfig):
    """
    Executes the full web scraping pipeline for Spitogatos Athens listings.

    The function applies human-like delays and configures a Selenium WebDriver
    with custom options for stability and performance. It navigates paginated
    search result pages, collects property URLs, retrieves detailed listing
    information (including attributes and images), and stores the results in
    structured CSV outputs (latest snapshot, historical records, and derived
    metrics).

    Parameters
    ----------
    config : ScraperConfig
        Configuration object controlling scraper behavior:

        max_pages : int
            Maximum number of result pages to scrape.

        full : bool
            If True, enables image downloading and local storage.
            If False, skips image downloading.

        mode : str
            Defines the execution and storage strategy:

            "normal" (default)
                Scrapes listings and appends results to the default
                historical dataset.

            "weekly"
                Scrapes listings and appends results to a weekly
                historical dataset.

        display_mode : str
            Controls display environment handling:

            "auto"
                Automatically enables virtual display on headless Linux.

            "virtual"
                Forces use of a virtual display (Xvfb).

            "native"
                Uses the system’s native display.

    Returns
    -------
    None
        Writes processed listings, images, and metrics to disk.
        Optionally pushes cleaned data to backend service.
    """
    # Extracts params
    max_pages = config.max_pages
    image_download = config.full
    mode = config.mode

    # Initializes Display & Options
    try:
        display = setup_display(config)
    except RuntimeError as e:
        logger.critical("Display setup failed, aborting | error=%s", e)
        return

    options = setup_chrome_options()

    try:
        driver = init_driver(options)
        dynamic_user_agent = driver.execute_script("return navigator.userAgent;")
    except Exception as e:
        logger.exception("Driver initialization failed")
        if display:
            display.stop()
        return

    wait = WebDriverWait(driver, WAIT_SECONDS)
    logger.info("WebDriver initialized")

    try:
        # Loops through result pages for Spitogatos Athens
        base_search_url = "https://www.spitogatos.gr/enoikiaseis-katoikies/athina-kentro/selida_{}"
        
        # State Management
        state_file = DATA_DIR / "scraper_state.json"
        state = {}
        if state_file.exists():
            try:
                with open(state_file, "r", encoding="utf-8") as f:
                    state = json.load(f)
                logger.info(
                    "Resuming from saved state. Phase 1 complete: %s",
                    state.get("phase_1_complete", False)
                )
            except Exception as e:
                logger.warning("Failed to load state file: %s", e)

        start_page = state.get("last_page", 0) + 1
        phase_1_complete = state.get("phase_1_complete", False)
        all_property_data = state.get("all_property_data", [])
        all_property_urls = {d["url"] for d in all_property_data if "url" in d}
        last_detail_index = state.get("last_detail_index", -1)

        cookies_accepted = False
        fresh_session = True

        # STEP 1: Parse Search Pages
        if phase_1_complete:
            logger.info(
                "Phase 1 skipped (already complete in saved state) | unique_listings=%d",
                len(all_property_data)
            )
            start_page = max_pages + 1  # Skip loop
        else:
            logger.info("Phase 1: scraping search pages | max_pages=%d", max_pages)

        for page_num in range(start_page, max_pages + 1):
            url = base_search_url.format(page_num)
            logger.info("Scraping search page | page=%d/%d | url=%s", page_num, max_pages, url)

            try:
                if not safe_get(driver, url):
                    logger.warning(
                        "Hard block on search page | page=%d | rotating session", page_num)
                    driver, dynamic_user_agent = rotate_session(driver, display)

                    cookies_accepted = False
                    fresh_session = True
                    wait = WebDriverWait(driver, WAIT_SECONDS)
                    if not safe_get(driver, url, retries=1):
                        logger.error("Block persists after session rotation. Aborting Phase 1.")
                        break

                # Handles cookie pop-up
                if not cookies_accepted:
                    accepted = handle_cookies(
                        driver,
                        timeout=8 if fresh_session else 3
                    )

                    cookies_accepted = accepted
                    fresh_session = False

                try:
                    wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, "article.ordered-element")))
                except Exception:
                    logger.warning("Timeout waiting for listings | page=%d | url=%s", page_num, url)
                    break

                # Scrolls down slowly to trigger lazy loading
                scroll_height = random.uniform(0.4, 0.8)
                driver.execute_script(f"window.scrollTo(0, document.body.scrollHeight * {scroll_height});")
                time.sleep(random.uniform(2, 6))
                driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
                time.sleep(random.uniform(2, 6))

                soup = BeautifulSoup(driver.page_source, "html.parser")
                articles = soup.find_all("article", class_="ordered-element")

                if len(articles) < 5:
                    logger.warning("Suspiciously low article count (%d) on page %d - likely blocked", len(articles), page_num)
                    break

                if not articles:
                    logger.warning("No listings found on page | page=%d | url=%s", page_num, url)
                    break

                for article in articles:
                    try:
                        # Extracts URL
                        link_tag = article.select_one("a.tile__link")
                        listing_url = urljoin("https://www.spitogatos.gr",
                                              link_tag["href"]) if link_tag and "href" in link_tag.attrs else None
                        if not listing_url or listing_url in all_property_urls:
                            continue
                        all_property_urls.add(listing_url)

                        title_tag = article.select_one("h3.tile__title")
                        title = title_tag.get_text(strip=True) if title_tag else None

                        size_m2, prop_type = None, None
                        if title:
                            parts = title.split(",")
                            prop_type = parts[0].strip() if len(parts) > 0 else "Ακίνητο"
                            size_match = re.search(r"(\d+(?:[.,]\d+)?)\s*τ\.μ\.", title or "")
                            if size_match:
                                size_m2 = float(size_match.group(1).replace(",", "."))

                        # Location
                        loc_tag = article.select_one("h3.tile__location")
                        location = loc_tag.get_text(strip=True) if loc_tag else ""

                        # Description Preview
                        desc_tag = article.select_one("p.tile__description")
                        description_preview = desc_tag.get_text(strip=True) if desc_tag else None

                        # Price
                        price_tag = article.select_one(".price__text")
                        price_val = parse_money_to_float(price_tag.get_text()) if price_tag else None

                        # Calculate PPM2
                        ppm2 = round(price_val / size_m2, 2) if price_val and size_m2 else None
                        if ppm2 and (ppm2 < PPM2_MIN or ppm2 > PPM2_MAX):
                            ppm2 = None

                        # Attributes
                        attributes = {}
                        for li in article.select("ul.tile__info li"):
                            key = li.get("title")
                            val = li.get_text(strip=True)
                            if key and val:
                                attributes[key] = val

                        all_property_data.append({
                            "url": listing_url,
                            "title": title,
                            "subtitle": location,
                            "address": location,
                            "price": price_val,
                            "Μέγεθος": size_m2,
                            "price_per_m2": ppm2,
                            "Τύπος_ακινήτου": prop_type,
                            "floor": parse_floor(attributes.get("Όροφος")),
                            "yearBuilt": parse_year_built(
                                attributes.get("Έτος κατασκευής") or attributes.get("Έτος Κατασκευής")),
                            "bedrooms": safe_int(attributes.get("Υπνοδωμάτια")),
                            "bathrooms": safe_int(attributes.get("Μπάνια")),
                            "rental_duration": "LONG_TERM",
                            "date_scraped": dt.datetime.today().strftime("%Y-%m-%d"),
                            "description": description_preview,
                            "images": []
                        })
                    except Exception:
                        pass

                if page_num % 20 == 0:
                    logger.info("Deep sleep for IP cooldown | page=%d | duration=5-10 min", page_num)
                    time.sleep(random.uniform(300, 600))  # 5 to 10 minutes
                    driver, dynamic_user_agent = rotate_session(driver, display)
                    cookies_accepted = False
                    fresh_session = True
                    wait = WebDriverWait(driver, WAIT_SECONDS)
                elif page_num % 5 == 0:
                    logger.info("Long break between pages | page=%d | duration=30-50s", page_num)
                    time.sleep(random.uniform(30, 50))

                # Save state after each page
                state["last_page"] = page_num
                state["all_property_data"] = all_property_data
                if page_num % 10 == 0:
                    try:
                        temp_file = state_file.with_suffix(".tmp")
                        with open(temp_file, "w", encoding="utf-8") as f:
                            json.dump(state, f, ensure_ascii=False)
                        temp_file.replace(state_file)
                    except Exception as e:
                        logger.warning("Failed to save state: %s", e)

            except Exception:
                logger.exception("Error scraping search page | page=%d | url=%s", page_num, url)
                break
        else:
            # this block runs only if the loop completed without break
            if not phase_1_complete:
                state["phase_1_complete"] = True
                try:
                    with open(state_file, "w", encoding="utf-8") as f:
                        json.dump(state, f, ensure_ascii=False)
                except Exception:
                    pass
                logger.info("Phase 1 complete | unique_listings=%d", len(all_property_data))

        # STEP 2: Enrich with Detail Pages
        if all_property_data:
            logger.info("Phase 2: detail scraping | total=%d", len(all_property_data))

            resume_index = last_detail_index + 1
            details_processed = 0

            for i, data in enumerate(all_property_data):
                if i < resume_index:
                    continue

                if details_processed > 0 and details_processed % 100 == 0:
                    logger.info("Deep sleep for IP cooldown | listing=%d | duration=5-10 min", i)
                    time.sleep(random.uniform(300, 600))  # 5 to 10 minutes
                    driver, dynamic_user_agent = rotate_session(driver, display)

                    cookies_accepted = False
                    fresh_session = True

                url = data["url"]

                if not safe_get(driver, url, retries=2):
                    logger.warning("Block on detail page | listing=%d/%d | url=%s | rotating session",
                                   i + 1, len(all_property_data), url)
                    driver, dynamic_user_agent = rotate_session(driver, display)

                    cookies_accepted = False
                    fresh_session = True

                    if not safe_get(driver, url, retries=1):
                        logger.error("Persistent block after rotation | skipping | url=%s", url)
                        continue

                logger.info("Scraping detail page | listing=%d/%d | url=%s",
                            i + 1, len(all_property_data), url)

                # Handles cookie pop-up
                if not cookies_accepted:
                    accepted = handle_cookies(
                        driver,
                        timeout=8 if fresh_session else 3
                    )

                    cookies_accepted = accepted
                    fresh_session = False

                try:
                    more_btn = driver.find_element(By.CSS_SELECTOR, ".property__description__more, .read-more")
                    driver.execute_script("arguments[0].click();", more_btn)
                    time.sleep(0.5)
                except Exception:
                    pass

                # Parses the page
                detail_html = driver.page_source
                details = parse_property_page(detail_html)
                data.update(details)

                # Save state after each detail page
                state["last_detail_index"] = i
                state["all_property_data"] = all_property_data
                if details_processed % 10 == 0:
                    try:
                        temp_file = state_file.with_suffix(".tmp")
                        with open(temp_file, "w", encoding="utf-8") as f:
                            json.dump(state, f, ensure_ascii=False)
                        temp_file.replace(state_file)
                    except Exception:
                        pass

                details_processed += 1

        if not all_property_data:
            logger.critical("No data extracted. Exiting.")
            return

        df_new = pd.DataFrame(all_property_data)

        # Validation
        df_validated = validate_df_with_report(df_new)

        if df_validated.empty:
            logger.critical("No listings passed validation. Exiting to protect dataset.")
            return

        # Merges with History
        df_old = pd.read_csv(LATEST_CSV) if LATEST_CSV.exists() else pd.DataFrame()

        ideal_cols_order = [
            'date_scraped', 'url', 'title', 'price', 'address', 'Μέγεθος', 'price_per_m2',
            'floor', 'yearBuilt', 'bedrooms', 'bathrooms',
            'Τύπος_ακινήτου', 'Διάρκεια_ενοικίασης',
            'subtitle', 'description', 'images', 'rental_duration'
        ]

        # Creates a new list with the ideal cols that exist
        ordered_cols = [col for col in ideal_cols_order if col in df_validated.columns]

        # Adds all other remaining columns that weren't in the ideal list
        other_cols = [col for col in df_validated.columns if col not in ordered_cols]

        # Sets the DataFrame's column order
        df_validated = df_validated[ordered_cols + other_cols]

        if not df_old.empty:
            df = (pd.concat([df_old, df_validated])
                  .drop_duplicates(subset=["url"], keep="last").reset_index(drop=True))
        else:
            df = df_validated.reset_index(drop=True)

        logger.info("Initializing metadata synchronization for image records")
        IMAGES_DIR.mkdir(parents=True, exist_ok=True)
        today_str = dt.datetime.today().strftime("%Y-%m-%d")
        current_ids = set()

        for idx, row in df.iterrows():
            if pd.isna(row.get('url')):
                continue
            lid = name_for_path(row['url'].split("/")[-1])
            current_ids.add(lid)
            meta_path = IMAGES_DIR / lid / "meta.json"
            meta_path.parent.mkdir(parents=True, exist_ok=True)
            meta = load_meta(meta_path)

            if not meta:
                meta = {
                    "url": row['url'],
                    "first_seen": today_str
                }

            meta["last_seen"] = today_str

            save_meta(meta_path, meta)

        # Downloads images per listing and creates images table rows
        if image_download:
            logger.info("Image download pipeline starting | total_listings=%d | max_per_listing=%d",
                        len(df), MAX_IMAGES)
            for idx, row in df.iterrows():

                listing_id = name_for_path(row['url'].split("/")[-1])
                listing_folder = IMAGES_DIR / listing_id

                existing_files = [
                    p for p in listing_folder.glob("*")
                    if p.is_file() and p.name != "meta.json"
                ]
                downloaded_paths = [str(p) for p in existing_files]
                current_count = len(existing_files)

                if current_count < MAX_IMAGES:
                    logger.debug("Checking images | listing_id=%s | existing=%d/%d",
                                 listing_id, current_count, MAX_IMAGES)

                    for img_url in ensure_list(row.get("images", [])):
                        if current_count >= MAX_IMAGES:
                            break
                        fname = filename_from_url(img_url)
                        if not fname or (listing_folder / fname).exists():
                            continue

                        # Small delay between individual images
                        time.sleep(random.uniform(0.5, 1.5))

                        local = download_image(
                            img_url,
                            listing_folder,
                            dynamic_user_agent,
                            row['url']
                        )
                        if local:
                            downloaded_paths.append(local)  # path string
                            current_count += 1

                time.sleep(random.uniform(1, 2))

                # Joins all local paths into a pipe-separated string
                df.at[idx, "image_paths_local"] = "|".join(downloaded_paths) if downloaded_paths else None

        # Garbage Collection Pipeline
        today = dt.datetime.today()
        logger.info("Running image garbage collection")

        for folder in IMAGES_DIR.iterdir():
            if folder.is_dir() and folder.name not in current_ids:
                meta_path = folder / "meta.json"
                meta = load_meta(meta_path)

                if "last_seen" in meta:
                    try:
                        last_seen_date = dt.datetime.strptime(meta["last_seen"], "%Y-%m-%d")
                        days_missing = (today - last_seen_date).days

                        if days_missing >= GRACE_DAYS:
                            shutil.rmtree(folder)
                            logger.info("Garbage collection: deleted stale listing | folder=%s | days_missing=%d",
                                        folder.name, days_missing)

                    except Exception:
                        logger.exception("Garbage collection error | folder=%s", folder.name)

        # Saves to CSV
        # Uses utf-8-sig for Excel to correctly read Greek characters
        df.to_csv(LATEST_CSV, index=False, encoding='utf-8-sig')
        logger.info("Saved listings to CSV | count=%d | path=%s", len(df), LATEST_CSV)

        # History aggregation (Weighted Average)
        if mode == "weekly":
            hist_path = DATA_DIR / "spitogatos_history_weekly.csv"
        else:
            hist_path = HISTORY_AGG_CSV

        hist_path.parent.mkdir(exist_ok=True)

        avg_price = None
        clean_sample_size = 0
        # Calculation of average price (area-weighted mean)
        if "price_per_m2" in df.columns and "Μέγεθος" in df.columns:

            df_calc = df_validated.copy()
            df_calc["price_per_m2"] = pd.to_numeric(df_calc["price_per_m2"], errors="coerce")
            df_calc["area_numeric"] = pd.to_numeric(df_calc["Μέγεθος"], errors="coerce")

            # Deep Bounds Filtering for Statistical Accuracy
            df_calc = df_calc.dropna(subset=["price_per_m2", "area_numeric"])
            df_calc = df_calc[
                (df_calc["area_numeric"] >= 30) &
                (df_calc["area_numeric"] <= 250) &
                (df_calc["price_per_m2"] >= PPM2_MIN) &
                (df_calc["price_per_m2"] <= PPM2_MAX)
                ]

            # Effective sample size
            clean_sample_size = len(df_calc)

            # Computes weighted mean on cleaned data
            if clean_sample_size > 0:
                weighted_sum = (df_calc["price_per_m2"] * df_calc["area_numeric"]).sum()
                total_area = df_calc["area_numeric"].sum()

                if total_area > 0:
                    avg_price = weighted_sum / total_area

        record = {
            "date": today_str,
            "city": "Athens",
            "price_per_m2": round(avg_price, 2) if avg_price else None,
            "sample_size": clean_sample_size
        }

        # Appends or Creates
        if hist_path.exists():
            hist_df = pd.read_csv(hist_path)
            updated_hist = pd.concat([hist_df, pd.DataFrame([record])], ignore_index=True)
            updated_hist.to_csv(hist_path, index=False)
        else:
            pd.DataFrame([record]).to_csv(hist_path, index=False)

        logger.info("Updated history | path=%s | date=%s | price_per_m2=%s | sample_size=%d",
                    hist_path, record["date"],
                    f"{record['price_per_m2']:.2f}" if record["price_per_m2"] else "N/A",
                    record["sample_size"])

        if config.push_backend:
            push_to_backend(
                df_validated,
                api_url=config.backend_url
            )
        else:
            logger.info("Backend upload skipped (--push-backend not set)")

        # Clear state file on successful completion
        if state_file.exists():
            try:
                state_file.unlink()
                logger.info("Cleared state file after successful run.")
            except Exception as e:
                logger.warning("Could not clear state file: %s", e)

    finally:
        # Cleanup
        logger.info("Shutting down driver and virtual display")
        if driver:
            try:
                driver.quit()
            except Exception as e:
                logger.exception("Error while quitting driver")

        if display:
            try:
                display.stop()
            except Exception as e:
                logger.exception("Error while stopping virtual display")


# CLI & ENTRY POINT

# CLI Builder
def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="dras-scraper",
        description="DRAS - Dynamic Rent Adjustment System "
                    "(Spitogatos Athens Data Collection Pipeline)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent(
            """
            Examples:
                1. python spitogatos_scraper.py --mode normal --max-pages 3 --push-backend
                2. python spitogatos_scraper.py --full --display-mode virtual
                3. python spitogatos_scraper.py --max-pages 5 --push-backend
            
            Notes:
                - 'auto' display mode detects headless environments
                - '--full' enables image downloading
                - default backend url: http://localhost:8080/api/external-import/listings
            """
        )
    )

    parser.add_argument(
        "--version",
        action="version",
        version=f"DRAS Scraper {__version__}"
    )

    # Execution Group
    exec_group = parser.add_argument_group("Execution")
    exec_group.add_argument(
        "--mode",
        choices=["normal", "weekly"],
        default="normal",
        help="Scraping mode"
    )
    exec_group.add_argument(
        "--max-pages",
        type=int,
        default=1,
        help="Maximum pages to scrape"
    )

    # Display Group
    display_group = parser.add_argument_group("Display")
    display_group.add_argument(
        "--display-mode",
        choices=["auto", "virtual", "native"],
        default="auto",
        help="Xvfb display strategy"
    )

    # Output Group
    output_group = parser.add_argument_group("Output")
    output_group.add_argument(
        "--full",
        action="store_true",
        help="Enable image downloading pipeline"
    )
    output_group.add_argument(
        "--push-backend",
        action="store_true",
        help="Push scraped listings to backend API"
    )
    output_group.add_argument(
        "--backend-url",
        default="http://localhost:8080/api/external-import/listings",
        help="Backend endpoint URL"
    )

    return parser


# Parser -> Config
def parse_args() -> ScraperConfig:
    parser = build_parser()
    args = parser.parse_args()

    return ScraperConfig(
        full=args.full,
        mode=args.mode,
        max_pages=args.max_pages,
        display_mode=args.display_mode,
        push_backend=args.push_backend,
        backend_url=args.backend_url,
    )


# Main Entry Point
def main():
    setup_logging(debug=True)

    start_time = dt.datetime.now()
    try:
        config = parse_args()

        logger.info(
            "DRAS start | mode=%s | pages=%d | display=%s | images=%s | push_backend=%s",
            config.mode,
            config.max_pages,
            config.display_mode,
            "ON" if config.full else "OFF",
            "ON" if config.push_backend else "OFF",
        )

        run_scraper(config)
    finally:
        end_time = dt.datetime.now()
        duration = end_time - start_time
        total_seconds = duration.total_seconds()
        
        # Calculate tracked sleep
        time_spent_sleeping = _total_sleep_time
        true_runtime = max(0.0, total_seconds - time_spent_sleeping)

        logger.info("DRAS end | finished_at=%s | runtime=%s | time_spent_sleeping=%s | true_runtime=%s",
                    end_time.strftime("%Y-%m-%d %H:%M:%S"),
                    str(dt.timedelta(seconds=int(total_seconds))),
                    str(dt.timedelta(seconds=int(time_spent_sleeping))),
                    str(dt.timedelta(seconds=int(true_runtime))))


if __name__ == "__main__":
    main()
