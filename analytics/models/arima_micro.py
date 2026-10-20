"""
Dynamic Rent Adjustment System - ARIMA Forecasting Microservice & Scraped Data Analytics
----------------------------------------------------------------------------------------
This module performs time-series forecasting on rental price data scraped from Spitogatos.gr,
aggregates market benchmarks across active scraped residential listings (spitogatos_athens_listings.csv),
and generates automated price suggestions for property owners when creating listings.

Author: Athos Exarchou
Date: 26.10.2025 (Updated 2026 for Spitogatos.gr)
"""

# Imported libraries
import sys
import re
import json
import logging
import argparse
import warnings
import unicodedata
from pathlib import Path
from typing import Optional, Dict, Any, List
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.dates as dates
import pmdarima as pm

def strip_accents(text: Any) -> str:
    """Normalizes text to lowercase and strips diacritics/accents."""
    if pd.isna(text) or text is None:
        return ""
    normalized = unicodedata.normalize("NFD", str(text).lower())
    return "".join(c for c in normalized if unicodedata.category(c) != "Mn")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
warnings.simplefilter("ignore", category=FutureWarning)

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "data"
OUTPUTS_DIR = BASE_DIR / "outputs"

SOURCE_DATA_FILES = [
    "spitogatos_history_weekly.csv",
    "spitogatos_history.csv",
    "spitogatos_athens_listings.csv",
]


def get_source_files_max_mtime(data_dir: Optional[Path] = None) -> float:
    """Returns the latest modification timestamp among active source data files."""
    directory = data_dir or DATA_DIR
    mtimes = []
    for fname in SOURCE_DATA_FILES:
        p = directory / fname
        if p.exists():
            mtimes.append(p.stat().st_mtime)
    return max(mtimes) if mtimes else 0.0


# Load & Prepare Historical Time Series Data
def load_historical_data(filepath: Optional[str | Path] = None) -> pd.DataFrame:
    """
    Loads and cleans aggregated rental price data, ensuring correct date/numeric formats.
    By default, aggregates active Spitogatos time-series datasets (spitogatos_history.csv
    and spitogatos_history_weekly.csv). In case of overlapping dates, spitogatos_history_weekly.csv
    takes strict precedence.
    Returns: Cleaned pd.DataFrame containing [city, date, rent_price].
    """
    if filepath:
        path = Path(filepath)
        if not path.is_absolute() and not path.exists():
            path = DATA_DIR / path.name

        logging.info(f"Loading input file: {path}")
        if not path.exists():
            raise FileNotFoundError(f"File not found: {path}")

        df = pd.read_csv(path)
        df.columns = df.columns.str.strip().str.lower()

        if {"date", "city", "price_per_m2"}.issubset(df.columns):
            df["date"] = pd.to_datetime(df["date"], errors="coerce")
            df["rent_price"] = pd.to_numeric(df["price_per_m2"], errors="coerce")
            # Strict cleaning: keep only finite positive rental rates in realistic residential bounds
            clean_df = df[
                df["date"].notna()
                & df["rent_price"].notna()
                & np.isfinite(df["rent_price"])
                & (df["rent_price"] >= 2.0)
                & (df["rent_price"] <= 150.0)
            ][["city", "date", "rent_price"]].dropna()
            logging.info("Detected historical file with valid numeric price_per_m2.")
            return clean_df
        else:
            raise ValueError("Input CSV must be pre-aggregated with date, city, price_per_m2 columns.")

    # Default flow: Load Spitogatos historical datasets.
    # Load spitogatos_history first, then spitogatos_history_weekly so weekly takes precedence on duplicate dates
    history_dfs = []
    spito_hist = DATA_DIR / "spitogatos_history.csv"
    spito_weekly = DATA_DIR / "spitogatos_history_weekly.csv"

    for p in [spito_hist, spito_weekly]:
        if p.exists():
            try:
                sub_df = pd.read_csv(p)
                sub_df.columns = sub_df.columns.str.strip().str.lower()
                if {"date", "city", "price_per_m2"}.issubset(sub_df.columns):
                    history_dfs.append(sub_df)
            except Exception as e:
                logging.warning(f"Could not read historical file {p}: {e}")

    if not history_dfs:
        # Fallback to legacy rentola_history if Spitogatos files are not found
        legacy_hist = DATA_DIR / "rentola_history.csv"
        if legacy_hist.exists():
            logging.info(f"Using fallback legacy history file: {legacy_hist}")
            sub_df = pd.read_csv(legacy_hist)
            sub_df.columns = sub_df.columns.str.strip().str.lower()
            history_dfs.append(sub_df)

    if not history_dfs:
        raise FileNotFoundError("No historical price files found in data directory.")

    df = pd.concat(history_dfs, ignore_index=True)
    df["date"] = pd.to_datetime(df["date"], errors="coerce")
    df["rent_price"] = pd.to_numeric(df["price_per_m2"], errors="coerce")

    # Strict numeric cleaning and residential sanity bounds (2 to 150 EUR/m2)
    df = df[
        df["date"].notna()
        & df["rent_price"].notna()
        & np.isfinite(df["rent_price"])
        & (df["rent_price"] >= 2.0)
        & (df["rent_price"] <= 150.0)
    ]

    # Ensure spitogatos_history_weekly overwrites spitogatos_history on duplicate dates
    df = df.drop_duplicates(subset=["date", "city"], keep="last").sort_values("date")

    # Validate observation count per city
    for city, count in df["city"].value_counts().items():
        if count < 12:
            logging.warning(
                f"City '{city}' has only {count} observations; Auto-ARIMA requires at least 12 weekly observations."
            )

    logging.info(f"Loaded {len(df)} historical data points across cities: {df['city'].unique().tolist()}")
    return df[["city", "date", "rent_price"]].dropna()


def load_rentola_data(filepath: str | Path) -> pd.DataFrame:
    """Legacy alias for load_historical_data (preserved for backwards compatibility)."""
    return load_historical_data(filepath)


