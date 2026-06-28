"""
Surgical Phase-1 Scraper for Monte Carlo Population Baseline
------------------------------------------------------------
This script bypasses detail pages (Phase 2) entirely to avoid Datadome
velocity bans. It extracts Price and Size directly from the search
tiles to build the population dataset for spitogatos_sample_size_analysis.py.
"""

# Imported Libraries
import undetected_chromedriver as uc
from webdriver_manager.chrome import ChromeDriverManager
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from bs4 import BeautifulSoup
import pandas as pd
import time
import random
import re
import shutil
from urllib.parse import urljoin
from pathlib import Path

# CONFIG
POPULATION_CSV = Path("data/spitogatos_population.csv")
MAX_PAGES = 280


def setup_chrome():
    options = uc.ChromeOptions()
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--disable-blink-features=AutomationControlled")
    options.add_argument("--disable-notifications")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    driver = uc.Chrome(options=options)
    driver.execute_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
    return driver


def parse_money(text):
    if not text: return None
    s = re.sub(r"[^\d.,\-]", "", str(text))
    if not s: return None
    if "." in s and "," in s:
        s = s.replace(".", "").replace(",", ".")
    elif "," in s:
        s = s.replace(",", ".")
    elif "." in s:
        if re.match(r"^\d{1,3}(\.\d{3})+$", s): s = s.replace(".", "")
    try:
        return float(s)
    except:
        return None


def get_population():
    driver = setup_chrome()

    wait = WebDriverWait(driver, 15)
    print("WebDriver initialized.")

    base_url = "https://www.spitogatos.gr/enoikiaseis-katoikies/athina-kentro/selida_{}"
    all_data = []
    seen_urls = set()
    cookies_accepted = False

    try:
        for page in range(1, MAX_PAGES + 1):
            url = base_url.format(page)
            print(f"Scraping Page {page}/{MAX_PAGES}...")

            try:
                driver.get(url)
                time.sleep(random.uniform(4, 7))

                BLOCK_SIGNALS = ["access denied", "security", "just a moment"]
                if any(s in driver.title.lower() for s in BLOCK_SIGNALS):
                    print("Blocked by Datadome. Saving and aborting...")
                    break

                if not cookies_accepted:
                    try:
                        btn = WebDriverWait(driver, 3).until(
                            EC.element_to_be_clickable((By.XPATH, "//button[contains(., 'ΣΥΜΦΩΝΩ')]")))
                        driver.execute_script("arguments[0].click();", btn)
                        cookies_accepted = True
                        time.sleep(1)
                    except Exception:
                        pass

                # Scroll down slowly to trigger lazy loading
                scroll_height = random.uniform(0.4, 0.8)
                driver.execute_script(f"window.scrollTo(0, document.body.scrollHeight * {scroll_height});")
                time.sleep(random.uniform(2, 6))
                driver.execute_script("window.scrollTo(0, document.body.scrollHeight);")
                time.sleep(random.uniform(2, 6))

                soup = BeautifulSoup(driver.page_source, "html.parser")
                articles = soup.find_all("article", class_="ordered-element")

                if not articles:
                    print("No more listings found. Reached end of market.")
                    break

                for article in articles:
                    link_tag = article.select_one("a.tile__link")
                    if not link_tag: continue

                    l_url = urljoin("https://www.spitogatos.gr", link_tag["href"])
                    if l_url in seen_urls: continue
                    seen_urls.add(l_url)

                    title = article.select_one("h3.tile__title").get_text(strip=True) if article.select_one(
                        "h3.tile__title") else ""

                    # Extract Size
                    size_m2 = None
                    size_match = re.search(r"(\d+(?:[.,]\d+)?)\s*τ\.μ\.", title)
                    if size_match:
                        size_m2 = float(size_match.group(1).replace(",", "."))
                        
                    if not size_m2 or size_m2 == 0:
                        continue

                    # Extract Price
                    price_tag = article.select_one(".price__text")
                    price_val = parse_money(price_tag.get_text()) if price_tag else None

                    # Calculate PPM2
                    ppm2 = round(price_val / size_m2, 2) if price_val and size_m2 else None
                    if ppm2 and (ppm2 < 2 or ppm2 > 80):
                        ppm2 = None

                    all_data.append({
                        "url": l_url,
                        "price": price_val,
                        "Μέγεθος": size_m2,
                        "price_per_m2": ppm2
                    })

                time.sleep(random.uniform(5, 10))

                if page % 10 == 0:
                    print("Taking a 45-second break...")
                    time.sleep(random.uniform(40, 55))

            except Exception as e:
                print(f"Error on page {page}: {e}")
                continue

    finally:
        try:
            driver.quit()
        except Exception as quit_err:
            print(f"Warning: error closing browser: {quit_err}")

    if all_data:
        df = pd.DataFrame(all_data)
        # Clean invalid outliers
        df = df.dropna(subset=["price_per_m2", "Μέγεθος"])
        df = df[
            (df["Μέγεθος"] >= 20) &
            (df["Μέγεθος"] <= 500) &
            (df["price_per_m2"] >= 2) &
            (df["price_per_m2"] <= 80)
        ]

        POPULATION_CSV.parent.mkdir(exist_ok=True)
        df.to_csv(POPULATION_CSV, index=False, encoding='utf-8-sig')
        print(f"\nSuccessfully saved {len(df)} listings to {POPULATION_CSV}")
    else:
        print("No data extracted.")


if __name__ == "__main__":
    get_population()
