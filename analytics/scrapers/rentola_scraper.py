"""
Dynamic Rent Adjustment System (DRAS) – Rentola Web Scraper
-----------------------------------------------------------
This script automates the collection of residential rental listings from the
Rentola (rentola.gr) website for the Athens area. It navigates paginated search
results, handles cookie consent prompts, extracts structured and semi-structured
listing attributes including price, address, description, and all available
property characteristics (e.g. size, rooms, property type, and rental duration),
as well as derived metrics such as price per square meter and optional image
data. All results are stored in CSV format for subsequent statistical analysis.

The scraper is designed for research and educational use and relies on the
current HTML structure of Rentola. Changes to the website may require updates
to CSS selectors.

Outputs:
- data/rentola_athens_listings.csv (latest snapshot)
- data/rentola_history.csv (historical price metrics)
- data/images/<listing_slug>/ (optional image downloads)

Dependencies:
selenium, webdriver-manager, beautifulsoup4, pandas, requests

Usage:
python rentola_scraper.py
    -> Executes in fast mode (default mode scraping only).

python rentola_scraper.py --full
    -> Enables full mode with image downloading and local storage.

python rentola_scraper.py --mode weekly
    -> Stores aggregated results in the weekly historical dataset.

python rentola_scraper.py --mode weekly --full
    -> Full execution mode with image downloads and weekly aggregation.

Author: Athos Exarchou
Date: 24.10.2025

Disclaimer:
This script is intended for research and educational purposes only. Users are
responsible for complying with Rentola’s Terms of Service and applicable laws.
"""

# Imported Libraries
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
from bs4 import BeautifulSoup
from pathlib import Path
from typing import List, Dict, Any
from webdriver_manager.chrome import ChromeDriverManager
from urllib.parse import unquote, urlparse
import pandas as pd
import time
import re
import requests
import datetime as dt
import random
import shutil
import urllib
import hashlib
import json
import os
import argparse

# CONFIG
DATA_DIR = Path("data")
DATA_DIR.mkdir(exist_ok=True)
LATEST_CSV = DATA_DIR / "rentola_athens_listings.csv"
HISTORY_AGG_CSV = DATA_DIR / "rentola_history.csv"
IMAGES_DIR = DATA_DIR / "images"  # Images stored per-listing in subfolders
VALID_EXTS = {".jpg", ".jpeg", ".webp"}

# Runtime
MAX_PAGES = 20  # number of pages scraped
WAIT_SECONDS = 10
MAX_IMAGES = 5  # max number of images downloaded per listing


# Helper functions
def parse_numeric(x):
    """Numeric parsing for strings such as '1.234,56 €' or '950'."""
    if pd.isna(x):
        return None
    s = str(x).strip()
    if s == "":
        return None
    # Removes euro and non-digit except ".", and ","
    s = re.sub(r"[^\d.,\-]", "", s)
    # If both "." and "," exist and "." appears before comma, treats "." as thousand seperator
    if "." in s and "," in s:
        # Converts thousand separators
        if s.rfind(".") < s.rfind(","):
            s = s.replace(".", "")
            s = s.replace(",", ".")
    else:
        # If only "," is present and no ".", then comma is decimal
        if "," in s and "." not in s:
            s = s.replace(",", ".")
    try:
        return float(s)
    except Exception:
        return None


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