def _compute_historical_baseline(
    city_name: str = "Athens",
    df: Optional[pd.DataFrame] = None,
) -> Dict[str, Any]:
    """
    Derives baseline market statistics directly from the historical weekly CSV,
    without running ARIMA. Used as a live, data-driven fallback whenever:
      - ARIMA fails to fit (not enough data, convergence issues)
      - The cached model JSON does not yet exist on first run
      - A caller needs a quick price reference without a full model regeneration

    All returned values are computed fresh from the actual rows in the CSV.
    As new weekly rows are appended, all derived values update automatically
    without manual intervention or hardcoded prices.

    Args:
        city_name: City to compute the baseline for (default: "Athens").
        df: Optional pre-loaded DataFrame (output of load_historical_data()). When
            supplied the function skips the disk read entirely, guaranteeing that the
            fallback baseline uses the same historical source as the ARIMA path.
            When omitted (or None) the function loads the default datasets from disk.

    Returns:
        Dict containing lastHistoricalDate, predictedPricePerM2 (rolling mean),
        lowerBoundPerM2 / upperBoundPerM2 (historical variability range from std), trend,
        trendPercentage, modelStatus="STATISTICAL_BASELINE", horizonWeeks=0,
        and overallAvgPricePerM2 (for use as market_avg_fallback in benchmarks).
        Returns an empty dict if no data is available.
    """
    try:
        if df is None:
            df = load_historical_data()
        city_df = df[df["city"].str.lower() == city_name.lower()].sort_values("date")
        if city_df.empty:
            logging.warning(f"No historical data found for '{city_name}'; cannot derive baseline.")
            return {}

        # Resample to weekly (Sunday matching scrape schedule) to normalize mixed 3-day / 7-day intervals
        city_df = city_df.set_index("date")
        weekly_series = city_df["rent_price"].resample("W-SUN").mean().ffill().dropna()
        if weekly_series.empty:
            logging.warning(f"No valid weekly observations for '{city_name}'; cannot derive baseline.")
            return {}

        series = weekly_series
        last_date = series.index[-1].strftime("%Y-%m-%d")
        last_val = float(round(series.iloc[-1], 4))

        # Rolling statistics over the most recent 8 observations (or all if fewer)
        window = min(8, len(series))
        recent = series.iloc[-window:]
        rolling_mean = float(round(recent.mean(), 4))
        # Standard deviation of recent window to capture recent volatility instead of a fixed spread
        rolling_std = float(round(recent.std(ddof=1), 4)) if len(recent) > 1 else float(round(last_val * 0.02, 4))

        # Trend direction: compare the mean of the last 4 observations vs. the prior 4, with zero-denominator guards
        if len(series) >= 8:
            prior_mean = series.iloc[-8:-4].mean()
            if abs(prior_mean) > 1e-6:
                trend_pct = round(((series.iloc[-4:].mean() - prior_mean) / prior_mean) * 100, 2)
            else:
                trend_pct = 0.0
        elif len(series) >= 2:
            denom = series.iloc[0]
            if abs(denom) > 1e-6:
                trend_pct = round(((series.iloc[-1] - denom) / denom) * 100, 2)
            else:
                trend_pct = 0.0
        else:
            trend_pct = 0.0

        trend = "RISING" if trend_pct > 1.0 else ("FALLING" if trend_pct < -1.0 else "STABLE")

        # Historical variability spread: rolling_mean +/- 1.96 * rolling_std
        variability_half = round(1.96 * rolling_std, 4)

        return {
            "modelStatus": "STATISTICAL_BASELINE",
            "intervalType": "HISTORICAL_VARIABILITY_SPREAD",
            "intervalDescription": "Historical +/- 1.96 standard deviation window over recent observations",
            "confidenceLevel": None,
            "lastHistoricalDate": last_date,
            "lastHistoricalPricePerM2": last_val,
            "predictedPricePerM2": rolling_mean,
            "lowerBoundPerM2": float(round(max(0.0, rolling_mean - variability_half), 2)),
            "upperBoundPerM2": float(round(rolling_mean + variability_half, 2)),
            "trend": trend,
            "trendPercentage": trend_pct,
            "horizonWeeks": 0,
            "modelOrder": "statistical-baseline (no ARIMA)",
            "weeklyForecast": [],
            # Exposed separately so compute_scraped_market_benchmarks can use it
            "overallAvgPricePerM2": rolling_mean,
        }
    except Exception as e:
        logging.warning(f"Could not compute historical baseline for '{city_name}': {e}")
        return {}


def forecast_city_rent(df: pd.DataFrame, city_name: str, steps: int = 5, save_plot: bool = True):
    """
    Resamples data to weekly frequency and fits an Auto-ARIMA model.
    Generates a forecast for 'steps' weeks ahead.
    Returns: pd.Series of forecasted values (or None on failure).
    """
    city_df = df[df["city"].str.lower() == city_name.lower()].copy()

    if city_df.empty:
        logging.warning(f"No data for city: {city_name}")
        return None

    # Resamples weekly (Sunday start)
    city_df.set_index("date", inplace=True)
    raw_weekly = city_df["rent_price"].resample("W-SUN").mean().sort_index()

    # Check actual observed (non-NaN) weeks BEFORE forward-filling to avoid counting fabricated weeks
    if raw_weekly.notna().sum() < 12:
        logging.warning(f"Not enough actual weekly observations ({raw_weekly.notna().sum()} < 12) to forecast for {city_name}.")
        return None

    # Preserve regular W-SUN grid across interior gaps without deleting timestamps
    weekly_df = raw_weekly.interpolate(method="linear").ffill().bfill().dropna()

    try:
        logging.info(f"Fitting Auto-ARIMA for {city_name}...")
        # Note: seasonal=False is intentional because the current weekly series length
        # (< 1 year) does not span multiple full annual cycles (52 weeks) required for seasonal ARIMA.
        model = pm.auto_arima(
            weekly_df,
            seasonal=False,
            stepwise=True,
            suppress_warnings=True,
            error_action='ignore'
        )

        # Get forecast and prediction intervals
        forecast, conf_int = model.predict(n_periods=steps, return_conf_int=True, alpha=0.05)

        # Converts to Series/DataFrame
        future_dates = pd.date_range(weekly_df.index[-1] + pd.Timedelta(days=7),
                                     periods=steps, freq='W-SUN')

        forecast_series = pd.Series(forecast, index=future_dates)
        conf_df = pd.DataFrame(conf_int, index=future_dates, columns=['lower', 'upper'])

        logging.info(f"Forecast for {city_name} generated.")

        if save_plot:
            plot_forecast(weekly_df, forecast_series, conf_df, city_name)

        return forecast_series

    except Exception as e:
        logging.error(f"ARIMA failed for {city_name}: {e}")
        return None


def get_city_forecast_details(df: pd.DataFrame, city_name: str = "Athens", steps: int = 5) -> Optional[Dict[str, Any]]:
    """
    Fits Auto-ARIMA and extracts detailed forecast metrics including 95% prediction intervals,
    order parameters, and trend direction.
    """
    city_df = df[df["city"].str.lower() == city_name.lower()].copy()
    if city_df.empty:
        return None

    city_df.set_index("date", inplace=True)
    raw_weekly = city_df["rent_price"].resample("W-SUN").mean().sort_index()

    # Check actual observed (non-NaN) weeks BEFORE forward-filling
    if raw_weekly.notna().sum() < 12:
        return None

    # Preserve regular W-SUN grid across interior gaps without deleting timestamps
    weekly_df = raw_weekly.interpolate(method="linear").ffill().bfill().dropna()

    try:
        model = pm.auto_arima(
            weekly_df,
            seasonal=False,
            stepwise=True,
            suppress_warnings=True,
            error_action='ignore'
        )

        forecast, conf_int = model.predict(n_periods=steps, return_conf_int=True, alpha=0.05)
        future_dates = pd.date_range(weekly_df.index[-1] + pd.Timedelta(days=7), periods=steps, freq='W-SUN')

        forecast_arr = forecast.values if hasattr(forecast, "values") else forecast
        last_historical_date = weekly_df.index[-1].strftime("%Y-%m-%d")
        last_historical_val = float(round(weekly_df.iloc[-1], 2))
        # Headline predicted value and prediction interval bounds correspond to the full horizon (steps weeks ahead)
        predicted_val = float(round(forecast_arr[-1], 2))
        lower_bound = float(round(conf_int[-1][0], 2))
        upper_bound = float(round(conf_int[-1][1], 2))

        # Denominator zero guard: projected trend percentage over the full horizon
        if abs(last_historical_val) > 1e-6:
            trend_pct = round(((predicted_val - last_historical_val) / last_historical_val) * 100, 2)
        else:
            trend_pct = 0.0

        if trend_pct > 1.0:
            trend = "RISING"
        elif trend_pct < -1.0:
            trend = "FALLING"
        else:
            trend = "STABLE"

        weekly_steps = []
        for d, val, ci in zip(future_dates, forecast, conf_int):
            weekly_steps.append({
                "date": d.strftime("%Y-%m-%d"),
                "predictedPricePerM2": float(round(val, 2)),
                "lowerBound": float(round(ci[0], 2)),
                "upperBound": float(round(ci[1], 2))
            })

        return {
            "city": city_name,
            "modelStatus": "ARIMA",
            "confidenceLevel": 0.95,
            "intervalType": "ARIMA_PREDICTION_INTERVAL",
            "intervalDescription": "Auto-ARIMA 95% prediction interval for future weekly observations",
            "lastHistoricalDate": last_historical_date,
            "lastHistoricalPricePerM2": last_historical_val,
            "predictedPricePerM2": predicted_val,
            "lowerBoundPerM2": lower_bound,
            "upperBoundPerM2": upper_bound,
            "trend": trend,
            "trendPercentage": trend_pct,
            "horizonWeeks": steps,
            "modelOrder": str(model.order),
            "weeklyForecast": weekly_steps
        }
    except Exception as e:
        logging.error(f"Failed to fit Auto-ARIMA forecast details for {city_name}: {e}")
        return None


