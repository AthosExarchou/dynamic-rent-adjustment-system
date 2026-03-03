"""
Dynamic Rent Adjustment System - ARIMA Forecasting Microservice
---------------------------------------------------------------
Performs automated SARIMA forecasting on apartment price indices (Athens).
Includes data loading, model fitting, forecast generation, and visualization
with 95% confidence intervals.

Author: Athos Exarchou
Date: 19.10.2025
"""

# Imported libraries
from __future__ import annotations
import pandas as pd
import matplotlib.pyplot as plt
import pmdarima as pm
import warnings
import logging
from pathlib import Path


# 0. Configuration
CONFIG = {
    "file_path": Path("data/nbg_data.xls"),
    "target_column": "New_index_of_apartment_prices_by_geographical_area_Athens",
    "forecast_periods": 8,  # 8 quarters = 2 years
    "season_length": 4      # 4 quarters per year
}
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
warnings.filterwarnings("ignore", category=FutureWarning)


# 1. Load & Clean Data
def load_and_clean_data(file_path: Path, target_column: str) -> pd.Series:
    """
    Loads Excel data and prepares quarterly time series for the given region.

    Args:
        file_path: Path to the Excel dataset.
        target_column: Target column for apartment price index.

    Returns:
        pd.Series: Cleaned time series indexed by quarterly timestamps.
    """
    df = pd.read_excel(file_path, engine='xlrd')
    df = df.dropna(subset=['Reference_year', 'Reference_quarter', target_column])

    df['Year'] = df['Reference_year'].astype(int)
    df['Quarter'] = df['Reference_quarter'].astype(int)
    df['Month'] = (df['Quarter'] - 1) * 3 + 1
    df['Date'] = pd.to_datetime(df['Year'].astype(str) + '-' + df['Month'].astype(str) + '-01')

    df = df.set_index('Date')
    ts = df[target_column].asfreq('QS-JAN')

    logging.info(f"Loaded {len(ts)} quarterly records from {file_path.name}")
    return ts


# 2. Fit SARIMA Model
def fit_sarima_model(
    time_series: pd.Series,
    season_length: int,
    summary_file: Path | None = None
) -> pm.ARIMA:
    """
    Perform automatic SARIMA model selection using stepwise AIC minimization.

    Args:
        time_series: Quarterly time series data.
        season_length: Seasonal cycle length (e.g., 4 for quarterly).
        summary_file: Name of the file the model will be saved to.

    Returns:
        pm.ARIMA: The fitted SARIMA model.
    """
    logging.info("Searching for best SARIMA model...")
    model = pm.auto_arima(
        time_series,
        seasonal=True,
        m=season_length,
        stepwise=True,
        suppress_warnings=True,
        trace=True
    )
    logging.info("Model fitting complete.")
    logging.info(f"Selected model: {model.order} x {model.seasonal_order}")

    # Displays summary in console
    print("\n" + "=" * 80)
    print("--- Best Model Summary ---")
    print(model.summary())
    print("=" * 80 + "\n")

    # Saves summary to file
    if summary_file is not None:
        with open(summary_file, "w", encoding="utf-8") as f:
            f.write(str(model.summary()))
        logging.info(f"Model summary saved to {summary_file}")

    return model


# 3. Generate Forecast
def generate_forecast(model: pm.ARIMA, periods: int, last_date: pd.Timestamp) -> tuple[pd.Series, pd.DataFrame]:
    """
    Generate forecasts and confidence intervals for given periods.

    Args:
        model: Fitted SARIMA model.
        periods: Number of quarters to forecast.
        last_date: Last date in the training data.

    Returns:
        tuple[pd.Series, pd.DataFrame]: Forecast values and confidence intervals.
    """
    forecasts, conf_int = model.predict(n_periods=periods, return_conf_int=True, alpha=0.05)
    forecast_index = pd.date_range(start=last_date, periods=periods + 1, freq='QS-JAN')[1:]

    forecast_series = pd.Series(forecasts, index=forecast_index)
    conf_df = pd.DataFrame(conf_int, index=forecast_index, columns=['Lower 95%', 'Upper 95%'])

    logging.info(f"Forecast generated for next {periods} quarters.")
    return forecast_series, conf_df


# 4. Visualization
def plot_forecast(
    historical: pd.Series,
    forecast: pd.Series,
    conf_int: pd.DataFrame,
    region_name: str = "Athens",
    save_path: Path | None = None
) -> None:
    """
    Plot historical and forecasted data with confidence intervals.

    Args:
        historical: Original time series data.
        forecast: Forecasted values.
        conf_int: Lower/upper confidence bounds.
        region_name: Name of the region.
        save_path: Output directory.
    """
    plt.figure(figsize=(14, 7))
    plt.plot(historical, label=f'Historical Data ({region_name})')
    plt.plot(forecast, label='ARIMA Forecast', color='red', linestyle='--')

    plt.fill_between(
        forecast.index,
        conf_int['Lower 95%'],
        conf_int['Upper 95%'],
        color='red',
        alpha=0.1,
        label='95% Confidence Interval'
    )

    plt.title(f'Listing Price Index Forecast - {region_name.upper()}')
    plt.ylabel('Price Index')
    plt.legend(loc='upper left')
    plt.grid(True)
    plt.tight_layout()

    if save_path:
        # Ensures the directory exists
        save_path.parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(save_path, dpi=300)
        logging.info(f"Forecast plot saved to {save_path}")

    plt.show()


# 5. Main Execution
def main() -> None:
    """Main execution flow."""
    output_dir = Path("outputs")
    output_dir.mkdir(parents=True, exist_ok=True)

    ts_data = load_and_clean_data(CONFIG["file_path"], CONFIG["target_column"])
    summary_path = output_dir / "SARIMA_Model_Summary.txt"
    sarima_model = fit_sarima_model(ts_data, CONFIG["season_length"], summary_file=summary_path)

    forecast_series, conf_int = generate_forecast(
        sarima_model, CONFIG["forecast_periods"], ts_data.index[-1]
    )

    plot_path = output_dir / "macro_forecast_plot.png"
    plot_forecast(ts_data, forecast_series, conf_int, "Athens", plot_path)


if __name__ == "__main__":
    main()