def name_for_path(s: str) -> str:
    """String to use as filename/folder (keeps it readable)."""
    if not isinstance(s, str):
        s = str(s)
    return re.sub(r"[^\w\-. ]", "_", s)[:100].strip()


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
    Works with Rentola CDN wrapper URLs.
    Rejects svg/png/gif/etc.
    """
    if not url or not isinstance(url, str):
        return False

    try:
        parsed = urlparse(url.split("?")[0])
        decoded_path = unquote(parsed.path).lower()

        return any(ext in decoded_path for ext in VALID_EXTS)
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
                    print(f"    [Image Error] {resp.status_code} for {img_url} - Aborting.")
                    return None

                # If server error (500, 502, etc.), waits and retries
                time.sleep(2)

            except requests.RequestException as e:
                if attempt == 2:
                    print(f"    [Image Failed after 3 tries] {e} for {img_url}")
                    return None
                time.sleep(2)

        if not resp or resp.status_code != 200:
            print(f"    [Image] non-200 {resp.status_code} for {img_url}")
            return None

        # Writes to disk in chunks
        with open(local_path, "wb") as fh:
            for chunk in resp.iter_content(chunk_size=8192):
                if chunk:
                    fh.write(chunk)

        # File must be > small threshold (skips tiny placeholders)
        if local_path.stat().st_size < 2000:
            local_path.unlink(missing_ok=True)
            print(f"    [Image] Skipped small content.")
            return None

        return str(local_path)

    except Exception as e:
        print(f"    [Image download critical error] {e} for {img_url}")
        return None


# Detail page parser
def parse_property_page(html_content):
    """
    Parses the HTML of a property's detail page.
    Extracts title, subtitle, address, description, price, Greek-labelled details, images.
    """
    soup = BeautifulSoup(html_content, "html.parser")
    property_data: Dict[str, Any] = {}

    try:
        # 1. Title
        title = soup.select_one("h1.mt-4.text-xl.font-bold")
        property_data["title"] = title.get_text(strip=True) if title else None

        # 2. Subtitle
        subtitle = (
                soup.select_one("article h2.font-bold")
                or soup.select_one("div.listing h2.font-bold")
                or soup.select_one("h2.mt-6.font-bold")
        )
        if subtitle:
            property_data["subtitle"] = subtitle.get_text(strip=True)
        else:
            property_data["subtitle"] = None

        # 3. Address
        addr = soup.select_one('a[href="#propertyMap"] p')
        property_data["address"] = addr.get_text(strip=True) if addr else None

        # 4. Description
        desc = soup.select_one(".mt-4 p.line-clamp-5")
        if not desc:
            # fallback to any visible description
            desc = soup.select_one("div.mt-4 > p")
        property_data["description"] = desc.get_text(strip=True) if desc else None

        # 5. Price
        price_element = soup.select_one("p.text-\\[32px\\].font-bold")
        raw_price_text = None

        if price_element:
            # Joins text parts to handle potential nested spans
            raw_price_text = " ".join(price_element.stripped_strings)
        else:
            # Fallback for mobile view
            mob_price = soup.select_one("p.mb-4.font-bold.md\\:text-lg")
            if mob_price:
                raw_price_text = " ".join(price_element.stripped_strings)

        property_data["price"] = parse_money_to_float(raw_price_text)

        # 6. Details section (Greek labels such as 'Μέγεθος', 'Δωμάτια', etc.)
        details = soup.select("div.flex.justify-between.border-b")

        for row in details:
            cols = row.find_all("p")
            if len(cols) == 2:
                key = cols[0].get_text(strip=True)
                val = cols[1].get_text(strip=True)

                # If the key is "Τιμή", doesn't add it to property_data
                if key == "Τιμή":
                    continue

                clean_key = key.replace(" ", "_").replace(":", "")
                property_data[clean_key] = val

        # 7. Computes price per square meter
        try:
            size_val = parse_money_to_float(property_data.get("Μέγεθος"))
            price_val = property_data.get("price")

            if price_val and size_val and size_val > 0:
                property_data["price_per_m2"] = round(price_val / size_val, 2)
            else:
                property_data["price_per_m2"] = None

            # Filter for absurd €/m_2 values
            ppm2 = property_data["price_per_m2"]
            if ppm2 and (ppm2 < 2 or ppm2 > 80):
                property_data["price_per_m2"] = None

        except Exception as e:
            print(f"    [Error calculating price_per_m2: {e}]")

        # 8. Image list
        try:
            img_urls: List[str] = []
            for img in soup.select("img"):
                src = (
                        img.get("src")
                        or img.get("data-src")
                        or img.get("data-lazy-src")
                )
                if not src:
                    continue

                src = src.strip()

                # Normalizes protocol-relative URLs
                if src.startswith("//"):
                    src = "https:" + src

                # Normalizes relative URLs
                if src.startswith("/"):
                    url = "https://rentola.gr"
                    src = urllib.parse.urljoin(url, src)

                # Only keeps non-empty absolute links or relative paths
                if src:
                    img_urls.append(src)

            # Deduplicates while preserving order
            seen = set()
            imgs_filtered = []
            for u in img_urls:
                if u not in seen:
                    imgs_filtered.append(u)
                    seen.add(u)

            property_data["images"] = imgs_filtered

        except Exception as ie:
            property_data["images"] = []
            print(f"    [Image parse error] {ie}")

    except Exception as e:
        print(f"    [Error parsing detail page: {e}]")

    return property_data


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
        print(f"    [Meta Save Error] {e}")


# Selenium scraping loop
def run_scraper(MAX_PAGES, image_download=False, mode="normal"):
    """
    Executes the full web scraping pipeline for Rentola Athens listings.

    The function applies human-like delays and configures a Selenium WebDriver
    with custom options for stability and performance. It navigates paginated
    search result pages, collects property URLs, retrieves detailed listing
    information (including attributes and images), and stores the results in
    structured CSV outputs (latest snapshot, historical records, and derived
    metrics).

    Parameters
    ----------
    MAX_PAGES : int
        Maximum number of result pages to scrape.
    image_download : bool, optional
        If True, downloads listing images and tracks them locally.
        If False, image downloading is skipped (default is False).
    mode : str, optional
        Defines the execution and storage strategy of the scraper.

        "normal" (default)
            Scrapes listings and appends results to the default
            historical dataset for time-series analysis.

        "weekly"
            Scrapes listings and appends results to the weekly
            historical dataset for time-series analysis.

        "full"
            Equivalent to "normal" mode but with image downloading enabled.
            (Typically controlled via image_download=True.)

        Default is "normal".

    Returns
    -------
    None
        Outputs are written to CSV files and image directories on disk.
    """
    # 1. Setup Selenium
    chrome_options = Options()
    chrome_options.add_argument("--headless=new")
    chrome_options.add_argument("--window-size=1920,1080")
    chrome_options.add_argument("--disable-blink-features=AutomationControlled")
    chrome_options.add_argument("--disable-notifications")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")

    print("Initializing the WebDriver...")
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=chrome_options)

    # Hides "navigator.webdriver"
    driver.execute_script(
        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
    )

    wait = WebDriverWait(driver, WAIT_SECONDS)
    dynamic_user_agent = driver.execute_script("return navigator.userAgent;")
    print("WebDriver initialized.")

    # 2. Loop through result pages for Rentola Athens
    base_search_url = "https://rentola.gr/pros-enoikiasi/athina?page={}"
    print(f"Opening {driver.current_url}")
    all_property_urls: List[str] = []
    cookies_accepted = False

    # 3. Load listings
    for page_num in range(1, MAX_PAGES + 1):
        url = base_search_url.format(page_num)
        print(f"\nScraping page [{page_num}/{MAX_PAGES}]: {url}")
        driver.get(url)

        time.sleep(random.uniform(2, 5))  # delay

        # 4. Handle cookie pop-up
        if not cookies_accepted:
            print("Checking for cookie banner...")
            try:
                time.sleep(2)
                # Tries known ID
                try:
                    cookie_btn = wait.until(EC.element_to_be_clickable(
                        (By.ID, "CybotCookiebotDialogBodyButtonAccept")
                    ))
                    cookie_btn.click()
                    cookie_dismissed = True
                    print("Cookie banner dismissed via ID.")
                except:
                    # Tries visible text
                    possible_texts = ["Αποδοχή όλων", "Accept all"]
                    buttons = driver.find_elements(By.TAG_NAME, "button")
                    cookie_dismissed = False
                    for btn in buttons:
                        try:
                            if any(text.lower() in btn.text.lower() for text in possible_texts):
                                btn.click()
                                cookie_dismissed = True
                                print("Cookie banner dismissed via text.")
                                break
                        except Exception:
                            continue
                    if not cookie_dismissed:
                        # Tries iframes
                        try:
                            iframes = driver.find_elements(By.TAG_NAME, "iframe")
                            for iframe in iframes:
                                driver.switch_to.frame(iframe)
                                inner_buttons = driver.find_elements(By.TAG_NAME, "button")

                                for btn in inner_buttons:
                                    try:
                                        if any(text.lower() in (btn.text or "").lower() for text in possible_texts):
                                            btn.click()
                                            cookie_dismissed = True
                                            print("Cookie banner dismissed in iframe via text.")
                                            break
                                    except Exception:
                                        continue

                                driver.switch_to.default_content()

                                if cookie_dismissed:
                                    break

                        except Exception:
                            driver.switch_to.default_content()

                if cookie_dismissed:
                    time.sleep(1.5)
                    cookies_accepted = True
                    print("Cookie banner handling complete.")
                else:
                    print("No cookie banner found or already dismissed.")

            except Exception as e:
                print(f"️ Cookie handling failed or not needed: {e}")

        # Waits for listings or break if none found
        try:
            wait.until(EC.presence_of_all_elements_located(
                (By.CSS_SELECTOR, "div[data-testid='propertyTile']")))
        except:
            print("No listings found, reached last page or timed out.")
            break

        time.sleep(random.uniform(1, 3))

        # Scrolls to load listings
        print("Scrolling to load all listings...")
        scroll_pause = 1.5  # pause time for JS to load
        last_height = driver.execute_script("return document.body.scrollHeight")
        max_scrolls = 20
        unchanged_count = 0

        for i in range(max_scrolls):
            driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
            time.sleep(scroll_pause)
            new_height = driver.execute_script("return document.body.scrollHeight")
            print(f"Scroll {i + 1}: Old={last_height}, New(after scroll)={new_height}")

            if new_height == last_height:
                unchanged_count += 1
                if unchanged_count >= 2:  # in case of lazy load or delayed JS
                    print("No new content detected, stopping...")
                    break
            else:
                unchanged_count = 0
                last_height = new_height

        print("Scrolling complete.")

        # 5. STEP 1: Parse Search Page for Links
        print("Starting Step 1: Parsing search results page to find links...")
        soup = BeautifulSoup(driver.page_source, "html.parser")

        # Uses the data-testid selector from the page HTML
        listings = soup.select("div[data-testid='propertyTile']")
        print(f"Found {len(listings)} listings on page {page_num}.")

        if not listings:
            print("No more listings, stopping pagination...")
            break

        base_url = "https://rentola.gr"
        for item in listings:
            link_tag = item.select_one("a.absolute.inset-0.z-1")
            if link_tag and link_tag.has_attr('href'):
                rel = link_tag['href']
                if rel.startswith('/listings/'):
                    full_url = base_url + rel
                    if full_url not in all_property_urls:
                        all_property_urls.append(full_url)

        # Long break every 10 pages
        if page_num % 10 == 0:
            print("Taking a short break...")
            time.sleep(random.uniform(30, 60))
        else:
            print("Resting before next page...")
            time.sleep(random.uniform(3, 7))

    print(f"\nTotal collected property URLs: {len(all_property_urls)}")

    # 6. STEP 2: Visit Each Link and Scrape Details
    all_property_data = []

    if not all_property_urls:
        print("\tNo URLs found. Check selectors for Step 1.")
    else:
        print("\nStarting Step 2: Processing listings...")
        for i, url in enumerate(all_property_urls):
            print(f"Scraping [{i + 1}/{len(all_property_urls)}]: {url}")

            # Every 10 listings, pauses for 20-50 seconds
            if i > 0 and i % 10 == 0:
                print("Taking a short break...")
                time.sleep(random.uniform(20, 50))

            try:
                driver.get(url)
                time.sleep(random.uniform(2, 6))

                # Waits for a key element from the detail page to be present
                wait.until(EC.presence_of_element_located(
                    (By.XPATH, "//h4[contains(text(), 'Λεπτομέρειες')]")
                ))
                time.sleep(random.uniform(0.5, 1.5))  # pause for JS to finish

                # Passes the loaded page's HTML to the parser function
                detail_html = driver.page_source
                data = parse_property_page(detail_html)
                data['url'] = url
                data['date_scraped'] = dt.datetime.today().strftime("%Y-%m-%d")

                # Removes invalid formats before saving to CSV
                if "images" in data and isinstance(data["images"], list):
                    data["images"] = [u for u in data["images"] if is_valid_image_url(u)]

                all_property_data.append(data)

            except Exception as e:
                print(f"    [Failed to scrape {url}: {e}]")
                continue  # skips to the next URL

    print("\nDetail scraping complete.")
    driver.quit()  # closes browser after scraping is finished

    # 7. Save to CSV
    if all_property_data:
        df = pd.DataFrame(all_property_data)

        # Column re-ordering
        print(f"DataFrame created with {len(df.columns)} columns.")

        # Defines the columns
        ideal_cols_order = [
            'date_scraped', 'url', 'title', 'price', 'address',
            'Μέγεθος', 'Δωμάτια', 'Τύπος_ακινήτου', 'subtitle',
            'description', 'Διάρκεια_ενοικίασης'
        ]

        # Gets the columns that actually exist in the DataFrame
        existing_cols = df.columns.tolist()

        # Creates a new list with the ideal cols that exist
        ordered_cols = [col for col in ideal_cols_order if col in existing_cols]

        # Adds all other remaining columns that weren't in the ideal list
        other_cols = [col for col in existing_cols if col not in ordered_cols]

        # Sets the DataFrame's column order
        df = df[ordered_cols + other_cols]

        # Downloads images per listing and creates images table rows
        if image_download:
            IMAGES_DIR.mkdir(parents=True, exist_ok=True)
            print(f"\nDownloading images (this may take a while)...\n")

            # Detects new and removed listings
            # Current listing IDs from scraped DataFrame
            current_ids = set()

            for idx, row in df.iterrows():
                url_val = row.get("url")
                if url_val:
                    raw_id = url_val.split("/")[-1]
                    listing_id = name_for_path(raw_id)[:50]
                    current_ids.add(listing_id)

            images_table_rows = []
            today_str = dt.datetime.today().strftime("%Y-%m-%d")

            # If property has 'images' list, uses it, otherwise attempts to parse from df fields
            for idx, row in df.iterrows():
                url = row.get("url")
                if not url:
                    continue

                date_scraped = row.get("date_scraped", dt.datetime.today().strftime("%Y-%m-%d"))

                # Parses 'images' column
                images_raw = row.get("images", []) or []
                if isinstance(images_raw, str):
                    txt = images_raw.strip()
                    try:
                        imgs = json.loads(txt.replace("'", '"'))  # attempts standard JSON parse
                    except Exception:
                        # Splits on comma, removes surrounding quotes/spaces
                        imgs = [s.strip().strip("\"'") for s in re.split(r",\s*", txt) if s.strip()]
                else:
                    imgs = list(images_raw)

                # Filter for valid HTTP URLs
                valid_images = [u for u in imgs if u and is_valid_image_url(u)]

                # Creates output directory
                raw_id = url.split("/")[-1] if url else f"listing_{idx}"
                listing_id = name_for_path(raw_id)[:50]  # truncate
                listing_folder = IMAGES_DIR / listing_id
                listing_folder.mkdir(parents=True, exist_ok=True)

                meta_path = listing_folder / "meta.json"
                meta = load_meta(meta_path)

                if not meta:
                    meta = {
                        "url": url,
                        "first_seen": today_str,
                        "last_seen": today_str
                    }
                else:
                    meta.setdefault("first_seen", today_str)
                    meta["last_seen"] = today_str

                save_meta(meta_path, meta)

                # Downloads only if listing is new
                existing_files = [
                    p for p in listing_folder.glob("*")
                    if p.is_file() and p.suffix.lower() in VALID_EXTS and p.name != "meta.json"
                ]

                downloaded_paths = [str(p) for p in existing_files]
                current_count = len(existing_files)

                if current_count >= MAX_IMAGES:
                    print(f"[Skipping] {listing_id} (already has {len(existing_files)} images)")

                else:
                    print(f"[Checking images] {listing_id} ({current_count}/{MAX_IMAGES})")

                    for img_url in valid_images:
                        if current_count >= MAX_IMAGES:
                            break

                        fname = filename_from_url(img_url)
                        if not fname:
                            continue

                        local_path = listing_folder / fname

                        # Skips if this exact image already exists
                        if local_path.exists():
                            continue

                        # Small delay between individual images
                        time.sleep(random.uniform(0.5, 1.0))

                        local = download_image(
                            img_url=img_url,
                            folder=listing_folder,
                            user_agent=dynamic_user_agent,
                            referer_url=url
                        )
                        if local:
                            downloaded_paths.append(local)  # path string
                            current_count += 1

                            # Adds to the detailed tracking list
                            images_table_rows.append({
                                "url": url,
                                "date_scraped": date_scraped,
                                "image_url": img_url,
                                "local_path": local
                            })

                    time.sleep(random.uniform(1, 2))

                # Joins all local paths into a pipe-separated string
                df.at[idx, "image_paths_local"] = "|".join(downloaded_paths) if downloaded_paths else None

            # Removes listing image folders not seen recently
            GRACE_DAYS = 30
            today = dt.datetime.today()

            for folder in IMAGES_DIR.iterdir():
                if not folder.is_dir():
                    continue

                folder_id = folder.name
                if folder_id in current_ids:
                    continue  # skips folders just scraped

                meta_path = folder / "meta.json"
                meta = load_meta(meta_path)

                if not meta or "last_seen" not in meta:
                    continue

                try:
                    last_seen_date = dt.datetime.strptime(meta["last_seen"], "%Y-%m-%d")
                    days_missing = (today - last_seen_date).days

                    if days_missing >= GRACE_DAYS:
                        shutil.rmtree(folder)
                        print(f"[Deleted after {days_missing} days] {folder.name}")

                except Exception as e:
                    print(f"    [Cleanup error] {folder.name}: {e}")

            print("\nImage processing complete.\n")

        # Saves CSV after images are processed (if '--full' mode)
        # Uses utf-8-sig for Excel to correctly read Greek characters
        df.to_csv(LATEST_CSV, index=False, encoding='utf-8-sig')
        print(f"Saved {len(df)} listings to {LATEST_CSV}\n")

        # History aggregation
        if mode == "weekly":
            hist_path = DATA_DIR / "rentola_history_weekly.csv"
        else:
            hist_path = HISTORY_AGG_CSV

        hist_path.parent.mkdir(exist_ok=True)

        # Calculation of average price (area-weighted mean)
        if "price_per_m2" in df.columns and "Μέγεθος" in df.columns:

            # Converts columns to numeric types (if not already converted)
            df["price_per_m2"] = pd.to_numeric(df["price_per_m2"], errors="coerce")
            df["area_numeric"] = df["Μέγεθος"].apply(parse_numeric)

            # Keeps only rows that contain both price and area values
            df = df.dropna(subset=["price_per_m2", "area_numeric"])

            df = df[
                (df["area_numeric"] >= 30) &
                (df["area_numeric"] <= 250) &
                (df["price_per_m2"] >= 4) &
                (df["price_per_m2"] <= 40)
                ]

            clean_sample_size = len(df)

            # Computes weighted mean on cleaned data
            if clean_sample_size > 0:
                weighted_sum = (df["price_per_m2"] * df["area_numeric"]).sum()
                total_area = df["area_numeric"].sum()

                avg_price = weighted_sum / total_area if total_area > 0 else None
            else:
                avg_price = None
        else:
            avg_price = None

        record = {
            "date": dt.datetime.today().strftime("%Y-%m-%d"),
            "city": "Athens",
            "price_per_m2": round(avg_price, 2) if avg_price else None,
            "sample_size": len(df)
        }

        # Append or Create
        if hist_path.exists():
            hist_df = pd.read_csv(hist_path)
            hist_df = pd.concat([hist_df, pd.DataFrame([record])], ignore_index=True)
            hist_df.to_csv(hist_path, index=False)
        else:
            pd.DataFrame([record]).to_csv(hist_path, index=False)

        print(f"Updated {hist_path} with {record}")

    else:
        print("\nNo data was extracted. Check selectors or website access.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Dynamic Rent Adjustment System (DRAS) "
                    "- Automated Rentola Athens Data Collection Pipeline"
    )

    # Execution example: python rentola_scraper.py --full
    parser.add_argument(
        "--full",
        action="store_true",
        help="Download listing images"
    )

    parser.add_argument(
        "--mode",
        choices=["normal", "weekly"],
        default="normal",
        help="Execution mode (default: normal)"
    )

    args = parser.parse_args()

    IMAGE_DOWNLOAD = args.full
    MODE = args.mode

    if IMAGE_DOWNLOAD:
        print("FULL MODE: Images will be downloaded.")

    print(f"MODE: {MODE.upper()}")

    run_scraper(MAX_PAGES, image_download=IMAGE_DOWNLOAD, mode=MODE)