def plot_forecast(history, forecast, conf_int, city_name: str):
    """
    Visualizes historical data vs. forecast with a 95% prediction interval.
    Output: Saves a PNG plot to the 'outputs/' directory.
    """
    OUTPUTS_DIR.mkdir(parents=True, exist_ok=True)

    plt.figure(figsize=(10, 6))

    # Plot Historical Data
    plt.plot(history.index, history.values, label="Historical (weekly)", linewidth=2)

    # Forecast
    plt.plot(forecast.index, forecast.values, label="Forecast", linestyle="--", linewidth=2)

    # Prediction interval
    plt.fill_between(
        forecast.index,
        conf_int['lower'],
        conf_int['upper'],
        alpha=0.2,
        label="95% Prediction Interval"
    )

    plt.title(f"Rent Forecast for {city_name}")
    plt.xlabel("Date")
    plt.ylabel("Price per m2 (EUR)")
    plt.legend()
    plt.grid(True, alpha=0.3)

    plt.gcf().autofmt_xdate()
    plt.gca().xaxis.set_major_formatter(dates.DateFormatter("%Y-%m-%d"))

    plt.tight_layout()
    output_path = OUTPUTS_DIR / f"micro_forecast_{city_name.lower()}.png"
    plt.savefig(output_path, dpi=150)
    plt.close()

    logging.info(f"Saved forecast plot to {output_path}")


