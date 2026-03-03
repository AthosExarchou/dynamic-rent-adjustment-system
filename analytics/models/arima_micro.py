"""
Dynamic Rent Adjustment System - ARIMA Forecasting Microservice
---------------------------------------------------------------
This script performs time-series forecasting on weekly rental price data scraped from Rentola.gr.
It uses an Auto-ARIMA model to predict future rent trends and visualizes the results
with confidence intervals.

Author: Athos Exarchou
Date: 26.10.2025
"""

# Imported libraries
import os
import logging
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as dates
import pmdarima as pm
import warnings
from pathlib import Path


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
warnings.simplefilter("ignore", category=FutureWarning)


# Load & prepare data
def load_rentola_data(filepath: str) -> pd.DataFrame:
    """
    Loads and cleans the aggregated CSV data, ensuring correct date/numeric formats.
    Returns: Cleaned pd.DataFrame containing [city, date, rent_price].
    """
    logging.info(f"Loading input file: {filepath}")
    df = pd.read_csv(filepath)
    df.columns = df.columns.str.strip().str.lower()

    # Case 1: historical file (already aggregated)
    if {"date", "city", "price_per_m2"}.issubset(df.columns):
        df["date"] = pd.to_datetime(df["date"], errors="coerce")
        df.rename(columns={"price_per_m2": "rent_price"}, inplace=True)
        logging.info("Detected historical file.")
        return df[["city", "date", "rent_price"]].dropna()
    else:
        raise ValueError("Input CSV must be pre-aggregated.")


def forecast_city_rent(df: pd.DataFrame, city_name: str, steps: int = 5):
    """
    Resamples data to weekly frequency, interpolates missing values, and fits an Auto-ARIMA model.
    Generates a forecast for 'steps' weeks ahead.
    Returns: pd.Series of forecasted values (or None on failure).
    """
    city_df = df[df["city"] == city_name].copy()

    if city_df.empty:
        logging.warning(f"No data for city: {city_name}")
        return None

    # Resamples weekly (Monday=start)
    city_df.set_index("date", inplace=True)
    weekly_df = city_df["rent_price"].resample("W-MON").mean().sort_index()

    # Fills gaps
    weekly_df = weekly_df.interpolate(method='linear')

    # 14-Day (2-Week) Rolling Average
    weekly_df = weekly_df.rolling(window=2, min_periods=1).mean()

    if len(weekly_df) < 12:
        logging.warning(f"Not enough weekly data to forecast for {city_name}.")
        return None

    try:
        logging.info(f"Fitting Auto-ARIMA for {city_name}...")
        model = pm.auto_arima(
            weekly_df,
            seasonal=False,
            stepwise=True,
            suppress_warnings=True,
            error_action='ignore'
        )

        # Get forecast and confidence intervals
        forecast, conf_int = model.predict(n_periods=steps, return_conf_int=True, alpha=0.05)

        # Converts to Series/DataFrame
        future_dates = pd.date_range(weekly_df.index[-1] + pd.Timedelta(days=7),
                                     periods=steps, freq='W-MON')

        forecast_series = pd.Series(forecast, index=future_dates)
        conf_df = pd.DataFrame(conf_int, index=future_dates, columns=['lower', 'upper'])

        logging.info(f"Forecast for {city_name} generated.")

        plot_forecast(weekly_df, forecast_series, conf_df, city_name)
        return forecast_series

    except Exception as e:
        logging.error(f"ARIMA failed for {city_name}: {e}")
        return None


def plot_forecast(history, forecast, conf_int, city_name: str):
    """
    Visualizes historical data vs. forecast with a 95% confidence interval.
    Output: Saves a PNG plot to the 'outputs/' directory.
    """
    output_dir = Path("outputs")
    output_dir.mkdir(exist_ok=True)

    plt.figure(figsize=(10, 6))

    # Plot Historical Data
    plt.plot(history.index, history.values, label="Historical (weekly)", linewidth=2)

    # Forecast
    plt.plot(forecast.index, forecast.values, label="Forecast", linestyle="--", linewidth=2)

    # Confidence interval
    plt.fill_between(
        forecast.index,
        conf_int['lower'],
        conf_int['upper'],
        alpha=0.2,
        label="95% Confidence Interval"
    )

    plt.title(f"Rent Forecast for {city_name}")
    plt.xlabel("Date")
    plt.ylabel("Price per m² (€)")
    plt.legend()
    plt.grid(True, alpha=0.3)

    plt.gcf().autofmt_xdate()
    plt.gca().xaxis.set_major_formatter(dates.DateFormatter("%Y-%m-%d"))

    plt.tight_layout()
    output_path = output_dir / f"micro_forecast_{city_name.lower()}.png"
    plt.savefig(output_path, dpi=150)
    plt.show()

    logging.info(f"Saved forecast plot to {output_path}")


def main():
    """Main execution flow."""
    filepath = "data/rentola_history.csv"

    if not os.path.exists(filepath):
        logging.error(f"File not found: {filepath}")
        return

    df = load_rentola_data(filepath)
    for city in df["city"].unique():
        forecast_city_rent(df, city)


if __name__ == "__main__":
    main()
