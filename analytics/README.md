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

4. Install the required dependencies:
   ```bash
   pip install -r requirements.txt
   ```

## Usage Guide

> **Note for IDE Users:** If your editor terminal opens at the project root (`dynamic-rent-adjustment-system`), use the root-relative commands below so that IDE automatic run buttons ("Run in Terminal") work out of the box. Ensure the virtual environment is activated (`source analytics/venv/bin/activate` or `source venv/bin/activate`).

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

**Get an instant price suggestion for a property via CLI (from project root):**
```bash
python3 analytics/models/arima_micro.py --suggest --size 75 --type APARTMENT --address "Kypseli, Athens" --floor 3 --year 2010
```

*(Or from inside `analytics/`: `python models/arima_micro.py`)*

The script outputs:
- `outputs/price_predictions.json`: Comprehensive model metrics consumed by the Spring Boot backend (`/api/listings/price-suggestion`)
- `outputs/micro_forecast_athens.png`: Visualized forecast plot with 95% confidence intervals

## Project Structure

```text
analytics/
├── analysis/           # Data analysis scripts and Jupyter notebooks
├── data/               # Raw and processed datasets
├── scrapers/           # Web scraping scripts (Spitogatos active scraper, Rentola legacy)
├── models/             # Machine learning models (arima_micro.py, arima_macro.py)
├── outputs/            # Generated forecasts, plots, and price_predictions.json
├── requirements.txt    # Python dependencies (playwright, pandas, pmdarima, statsmodels, etc.)
└── scraper.log         # Logs from scraper execution
```

## Integration with Backend

The collected listings can be imported into the main DRAS system. The backend provides an import API endpoint for ingesting external listings. 

> **Note:** The backend enforces a maximum batch size limit of 500 for external listings per import request to maintain system stability and performance. Ensure your payload respects this limitation.

## License
This module is part of the DRAS project and is licensed under the **MIT License**.