# Causal Rolling-Origin Backtesting & Multi-Horizon Evaluation
def evaluate_arima_backtest(df: pd.DataFrame, city_name: str = "Athens", min_train: int = 12, max_horizon: int = 5) -> Dict[str, Any]:
    """
    Evaluates out-of-sample forecast accuracy using a causal rolling-origin backtest.
    Zero look-ahead bias:
      1. Each forecast origin t uses ONLY information available on or before that date.
      2. No future observations are used for interpolation or smoothing.
      3. Point forecasts and 95% Prediction Intervals (PI) are evaluated against the ACTUAL RAW weekly price.
      4. Evaluates performance across all horizons h = 1..max_horizon and compares against a Naive Persistence Baseline.
    """
    city_df = df[df["city"].str.lower() == city_name.lower()].copy().sort_values("date")
    if city_df.empty:
        raise ValueError(f"No data for city {city_name}")

    # Full raw weekly ground truth series (Sunday-indexed to match scraping schedule)
    raw_weekly = city_df.set_index("date")["rent_price"].resample("W-SUN").mean()

    n = len(raw_weekly)
    # Guard against insufficient non-NaN observations
    actual_obs = int(raw_weekly.notna().sum())
    if actual_obs <= min_train:
        raise ValueError(f"Not enough actual observations ({actual_obs}) for backtesting with min_train={min_train}")

    horizon_metrics = {h: {"actual": [], "pred": [], "naive": [], "covered": []} for h in range(1, max_horizon + 1)}

    logging.info(f"Starting causal rolling-origin backtest across {n - min_train} origins for {city_name}...")

    for t in range(min_train - 1, n - 1):
        # Skip calendar weeks where no scrape occurred: using a NaN origin adds no new information
        if pd.isna(raw_weekly.iloc[t]):
            continue

        # Training slice strictly <= t (zero future data accessible)
        train_slice = raw_weekly.iloc[: t + 1]

        # Skip if there are fewer than min_train actual (non-NaN) observations in the training window
        if train_slice.notna().sum() < min_train:
            continue

        train_causal = train_slice.ffill()

        max_h = min(max_horizon, n - 1 - t)
        if max_h < 1:
            continue

        try:
            model = pm.auto_arima(
                train_causal,
                seasonal=False,
                stepwise=True,
                suppress_warnings=True,
                error_action='ignore'
            )
            fc, pi = model.predict(n_periods=max_h, return_conf_int=True, alpha=0.05)
            # Causal last known observation: use train_causal to avoid NaN when origin week t has no scrape
            last_known_price = train_causal.iloc[-1]

            for h in range(1, max_h + 1):
                actual_val = raw_weekly.iloc[t + h]

                # Skip forecast targets where actual observation or naive baseline is missing/invalid
                if pd.isna(actual_val) or not np.isfinite(actual_val) or pd.isna(last_known_price) or not np.isfinite(last_known_price):
                    continue

                pred_val = fc.iloc[h - 1] if hasattr(fc, 'iloc') else fc[h - 1]
                low_pi, high_pi = pi[h - 1]

                horizon_metrics[h]["actual"].append(actual_val)
                horizon_metrics[h]["pred"].append(pred_val)
                horizon_metrics[h]["naive"].append(last_known_price)
                horizon_metrics[h]["covered"].append(bool(low_pi <= actual_val <= high_pi))
        except Exception as e:
            logging.warning(f"Auto-ARIMA failed at origin index t={t}: {e}")

    report_by_horizon = {}
    all_arima_abs_errors = []
    all_naive_abs_errors = []

    print("\n" + "=" * 88)
    print(f"  CAUSAL MULTI-HORIZON ROLLING-ORIGIN BACKTEST RESULTS ({city_name.upper()})")
    print("=" * 88)
    print(f"Target: Raw Observed Weekly Rent (EUR/m2) | Min Train: {min_train} | Zero Look-Ahead Bias")
    print("-" * 88)
    print(f"{'Horizon':<10} | {'Origins (N)':<12} | {'ARIMA MAE':<11} | {'Naive MAE':<11} | {'ARIMA RMSE':<11} | {'95% PI Cov':<11} | {'vs Naive':<10}")
    print("-" * 88)

    for h in range(1, max_horizon + 1):
        y_true = np.array(horizon_metrics[h]["actual"])
        y_pred = np.array(horizon_metrics[h]["pred"])
        y_naive = np.array(horizon_metrics[h]["naive"])
        count = len(y_true)

        if count == 0:
            continue

        mae_a = float(np.mean(np.abs(y_true - y_pred)))
        mae_n = float(np.mean(np.abs(y_true - y_naive)))
        rmse_a = float(np.sqrt(np.mean((y_true - y_pred) ** 2)))
        rmse_n = float(np.sqrt(np.mean((y_true - y_naive) ** 2)))
        cov_pct = float(np.mean(horizon_metrics[h]["covered"]) * 100)
        imp_pct = float(((mae_n - mae_a) / mae_n) * 100) if mae_n > 0 else 0.0

        all_arima_abs_errors.extend(np.abs(y_true - y_pred))
        all_naive_abs_errors.extend(np.abs(y_true - y_naive))

        sign = "+" if imp_pct >= 0 else ""
        print(f"Week {h:<5} | {count:<12} | {mae_a:<11.4f} | {mae_n:<11.4f} | {rmse_a:<11.4f} | {cov_pct:<10.1f}% | {sign}{imp_pct:.1f}%")

        report_by_horizon[f"week_{h}"] = {
            "evaluatedOrigins": count,
            "arimaMAE": round(mae_a, 4),
            "naiveMAE": round(mae_n, 4),
            "arimaRMSE": round(rmse_a, 4),
            "naiveRMSE": round(rmse_n, 4),
            "empirical95PICoveragePct": round(cov_pct, 1),
            "improvementOverNaivePct": round(imp_pct, 2)
        }

    overall_mae_a = float(np.mean(all_arima_abs_errors)) if all_arima_abs_errors else 0.0
    overall_mae_n = float(np.mean(all_naive_abs_errors)) if all_naive_abs_errors else 0.0
    overall_imp = float(((overall_mae_n - overall_mae_a) / overall_mae_n) * 100) if overall_mae_n > 0 else 0.0
    sign = "+" if overall_imp >= 0 else ""

    print("-" * 88)
    print(f"Avg (1-5)  | {len(all_arima_abs_errors):<12} | {overall_mae_a:<11.4f} | {overall_mae_n:<11.4f} | {'N/A':<11} | {'N/A':<11} | {sign}{overall_imp:.1f}%")
    print("=" * 88)
    print("Statistical Interpretation & Methodological Context:")
    print("  * Target evaluated: Actual raw weekly observations without retrospective smoothing.")
    # Derive Week 1 summary dynamically from computed metrics
    w1 = report_by_horizon.get("week_1")
    if w1:
        w1_sign = "+" if w1["improvementOverNaivePct"] >= 0 else ""
        w1_verb = "outperforms" if w1["improvementOverNaivePct"] > 0 else "underperforms"
        print(f"  * Horizon Week 1 (1-week ahead): Auto-ARIMA {w1_verb} Naive persistence "
              f"(MAE {w1['arimaMAE']:.4f} vs {w1['naiveMAE']:.4f}, {w1_sign}{w1['improvementOverNaivePct']:.1f}%)")
        print(f"    with {w1['empirical95PICoveragePct']:.1f}% empirical coverage of the estimated 95% Prediction Interval"
              f" over {w1['evaluatedOrigins']} valid test origins.")
    print(f"  * Longer Horizons (Weeks 2-{max_horizon}): Under limited historical sample sizes ({min_train}-{n} training observations),")
    print("    naive persistence provides strong baseline competition, and prediction interval coverage contracts.")
    print(f"  * Sample size: Results are preliminary prototype evidence from {n} weekly observations, not definitive proof")
    print("    of long-horizon superiority across varied economic regimes.")
    print("=" * 88 + "\n")

    return {
        "city": city_name,
        "totalObservations": n,
        "minTrainObservations": min_train,
        "byHorizon": report_by_horizon,
        "cumulativeAverage": {
            "totalForecastEvaluations": len(all_arima_abs_errors),
            "arimaMAE": round(overall_mae_a, 4),
            "naiveMAE": round(overall_mae_n, 4),
            "improvementOverNaivePct": round(overall_imp, 2)
        }
    }


# Scraped Real Estate Market Benchmark Extraction
# Keys are accent-stripped (via strip_accents) so matching works for both accented and unaccented Greek input
PROPERTY_TYPE_MAP = {
    "διαμερισμα": "APARTMENT",
    "studio / γκαρσονιερα": "STUDIO",
    "γκαρσονιερα": "STUDIO",
    "στουντιο": "STUDIO",
    "μονοκατοικια": "HOUSE",
    "μεζονετα": "MAISONETTE",
    "λοφτ": "LOFT",
    "βιλα": "VILLA",
}

AREA_KEYWORDS = {
    "kolonaki": ["κολωνακ", "kolonaki", "λυκαβηττ"],
    "kypseli": ["κυψελ", "kypseli"],
    "pagkrati": ["παγκρατ", "pagkrati"],
    "ampelokipoi": ["αμπελοκηπ", "ampelokipoi"],
    "exarcheia": ["εξαρχει", "exarcheia"],
    "koukaki": ["κουκακ", "koukaki", "φιλοπαππ", "μακρυγιανν"],
    "neos_kosmos": ["νεος κοσμος", "νεου κοσμου", "νεο κοσμο", "neos kosmos"],
    "kallithea": ["καλλιθε", "kallithea"],
    "glyfada": ["γλυφαδ", "glyfada"],
    "kifisia": ["κηφισια", "kifisia"],
    "marousi": ["μαρουσ", "marousi"],
}


def matches_area(addr_clean: str, area_key: str) -> bool:
    """
    Checks if a normalized address matches a neighborhood area key.
    """
    if not addr_clean:
        return False
    aliases = AREA_KEYWORDS.get(area_key, [])
    if area_key == "kifisia":
        # Guard against Kifisias Avenue in Central Athens:
        # Exclude genitive street forms ("κηφισιας" / "kifisias") and avenue designations ("λεωφ")
        if "κηφισιας" in addr_clean or "kifisias" in addr_clean or "λεωφ" in addr_clean:
            return False
        for alias in aliases:
            norm_alias = strip_accents(alias)
            # Use negative lookbehind and lookahead to match full word forms and exclude "κηφισιας" / "kifisias"
            pattern = rf"(?<![a-zα-ω]){re.escape(norm_alias)}(?![ςs\w])"
            if re.search(pattern, addr_clean):
                return True
        return False

    normalized_aliases = [strip_accents(kw) for kw in aliases]
    return any(alias in addr_clean for alias in normalized_aliases)

# Domain prior factors per property type vs. overall market average
PROPERTY_TYPE_RELATIVE_FACTORS: Dict[str, float] = {
    "APARTMENT":  1.00,   # Baseline unit type for the Athens market
    "STUDIO":     1.20,   # Premium per m2 due to small size and high turnover demand
    "HOUSE":      0.87,   # Standalone houses are typically cheaper per m2 in urban Athens
    "MAISONETTE": 1.08,   # Slight premium for multi-level split-level units
    "LOFT":       1.12,   # Industrial-aesthetic premium
    "VILLA":      1.45,   # Significant premium for detached/luxury properties
    "OTHER":      1.00,   # No adjustment; treated as apartment baseline
}

