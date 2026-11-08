# Dynamic Rent Adjustment System (DRAS) - Analytics & Scraper

This directory contains the Python-based data analytics and web scraping module for the **Dynamic Rent Adjustment System (DRAS)**.

This component is responsible for retrieving real estate market data from external sources and processing it into structured formats that can be imported by the Spring Boot backend.

## Contents
- [Overview](#overview)
- [Requirements](#requirements)
- [Installation & Setup](#installation--setup)
- [Usage Guide](#usage-guide)
- [Project Structure](#project-structure)
- [Integration with Backend](#integration-with-backend)

## Overview

The Analytics module uses Python to run automated web scraping tasks. It collects property listings from configured sources, processes the data, and saves the extracted information. This data is subsequently utilized by the backend API for dynamic rent adjustments and market trend analysis.

## Requirements
To run this module, ensure you have the following installed:
- **Python**: 3.12+
- **pip**: Python package manager
- **venv**: Standard library virtual environment module

## Installation & Setup

1. Navigate to the analytics directory:
   ```bash
   cd analytics
   ```

2. Create a virtual environment:
   ```bash
   python3 -m venv venv
   ```

3. Activate the virtual environment:
   - On Linux/macOS:
     ```bash
     source venv/bin/activate
     ```
   - On Windows:
     ```bash
     venv\Scripts\activate
     ```

4. Install the required dependencies and Playwright browser:
   ```bash
   pip install -r requirements.txt
   playwright install chromium
   ```

## Usage Guide

> **Note for IDE Users:** If your editor terminal opens at the project root (`dynamic-rent-adjustment-system`), use the root-relative commands below so that editor run actions execute correctly. Ensure the virtual environment is activated (`source analytics/venv/bin/activate` or `source venv/bin/activate`).

### 1. Web Scrapers
The scrapers are located in `analytics/scrapers/`. Run the Spitogatos scraping pipeline:

**Fast mode (from project root):**
```bash
python3 analytics/scrapers/spitogatos_scraper.py
```

**Push scraped listings to backend (from project root):**
```bash
python3 analytics/scrapers/spitogatos_scraper.py --push-backend
```

*(Or from inside `analytics/`: `python scrapers/spitogatos_scraper.py`)*

### 2. ARIMA Price Forecasting & Owner Price Suggestion Microservice
The forecasting engine (`analytics/models/arima_micro.py`) performs Auto-ARIMA time series modeling on scraped Spitogatos rental data and computes market benchmarks from scraped datasets:

**Run Auto-ARIMA forecasting and export model predictions JSON (from project root):**
```bash
python3 analytics/models/arima_micro.py
```

**Generate a price suggestion for a property via CLI (from project root):**
```bash
python3 analytics/models/arima_micro.py --suggest --size 75 --type APARTMENT --address "Kypseli, Athens" --floor 3 --year 2010
```

**Run rolling-origin out-of-sample ARIMA backtesting (from project root):**
```bash
python3 analytics/models/arima_micro.py --backtest
```

*(Or from inside `analytics/`: `python models/arima_micro.py`)*

The script outputs:
- `outputs/price_predictions.json`: Model metrics consumed by the Spring Boot backend (`/api/listings/price-suggestion`)
- `outputs/micro_forecast_athens.png`: Forecast plot with 95% confidence intervals

### 3. Macroeconomic SARIMA Forecasting & Bank of Greece Open Data Engine
The macroeconomic forecasting engine (`analytics/models/arima_macro.py`) models the long-term quarterly residential property price index published by the **Bank of Greece** ([Dataset 5: Index of Apartment Prices by Geographical Area](https://opendata.bankofgreece.gr/en/dataset/5)).

It features automated, time-based TTL checking against the Bank of Greece API to keep quarterly price index data dynamically synchronized without redundant downloads:

**Run macroeconomic quarterly SARIMA forecasting (with 14-day TTL auto-check):**
```bash
python3 analytics/models/arima_macro.py
```

**Force re-sync and download the latest release from the Bank of Greece API:**
```bash
python3 analytics/models/arima_macro.py --force-update
```

**Run offline / bypass API check using local cached dataset:**
```bash
python3 analytics/models/arima_macro.py --no-update
```

**Run standalone Bank of Greece updater module:**
```bash
python3 analytics/analysis/bog_updater.py
```

The script outputs:
- `outputs/macro_predictions.json`: Macroeconomic metrics and 8-quarter index projections consumed by the Spring Boot backend (`/api/analytics/macro-forecast`) and displayed on the DRAS Market Intelligence dashboard (`/market-trends`)
- `outputs/macro_forecast_plot.png`: Athens apartment price index forecast plot with 95% prediction intervals
- `outputs/SARIMA_Model_Summary.txt`: Detailed statsmodels SARIMAX mathematical summary
- `data/bog_dataset_metadata.json`: Synchronization metadata tracking last checked date, active quarter, and TTL settings
- `data/bank_of_greece_apartment_index.csv`: Normalized CSV time series ready for analytics

### 4. Running Unit Tests
Execute the analytical and data update unit test suite:
```bash
python3 -m unittest analytics/tests/test_arima_macro.py
```

## Project Structure

```text
analytics/
├── analysis/           # Data analysis scripts
├── data/               # Raw and processed datasets (Bank of Greece CSV/Excel, Spitogatos CSVs)
├── scrapers/           # Web scraping scripts (Spitogatos active scraper, Rentola legacy)
├── models/             # Time-series forecasting models (arima_micro.py, arima_macro.py)
├── outputs/            # Generated forecasts, plots, and prediction JSON payloads
├── tests/              # Python test suite
├── requirements.txt    # Python dependencies (openpyxl, pmdarima, statsmodels, etc.)
└── scraper.log         # Logs from scraper execution
```

## Integration with Backend

The collected listings can be imported into the main DRAS system. The backend provides an import API endpoint for ingesting external listings.

> **Note:** The backend enforces a maximum batch size limit of 500 for external listings per import request to maintain system stability and performance. Ensure your payload respects this limitation.

## License
This module is part of the DRAS project and is licensed under the **MIT License**.
