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

The scrapers are located in the `scrapers/` directory. Check the script instructions or run them using python:

```bash
# Example
python scrapers/main_scraper.py
```
*(Check specific scraper scripts for their expected arguments or CLI flags.)*

## Project Structure

```text
analytics/
├── analysis/           # Data analysis scripts and Jupyter notebooks
├── data/               # Raw and processed datasets
├── scrapers/           # Web scraping scripts (e.g., BeautifulSoup, Playwright)
├── models/             # Machine learning models (if applicable)
├── outputs/            # Generated reports or exports
├── requirements.txt    # Python dependencies
└── scraper.log         # Logs from scraper execution
```

## Integration with Backend

The collected listings can be imported into the main DRAS system. The backend provides an import API endpoint for ingesting external listings. 

> **Note:** The backend enforces a maximum batch size limit of 500 for external listings per import request to maintain system stability and performance. Ensure your payload respects this limitation.

## License
This module is part of the DRAS project and is licensed under the **MIT License**.