# Domain prior factors for prime areas when sample count is low (<5)
PRIOR_AREA_FACTORS: Dict[str, float] = {
    "kolonaki": 1.25,
    "glyfada": 1.25,
    "kifisia": 1.25,
    "koukaki": 1.10,
    "pagkrati": 1.10,
    "marousi": 1.10,
    "kypseli": 0.95,
    "kallithea": 0.95,
    "ampelokipoi": 1.00,
    "exarcheia": 1.00,
    "neos_kosmos": 1.00,
}


def normalize_property_type(raw: Any) -> str:
    if pd.isna(raw) or not str(raw).strip():
        return "OTHER"
    # Strip accents so both accented and unaccented inputs match
    norm = strip_accents(raw)
    for grk, enum_val in PROPERTY_TYPE_MAP.items():
        if grk in norm:
            return enum_val
    upper = norm.upper()
    if upper in ["APARTMENT", "HOUSE", "STUDIO", "MAISONETTE", "LOFT", "VILLA"]:
        return upper
    return "OTHER"


def compute_area_weighted_ppm2(df_subset: pd.DataFrame) -> float:
    """Computes area-weighted mean rent/m2 if size column is present; falls back to simple mean."""
    if "clean_area" in df_subset.columns:
        valid_subset = df_subset.dropna(subset=["clean_ppm2", "clean_area"])
        total_area = valid_subset["clean_area"].sum()
        if total_area > 0:
            return float(round((valid_subset["clean_ppm2"] * valid_subset["clean_area"]).sum() / total_area, 2))
    return float(round(df_subset["clean_ppm2"].mean(), 2))


def compute_scraped_market_benchmarks(
    data_dir: Optional[Path] = None,
    df_hist: Optional[pd.DataFrame] = None
) -> Dict[str, Any]:
    """
    Processes active scraped real estate listings (spitogatos_athens_listings.csv) to extract
    high-fidelity market benchmarks (overall, by property type, and by neighborhood).
    Explicitly distinguishes:
      - Overall benchmark status (overallBenchmarkStatus: MEASURED vs ESTIMATED_FALLBACK)
      - Individual property-type status (status: MEASURED for n>=5 vs ESTIMATED_PRIOR)
      - Individual neighborhood status (status: MEASURED for n>=5 vs ESTIMATED_PRIOR)
      - Area-to-overall ratios derived strictly from the SAME active listing population to prevent cross-dataset bias.
      - Area-weighted mean pricing to align with time-series aggregation and prevent small-unit skew.
    """
    directory = data_dir or DATA_DIR

    # Compute baseline dynamically from the active historical series (reuses pre-loaded df_hist if provided)
    hist_baseline = _compute_historical_baseline("Athens", df=df_hist)
    default_avg = hist_baseline.get("overallAvgPricePerM2", 0.0)
    if default_avg <= 0:
        if df_hist is not None and not df_hist.empty:
            default_avg = float(round(df_hist["rent_price"].dropna().mean(), 2))
        else:
            try:
                df_h = load_historical_data()
                default_avg = float(round(df_h["rent_price"].dropna().mean(), 2))
            except Exception:
                default_avg = 14.50

    benchmarks: Dict[str, Any] = {
        "overallBenchmarkStatus": "ESTIMATED_FALLBACK",
        "benchmarkStatus": "ESTIMATED_FALLBACK",
        "totalScrapedListings": 0,
        "overallAvgPricePerM2": default_avg,
        "overallMedianPricePerM2": None,
        "overallP25PricePerM2": None,
        "overallP75PricePerM2": None,
        "detailedListingsCount": 0,
        "detailedListingsOverallAvgPerM2": default_avg,
        "byPropertyType": {},
        "byArea": {},
        "sizeTiers": {}
    }

    # Load active scraped listings (Spitogatos detailed listings)
    detailed_dfs = []
    spito_det = directory / "spitogatos_athens_listings.csv"
    if spito_det.exists():
        try:
            df_s = pd.read_csv(spito_det)
            detailed_dfs.append(df_s)
        except Exception as e:
            logging.warning(f"Failed to read {spito_det}: {e}")

    detailed_valid = pd.DataFrame()
    if detailed_dfs:
        combined_df = pd.concat(detailed_dfs, ignore_index=True)

        # Robust data cleaning: ensure clean_ppm2 is numeric and within realistic residential bounds
        if "price_per_m2" in combined_df.columns:
            combined_df["clean_ppm2"] = pd.to_numeric(combined_df["price_per_m2"], errors="coerce")
            combined_df["clean_ppm2"] = combined_df["clean_ppm2"].where(
                (combined_df["clean_ppm2"] >= 3.0) & (combined_df["clean_ppm2"] <= 80.0)
            )
        else:
            combined_df["clean_ppm2"] = pd.Series(dtype=float)

        # Area cleaning for area-weighted mean rent/m2
        area_col = None
        for col in ["Μέγεθος", "size", "area"]:
            if col in combined_df.columns:
                area_col = col
                break
        if area_col:
            combined_df["clean_area"] = pd.to_numeric(combined_df[area_col], errors="coerce")
            combined_df["clean_area"] = combined_df["clean_area"].where(
                (combined_df["clean_area"] >= 15.0) & (combined_df["clean_area"] <= 600.0)
            )
        else:
            combined_df["clean_area"] = pd.Series(dtype=float)

        # Drop rows missing clean_ppm2; also drop rows where clean_area is invalid when area is available,
        # to prevent NaN areas from polluting area-weighted metrics in the MEASURED threshold.
        drop_cols = ["clean_ppm2", "clean_area"] if area_col else ["clean_ppm2"]
        detailed_valid = combined_df.dropna(subset=drop_cols)
        if len(detailed_valid) >= 20:
            ppm2_clean = detailed_valid["clean_ppm2"]
            benchmarks["overallBenchmarkStatus"] = "MEASURED"
            benchmarks["benchmarkStatus"] = "MEASURED"
            benchmarks["totalScrapedListings"] = int(len(ppm2_clean))
            # Area-weighted average to align with ARIMA historical series and avoid small-unit skew
            benchmarks["overallAvgPricePerM2"] = compute_area_weighted_ppm2(detailed_valid)
            benchmarks["overallMedianPricePerM2"] = float(round(ppm2_clean.median(), 2))
            benchmarks["overallP25PricePerM2"] = float(round(ppm2_clean.quantile(0.25), 2))
            benchmarks["overallP75PricePerM2"] = float(round(ppm2_clean.quantile(0.75), 2))
            benchmarks["detailedListingsOverallAvgPerM2"] = benchmarks["overallAvgPricePerM2"]
            benchmarks["detailedListingsCount"] = int(len(ppm2_clean))

    overall_mean = benchmarks["overallAvgPricePerM2"]
    overall_median = benchmarks["overallMedianPricePerM2"] or overall_mean

    # Dynamically initialize property type stats using relative multipliers of current baseline
    type_stats = {
        ptype: {
            "avgPricePerM2": float(round(overall_mean * factor, 2)),
            "medianPricePerM2": float(round(overall_median * factor, 2)),
            "count": 0,
            "sampleCount": 0,
            "status": "ESTIMATED_PRIOR",
            "isMeasured": False
        }
        for ptype, factor in PROPERTY_TYPE_RELATIVE_FACTORS.items()
    }

    if not detailed_valid.empty:
        type_col = "Τύπος_ακινήτου" if "Τύπος_ακινήτου" in detailed_valid.columns else None
        if type_col:
            detailed_valid = detailed_valid.copy()
            detailed_valid["clean_type"] = detailed_valid[type_col].apply(normalize_property_type)

            for ptype, group in detailed_valid.groupby("clean_type"):
                cnt = int(len(group))
                if cnt >= 5:
                    type_stats[ptype] = {
                        "avgPricePerM2": compute_area_weighted_ppm2(group),
                        "medianPricePerM2": float(round(group["clean_ppm2"].median(), 2)),
                        "count": cnt,
                        "sampleCount": cnt,
                        "status": "MEASURED",
                        "isMeasured": True
                    }
                elif cnt > 0 and ptype in type_stats:
                    # For small samples (<5), retain robust domain prior price while updating observation count
                    type_stats[ptype]["count"] = cnt
                    type_stats[ptype]["sampleCount"] = cnt

        # Neighborhood / Area benchmarks (ratio computed strictly against SAME detailed dataset mean)
        if "address" in detailed_valid.columns:
            detailed_valid = detailed_valid.copy()
            detailed_valid["address_clean"] = detailed_valid["address"].apply(strip_accents)
            for area_key in AREA_KEYWORDS:
                mask = detailed_valid["address_clean"].apply(lambda a: matches_area(a, area_key))
                area_group = detailed_valid[mask]
                cnt = int(len(area_group))
                if cnt > 0:
                    is_empirical = cnt >= 5
                    area_mean = compute_area_weighted_ppm2(area_group) if is_empirical else float(round(area_group["clean_ppm2"].mean(), 2))
                    ratio_to_detailed = float(round(area_mean / overall_mean, 3)) if overall_mean > 0 else 1.0
                    benchmarks["byArea"][area_key] = {
                        "avgPricePerM2": area_mean,
                        "medianPricePerM2": float(round(area_group["clean_ppm2"].median(), 2)),
                        "count": cnt,
                        "sampleCount": cnt,
                        "status": "MEASURED" if is_empirical else "ESTIMATED_PRIOR",
                        "isMeasured": is_empirical,
                        "ratioToDetailedOverall": ratio_to_detailed
                    }

    benchmarks["byPropertyType"] = type_stats
    return benchmarks


# 4. End-to-End Prediction Model Generation & Export
def generate_price_prediction_model(
    history_filepath: Optional[str | Path] = None,
    data_dir: Optional[Path] = None,
    output_filepath: Optional[str | Path] = None,
    df_hist: Optional[pd.DataFrame] = None
) -> Dict[str, Any]:
    """
    Fits Auto-ARIMA on historical rent data (Spitogatos), aggregates market benchmarks from scraped listings,
    and produces a comprehensive prediction artifact for the Spring Boot backend and UI.
    """
    if df_hist is None:
        df_hist = load_historical_data(history_filepath)

    # ARIMA detailed forecast for Athens
    arima_details = get_city_forecast_details(df_hist, city_name="Athens", steps=5)
    if not arima_details:
        logging.warning("ARIMA fit produced no results, deriving data-driven statistical baseline from CSV history.")
        baseline = _compute_historical_baseline("Athens", df=df_hist)

        # Fail if the baseline is empty or produces an invalid price
        if not baseline or not (baseline.get("predictedPricePerM2") or 0) > 0:
            raise RuntimeError(
                "Cannot generate price prediction model: both ARIMA and the historical "
                "statistical baseline failed to produce a valid (positive) price for Athens. "
                "Ensure that historical CSV data contains valid Athens observations."
            )

        arima_details = {
            "city": "Athens",
            "modelStatus": "STATISTICAL_BASELINE",
            "confidenceLevel": None,
            "intervalType": baseline.get("intervalType", "HISTORICAL_VARIABILITY_SPREAD"),
            "intervalDescription": baseline.get("intervalDescription", "Historical +/- 1.96 standard deviation window over recent observations"),
            "lastHistoricalDate": baseline.get("lastHistoricalDate", ""),
            "lastHistoricalPricePerM2": baseline.get("lastHistoricalPricePerM2", 0.0),
            "predictedPricePerM2": baseline["predictedPricePerM2"],
            "lowerBoundPerM2": baseline.get("lowerBoundPerM2", 0.0),
            "upperBoundPerM2": baseline.get("upperBoundPerM2", 0.0),
            "trend": baseline.get("trend", "STABLE"),
            "trendPercentage": baseline.get("trendPercentage", 0.0),
            "horizonWeeks": 0,
            "modelOrder": baseline.get("modelOrder", "statistical-baseline (no ARIMA)"),
            "weeklyForecast": []
        }

    # Scraped data market benchmarks (passes df_hist so benchmarks use the same historical series)
    scraped_benchmarks = compute_scraped_market_benchmarks(data_dir=data_dir or DATA_DIR, df_hist=df_hist)

    model_payload = {
        "version": "1.0",
        "generatedAt": pd.Timestamp.now().isoformat(),
        "dataSource": "Spitogatos.gr scraped residential listings with Auto-ARIMA time series",
        "arimaForecast": arima_details,
        "scrapedMarketBenchmarks": scraped_benchmarks
    }

    # Refuse to export a model with a zero, negative, or non-finite predicted price.
    final_pred = arima_details.get("predictedPricePerM2", 0.0)
    if not (np.isfinite(final_pred) and final_pred > 0):
        raise RuntimeError(
            f"Model generation produced an invalid predictedPricePerM2={final_pred}. "
            "Refusing to export a broken prediction model. Check input data integrity."
        )

    # Save to outputs directory
    out_path = Path(output_filepath) if output_filepath else OUTPUTS_DIR / "price_predictions.json"
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(model_payload, f, indent=2, ensure_ascii=False)
    logging.info(f"Saved price prediction model JSON to {out_path}")

    return model_payload


# 5. Price Suggestion Algorithm (Micro-Adjustments Engine)
def calculate_price_suggestion(
    size_m2: float,
    property_type: str = "APARTMENT",
    address: str = "",
    floor: Optional[int] = None,
    year_built: Optional[int] = None,
    bedrooms: Optional[int] = None,
    bathrooms: Optional[int] = None,
    model_data: Optional[Dict[str, Any]] = None
) -> Dict[str, Any]:
    """
    Computes a tailored rent price suggestion for an owner given property features,
    synthesizing the Auto-ARIMA time series prediction and scraped market benchmarks.
    Incorporates size tier (with studio double-counting protection), floor, construction era,
    layout (bedrooms), bathrooms, and empirical neighborhood adjustments from the same dataset.
    """
    if size_m2 <= 0:
        raise ValueError("size_m2 must be greater than 0")

    if not model_data:
        json_path = OUTPUTS_DIR / "price_predictions.json"
        need_regen = True
        if json_path.exists():
            try:
                json_mtime = json_path.stat().st_mtime
                max_source_mtime = get_source_files_max_mtime(DATA_DIR)
                if max_source_mtime > json_mtime:
                    logging.info(f"Source CSV dataset modified after cached JSON ({max_source_mtime} > {json_mtime}). Retraining model.")
                    model_data = generate_price_prediction_model()
                else:
                    with open(json_path, "r", encoding="utf-8") as f:
                        model_data = json.load(f)
                need_regen = False
            except Exception as e:
                logging.warning(f"Error reading cached model or retraining: {e}")
                need_regen = True

        if need_regen:
            model_data = generate_price_prediction_model()

    arima = model_data.get("arimaForecast", {})
    benchmarks = model_data.get("scrapedMarketBenchmarks", {})
    by_type = benchmarks.get("byPropertyType", {})

    # 1. Baseline Price per m2: Blend ARIMA forecast and scraped data
    # Only fall back to a live disk read if the model data lacks a valid ARIMA prediction
    arima_ppm2_raw = arima.get("predictedPricePerM2", 0)
    if not (arima_ppm2_raw and np.isfinite(arima_ppm2_raw) and arima_ppm2_raw > 0):
        baseline_stats = _compute_historical_baseline("Athens")
        fallback_ppm2 = baseline_stats.get("predictedPricePerM2", 14.5)
    else:
        fallback_ppm2 = arima_ppm2_raw

    arima_ppm2 = arima.get("predictedPricePerM2", fallback_ppm2)
    scraped_avg = benchmarks.get("overallAvgPricePerM2", fallback_ppm2)

    norm_type = normalize_property_type(property_type)
    type_info = by_type.get(norm_type, by_type.get("APARTMENT", {}))
    type_avg_ppm2 = type_info.get("avgPricePerM2", scraped_avg)

    # Weighted baseline: 40% ARIMA time-series momentum + 60% scraped property-type average
    base_ppm2 = (0.40 * arima_ppm2) + (0.60 * type_avg_ppm2)

    adjustments_applied = []

    # 2. Size multiplier (heuristic thresholds)
    # Note: For STUDIO apartments, the property-type baseline factor (1.20) already incorporates
    # the small-footprint / high-demand premium. To prevent double-counting, the compact size
    # premiums (<35m2 and 35-55m2) apply only to non-studio units (APARTMENT, HOUSE, etc.).
    size_multiplier = 1.0
    if norm_type != "STUDIO":
        if size_m2 < 35:
            size_multiplier = 1.15
            adjustments_applied.append("+15% compact space premium (<35 m2)")
        elif size_m2 < 55:
            size_multiplier = 1.05
            adjustments_applied.append("+5% high-demand size (35-55 m2)")
        elif size_m2 > 120:
            size_multiplier = 0.90
            adjustments_applied.append("-10% large area volume discount (>120 m2)")
        elif size_m2 > 90:
            size_multiplier = 0.95
            adjustments_applied.append("-5% medium-large area adjustment (90-120 m2)")
    else:
        if size_m2 > 50:
            size_multiplier = 0.95
            adjustments_applied.append("-5% large studio volume adjustment (>50 m2)")

    # 3. Floor adjustment (heuristic thresholds based on urban Athens market)
    floor_multiplier = 1.0
    if floor is not None:
        if floor <= 0:
            floor_multiplier = 0.92
            adjustments_applied.append("-8% ground/basement floor")
        elif floor >= 4:
            floor_multiplier = 1.06
            adjustments_applied.append(f"+6% top/high floor view premium (floor {floor})")
        elif floor >= 2:
            floor_multiplier = 1.03
            adjustments_applied.append(f"+3% elevated floor premium (floor {floor})")

    # 4. Year built adjustment (construction era bands)
    year_multiplier = 1.0
    if year_built is not None and year_built > 1900:
        if year_built >= 2015:
            year_multiplier = 1.10
            adjustments_applied.append(f"+10% contemporary construction (built {year_built})")
        elif year_built >= 2000:
            year_multiplier = 1.05
            adjustments_applied.append(f"+5% modern build (built {year_built})")
        elif year_built < 1980:
            year_multiplier = 0.95
            adjustments_applied.append(f"-5% vintage building adjustment (built {year_built})")

    # 5. Bedrooms layout efficiency adjustment
    rooms_multiplier = 1.0
    if bedrooms is not None and bedrooms > 0:
        if bedrooms >= 3 and size_m2 >= 80:
            rooms_multiplier = 1.04
            adjustments_applied.append(f"+4% high room utility ({bedrooms} bedrooms)")
        elif bedrooms == 2 and size_m2 >= 55:
            rooms_multiplier = 1.02
            adjustments_applied.append("+2% functional layout (2 bedrooms)")
        elif bedrooms == 1 and size_m2 > 85:
            rooms_multiplier = 0.97
            adjustments_applied.append("-3% spacious 1-bedroom (lower room density)")

    # 6. Bathrooms convenience adjustment
    bath_multiplier = 1.0
    if bathrooms is not None and bathrooms >= 2:
        bath_multiplier = 1.04
        adjustments_applied.append(f"+4% multi-bathroom convenience ({bathrooms} baths)")

    # 7. Area / Neighborhood check in Athens (empirical data-driven adjustment from same dataset)
    area_multiplier = 1.0
    if address:
        addr_clean = strip_accents(address)
        by_area = benchmarks.get("byArea", {})
        detailed_overall_mean = benchmarks.get("detailedListingsOverallAvgPerM2", benchmarks.get("overallAvgPricePerM2", 14.37))
        for area_key in AREA_KEYWORDS:
            if matches_area(addr_clean, area_key):
                area_stat = by_area.get(area_key)
                if area_stat and area_stat.get("isMeasured", False):
                    # Area-to-overall ratio derived strictly from the SAME detailed listings dataset
                    measured_ratio = area_stat.get("ratioToDetailedOverall")
                    if measured_ratio is None and detailed_overall_mean > 0:
                        measured_ratio = area_stat["avgPricePerM2"] / detailed_overall_mean
                    else:
                        measured_ratio = measured_ratio or 1.0

                    prior_factor = PRIOR_AREA_FACTORS.get(area_key, 1.0)
                    # Blend 70% empirical scraped data with 30% prior domain factor
                    blended_factor = round(0.70 * measured_ratio + 0.30 * prior_factor, 2)
                    area_multiplier = max(0.75, min(1.45, blended_factor))
                    pct_diff = int(round((area_multiplier - 1.0) * 100))
                    sign = "+" if pct_diff >= 0 else ""
                    adjustments_applied.append(
                        f"{sign}{pct_diff}% empirical area adjustment ({area_key.capitalize()}, avg €{area_stat['avgPricePerM2']}/m2, n={area_stat['count']})"
                    )
                elif area_key in PRIOR_AREA_FACTORS:
                    area_multiplier = PRIOR_AREA_FACTORS[area_key]
                    pct_diff = int(round((area_multiplier - 1.0) * 100))
                    sign = "+" if pct_diff >= 0 else ""
                    adjustments_applied.append(f"{sign}{pct_diff}% location factor ({area_key.capitalize()}, domain prior)")
                break

    # Final combined price per m2
    final_ppm2 = (
        base_ppm2
        * size_multiplier
        * floor_multiplier
        * year_multiplier
        * rooms_multiplier
        * bath_multiplier
        * area_multiplier
    )
    suggested_price = int(round(final_ppm2 * size_m2))

    # Refuse to return a zero or negative suggestion to the caller
    if not (np.isfinite(final_ppm2) and final_ppm2 > 0 and suggested_price > 0):
        raise ValueError(
            f"Price suggestion calculation produced an invalid result "
            f"(final_ppm2={final_ppm2:.4f}, suggested_price={suggested_price}). "
            "This indicates missing or corrupt upstream data. Cannot suggest a rent price."
        )

    # Spread interval: heuristic estimated market range bounded between 8% and 15%
    upper_ci = arima.get("upperBoundPerM2", arima_ppm2 * 1.05)
    lower_ci = arima.get("lowerBoundPerM2", arima_ppm2 * 0.95)
    ci_spread = max(0.08, min(0.15, (upper_ci - lower_ci) / (2 * max(1.0, arima_ppm2))))
    min_price = int(round(suggested_price * (1.0 - ci_spread)))
    max_price = int(round(suggested_price * (1.0 + ci_spread)))
    margin_pct = round(ci_spread * 100, 1)

    total_comparables = benchmarks.get("totalScrapedListings", 0)
    comp_text = f"{total_comparables}+" if total_comparables > 0 else "all available"

    # Distinguish ARIMA projection from statistical baseline in user-facing explanation
    model_status = arima.get("modelStatus", "ARIMA")
    if model_status == "ARIMA":
        projection_text = f"Spitogatos Auto-ARIMA {arima.get('horizonWeeks', 5)}-week projection (€{arima_ppm2:.2f}/m2, {arima.get('trend', 'STABLE')} trend)"
    else:
        projection_text = f"Spitogatos statistical baseline rolling average (€{arima_ppm2:.2f}/m2, {arima.get('trend', 'STABLE')} trend)"

    explanation = (
        f"Suggested monthly rent of €{suggested_price} (€{final_ppm2:.2f}/m2) for a {size_m2} m2 {norm_type}. "
        f"Based on {projection_text} synthesized with {comp_text} scraped listings "
        f"(market avg €{type_avg_ppm2:.2f}/m2). Estimated market range is +/- {margin_pct}%."
    )

    property_type_status = type_info.get("status", "MEASURED" if type_info.get("count", 0) >= 5 else "ESTIMATED_PRIOR")

    return {
        "suggestedPrice": suggested_price,
        "suggestedPricePerM2": float(round(final_ppm2, 2)),
        "minPrice": min_price,
        "maxPrice": max_price,
        "minPricePerM2": float(round(min_price / size_m2, 2)),
        "maxPricePerM2": float(round(max_price / size_m2, 2)),
        "priceRangeType": "ESTIMATED_MARKET_RANGE",
        "marginPercentage": margin_pct,
        "marginDescription": "Heuristic market range bounded at +/- 8% to +/- 15% based on forecast volatility",
        "arimaForecast": {
            "modelStatus": model_status,
            "confidenceLevel": arima.get("confidenceLevel", 0.95 if model_status == "ARIMA" else None),
            "intervalType": "ARIMA_PREDICTION_INTERVAL" if model_status == "ARIMA" else "HISTORICAL_VARIABILITY_SPREAD",
            "intervalDescription": "Auto-ARIMA 95% prediction interval for future weekly observations" if model_status == "ARIMA" else "Historical +/- 1.96 standard deviation window over recent observations",
            "predictedPricePerM2": arima.get("predictedPricePerM2", arima_ppm2),
            "lowerBoundPerM2": arima.get("lowerBoundPerM2", arima_ppm2 * 0.9),
            "upperBoundPerM2": arima.get("upperBoundPerM2", arima_ppm2 * 1.1),
            "trend": arima.get("trend", "STABLE"),
            "trendPercentage": arima.get("trendPercentage", 0.0),
            "horizonWeeks": arima.get("horizonWeeks", 5 if model_status == "ARIMA" else 0)
        },
        "marketBenchmark": {
            "overallBenchmarkStatus": benchmarks.get("overallBenchmarkStatus", benchmarks.get("benchmarkStatus", "MEASURED")),
            "benchmarkStatus": benchmarks.get("overallBenchmarkStatus", benchmarks.get("benchmarkStatus", "MEASURED")),
            "avgPricePerM2": benchmarks.get("overallAvgPricePerM2", scraped_avg),
            "medianPricePerM2": benchmarks.get("overallMedianPricePerM2"),
            "propertyType": norm_type,
            "propertyTypeAvgPerM2": type_avg_ppm2,
            "propertyTypeStatus": property_type_status,
            "propertyTypeSampleCount": type_info.get("count", 0),
            "comparableCount": type_info.get("count", benchmarks.get("totalScrapedListings", 0)),
            "dataSource": "Scraped real estate listings (Spitogatos.gr) & Auto-ARIMA"
        },
        "appliedAdjustments": adjustments_applied,
        "explanation": explanation
    }


# 6. Main Execution Flow & CLI Interface
def main():
    parser = argparse.ArgumentParser(description="DRAS ARIMA Forecasting & Price Suggestion Service")
    parser.add_argument("--export-json", nargs="?", const=str(OUTPUTS_DIR / "price_predictions.json"),
                        help="Export full ARIMA forecast and scraped benchmarks to JSON")
    parser.add_argument("--suggest", action="store_true", help="Calculate a price suggestion for a listing")
    parser.add_argument("--size", type=float, help="Property size in m2")
    parser.add_argument("--type", type=str, default="APARTMENT", help="Property type (e.g. APARTMENT, STUDIO)")
    parser.add_argument("--address", type=str, default="", help="Property address / neighborhood")
    parser.add_argument("--floor", type=int, help="Floor number")
    parser.add_argument("--year", type=int, help="Year built")
    parser.add_argument("--bedrooms", type=int, help="Number of bedrooms")
    parser.add_argument("--bathrooms", type=int, help="Number of bathrooms")
    parser.add_argument("--backtest", action="store_true", help="Run causal rolling-origin out-of-sample backtesting for ARIMA")

    args = parser.parse_args()

    # Serve price suggestions directly without loading/parsing historical CSVs
    if args.suggest:
        if not args.size:
            print("Error: --size is required when using --suggest")
            sys.exit(1)
        suggestion = calculate_price_suggestion(
            size_m2=args.size,
            property_type=args.type,
            address=args.address,
            floor=args.floor,
            year_built=args.year,
            bedrooms=args.bedrooms,
            bathrooms=args.bathrooms
        )
        print(json.dumps(suggestion, indent=2, ensure_ascii=False))
        return

    df = load_historical_data()

    if args.backtest:
        evaluate_arima_backtest(df, city_name="Athens")
        return

    # Default flow: Run ARIMA forecast on active historical data, plot, and export JSON
    for city in df["city"].unique():
        forecast_city_rent(df, city)

    out_file = args.export_json or (OUTPUTS_DIR / "price_predictions.json")
    generate_price_prediction_model(output_filepath=out_file, df_hist=df)
    logging.info("ARIMA forecasting & price prediction model generation complete.")


if __name__ == "__main__":
    main()
