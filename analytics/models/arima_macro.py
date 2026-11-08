"""
Dynamic Rent Adjustment System - Macroeconomic SARIMA Forecasting Module
-------------------------------------------------------------------------
SARIMA time-series forecasting on the Bank of Greece apartment price index for Athens.

- Auto-selects the SARIMA order by minimizing AIC over a stepwise search.
- Reports in-sample RMSE and MAPE as fit diagnostics.
- Generates 95% prediction intervals from the fitted state-space model.
- Exports only published Bank of Greece observations; refuses to run on datasets with gaps.
"""

# Imported libraries
from __future__ import annotations
import sys
import json
import logging
import argparse
import warnings
from pathlib import Path
from typing import Optional, Dict, Any, Tuple, List
import pandas as pd
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import pmdarima as pm

BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

try:
    from analysis.bog_updater import check_and_update_bog_dataset
except ImportError:
    from analytics.analysis.bog_updater import check_and_update_bog_dataset

DATA_DIR = BASE_DIR / "data"
OUTPUTS_DIR = BASE_DIR / "outputs"

DEFAULT_TARGET_COLUMN = "New_index_of_apartment_prices_by_geographical_area_Athens"
DEFAULT_FORECAST_PERIODS = 8  # 8 quarters = 2 years
DEFAULT_SEASON_LENGTH = 4     # 4 quarters per year (quarterly periodicity)

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)
warnings.filterwarnings("ignore", category=FutureWarning)
warnings.filterwarnings("ignore", category=UserWarning)


def load_and_clean_data(file_path: Path, target_column: str = DEFAULT_TARGET_COLUMN) -> Tuple[pd.Series, pd.DataFrame]:
    """
    Loads a Bank of Greece Excel (.xls) or CSV file and returns a quarterly time series.

    Raises ValueError if any quarters are missing after frequency alignment. Missing
    quarters are reported and the dataset is rejected rather than gap-filled.

    Returns:
        tuple[pd.Series, pd.DataFrame]: aligned quarterly series, cleaned dataframe
    """
    if not file_path.exists():
        raise FileNotFoundError(f"Dataset file not found at: {file_path}")

    if file_path.suffix.lower() in [".xls", ".xlsx"]:
        df = pd.read_excel(file_path, engine="xlrd" if file_path.suffix.lower() == ".xls" else "openpyxl")
    else:
        df = pd.read_csv(file_path)

    required_cols = ["Reference_year", "Reference_quarter", target_column]
    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        raise ValueError(f"Dataset missing required columns: {missing}. Available: {df.columns.tolist()}")

    for col in required_cols:
        df[col] = pd.to_numeric(df[col], errors="coerce")
    df = df.dropna(subset=required_cols).copy()
    df["Year"] = df["Reference_year"].astype(int)
    df["Quarter"] = df["Reference_quarter"].astype(int)
    df = df[df["Quarter"].isin([1, 2, 3, 4])].copy()
    df = df[df[target_column] > 0].copy()
    df["Month"] = (df["Quarter"] - 1) * 3 + 1
    df["Date"] = pd.to_datetime(df["Year"].astype(str) + "-" + df["Month"].astype(str).str.zfill(2) + "-01")

    df = df.sort_values("Date").drop_duplicates(subset=["Date"]).reset_index(drop=True)
    df = df.set_index("Date")

    ts = df[target_column].astype(float).asfreq("QS-JAN")

    missing_quarters = ts[ts.isna()].index.tolist()
    if missing_quarters:
        missing_str = ", ".join(str(q.date()) for q in missing_quarters)
        raise ValueError(
            f"Dataset is missing {len(missing_quarters)} quarterly observations "
            f"after frequency alignment: {missing_str}. Gaps must be resolved in source data."
        )

    logging.info(
        f"Loaded {len(ts)} quarterly records from {file_path.name} "
        f"({ts.index[0].date()} to {ts.index[-1].date()})"
    )
    return ts, df


def fit_sarima_model(
    time_series: pd.Series,
    season_length: int = DEFAULT_SEASON_LENGTH,
    summary_file: Optional[Path] = None
) -> Tuple[pm.ARIMA, Dict[str, Any]]:
    """
    Runs auto_arima stepwise search and returns the fitted model with goodness-of-fit metrics.

    inSampleRmse and inSampleMape are in-sample fit diagnostics that measure how well
    the model fits the training data, not how accurately it forecasts future quarters.
    """
    logging.info("Fitting optimal quarterly SARIMA model using stepwise AIC minimization...")
    model = pm.auto_arima(
        time_series,
        seasonal=True,
        m=season_length,
        stepwise=True,
        suppress_warnings=True,
        error_action="ignore",
        trace=False
    )

    # In-sample residuals and diagnostics excluding diffuse Kalman filter burn-in
    fitted_values = np.asarray(model.predict_in_sample())
    burn = getattr(model.arima_res_, "loglikelihood_burn", 0)
    if burn <= 0:
        burn = model.order[1] + (season_length * model.seasonal_order[1])

    valid_actual = time_series.values[burn:]
    valid_fitted = fitted_values[burn:]
    residuals = valid_actual - valid_fitted

    in_sample_rmse = float(np.sqrt(np.mean(residuals ** 2)))
    in_sample_mape = float(np.mean(np.abs(residuals / valid_actual)) * 100.0)

    # Statistical diagnostics directly from the fitted statsmodels wrapper
    aic_val = float(model.aic())
    bic_val = float(model.bic())
    aicc_val = float(model.aicc())
    
    # Try retrieving log-likelihood from statsmodels wrapper
    try:
        log_likelihood = float(model.arima_res_.llf)
    except Exception:
        log_likelihood = None

    metrics = {
        "modelOrder": str(model.order),
        "seasonalOrder": str(model.seasonal_order),
        "aic": round(aic_val, 3),
        "bic": round(bic_val, 3),
        "aicc": round(aicc_val, 3),
        "logLikelihood": round(log_likelihood, 3) if log_likelihood is not None else None,
        "inSampleRmse": round(in_sample_rmse, 4),
        "inSampleMape": round(in_sample_mape, 2),
        "numObservations": int(len(time_series))
    }

    logging.info(f"Model selected: SARIMA {metrics['modelOrder']} x {metrics['seasonalOrder']} (AIC: {metrics['aic']}, In-sample fit MAPE: {metrics['inSampleMape']}%)")

    if summary_file is not None:
        summary_file.parent.mkdir(parents=True, exist_ok=True)
        with open(summary_file, "w", encoding="utf-8") as f:
            f.write(str(model.summary()))
        logging.info(f"Saved model summary to {summary_file}")

    return model, metrics


def generate_forecast(
    model: pm.ARIMA,
    periods: int,
    last_date: pd.Timestamp
) -> Tuple[pd.Series, pd.DataFrame]:
    """
    Returns point forecasts and 95% prediction intervals for the next `periods` quarters.

    model.predict(return_conf_int=True) returns prediction intervals (uncertainty around
    a future observation, including forecast-error variance), not confidence intervals
    for the conditional mean.
    """
    forecasts, conf_int = model.predict(n_periods=periods, return_conf_int=True, alpha=0.05)
    forecast_index = pd.date_range(start=last_date, periods=periods + 1, freq="QS-JAN")[1:]

    forecast_series = pd.Series(forecasts, index=forecast_index)
    conf_df = pd.DataFrame(conf_int, index=forecast_index, columns=["Lower 95% PI", "Upper 95% PI"])

    return forecast_series, conf_df


def extract_recent_historical_observations(df: pd.DataFrame, target_column: str, limit: int = 16) -> List[Dict[str, Any]]:
    """Returns the most recent `limit` rows from the Bank of Greece dataframe as a list of dicts."""
    full_series = df[target_column].astype(float)
    computed_qoq = full_series.pct_change(periods=1) * 100.0
    computed_yoy = full_series.pct_change(periods=4) * 100.0

    recent_df = df.tail(limit)
    historical_list = []

    for dt, row in recent_df.iterrows():
        year = int(row["Year"])
        quarter = int(row["Quarter"])
        idx_val = round(float(row[target_column]), 2)

        qoq_val = computed_qoq.loc[dt]
        qoq_pct = round(float(qoq_val), 2) if pd.notna(qoq_val) else None

        yoy_val = computed_yoy.loc[dt]
        yoy_pct = round(float(yoy_val), 2) if pd.notna(yoy_val) else None

        status = str(row["Status"]) if "Status" in row and pd.notna(row["Status"]) else "Published"

        historical_list.append({
            "quarter": f"{year}-Q{quarter}",
            "date": dt.strftime("%Y-%m-%d"),
            "priceIndex": idx_val,
            "qoqGrowthPercentage": qoq_pct,
            "yoyGrowthPercentage": yoy_pct,
            "status": status
        })

    return historical_list


def build_macro_predictions_payload(
    historical_ts: pd.Series,
    raw_df: pd.DataFrame,
    forecast_series: pd.Series,
    conf_df: pd.DataFrame,
    model: pm.ARIMA,
    model_metrics: Dict[str, Any],
    sync_report: Dict[str, Any],
    target_column: str = DEFAULT_TARGET_COLUMN
) -> Dict[str, Any]:
    """Builds the macro prediction payload from historical series and forecasts."""
    last_hist_date = historical_ts.index[-1]
    last_hist_val = round(float(historical_ts.iloc[-1]), 2)
    final_forecast_val = round(float(forecast_series.iloc[-1]), 2)

    total_change_pct = round(((final_forecast_val - last_hist_val) / last_hist_val) * 100.0, 2)
    years_horizon = len(forecast_series) / 4.0
    annualized_growth = round((((final_forecast_val / last_hist_val) ** (1.0 / max(0.25, years_horizon))) - 1.0) * 100.0, 2)

    if annualized_growth > 1.0:
        macro_trend = "UPWARD"
    elif annualized_growth < -1.0:
        macro_trend = "DOWNWARD"
    else:
        macro_trend = "STABLE"

    # Concatenate historical and forecasted series for exact lag-4 (YoY) lookups
    combined_ts = pd.concat([historical_ts, forecast_series])

    quarterly_forecast = []
    prev_val = last_hist_val

    for i, (dt, pred_val) in enumerate(forecast_series.items()):
        val = round(float(pred_val), 2)
        low = round(float(conf_df.iloc[i]["Lower 95% PI"]), 2)
        high = round(float(conf_df.iloc[i]["Upper 95% PI"]), 2)

        qoq_growth = round(((val - prev_val) / prev_val) * 100.0, 2)

        # 4-quarter lag lookup from combined series for YoY calculation
        pos = combined_ts.index.get_loc(dt)
        if pos >= 4:
            yoy_base = float(combined_ts.iloc[pos - 4])
            yoy_growth = round(((val - yoy_base) / yoy_base) * 100.0, 2)
        else:
            yoy_growth = None

        quarter_str = f"{dt.year}-Q{(dt.month - 1) // 3 + 1}"

        quarterly_forecast.append({
            "quarter": quarter_str,
            "date": dt.strftime("%Y-%m-%d"),
            "predictedPriceIndex": val,
            "lowerBoundIndex": low,
            "upperBoundIndex": high,
            "qoqGrowthPercentage": qoq_growth,
            "yoyGrowthPercentage": yoy_growth
        })
        prev_val = val

    latest_quarter_str = f"{last_hist_date.year}-Q{(last_hist_date.month - 1) // 3 + 1}"
    recent_history = extract_recent_historical_observations(raw_df, target_column, limit=16)

    return {
        "version": "1.0",
        "generatedAt": pd.Timestamp.now().isoformat(),
        "dataSource": "Bank of Greece Open Data (Dataset 5: Index of Apartment Prices by Geographical Area)",
        "sourceDatasetUrl": "https://opendata.bankofgreece.gr/en/dataset/5",
        "region": "Athens",
        "macroForecast": {
            "modelStatus": "SARIMA",
            "modelOrder": model_metrics["modelOrder"],
            "seasonalOrder": model_metrics["seasonalOrder"],
            "predictionIntervalLevel": 0.95,
            "aic": model_metrics["aic"],
            "bic": model_metrics["bic"],
            "aicc": model_metrics["aicc"],
            "logLikelihood": model_metrics["logLikelihood"],
            "inSampleRmse": model_metrics["inSampleRmse"],
            "inSampleMape": model_metrics["inSampleMape"],
            "numObservations": model_metrics["numObservations"],
            "latestHistoricalQuarter": latest_quarter_str,
            "latestHistoricalDate": last_hist_date.strftime("%Y-%m-%d"),
            "latestHistoricalIndex": last_hist_val,
            "projectedEndIndex": final_forecast_val,
            "projectedTotalGrowthPercentage": total_change_pct,
            "projectedAnnualGrowthRate": annualized_growth,
            "macroTrend": macro_trend,
            "horizonQuarters": len(forecast_series),
            "recentHistoricalSeries": recent_history,
            "quarterlyForecast": quarterly_forecast
        },
        "syncMetadata": {
            "status": sync_report.get("status", "unknown"),
            "activeFile": str(Path(sync_report.get("active_file", "")).name) if sync_report.get("active_file") else None,
            "latestAvailableQuarter": sync_report.get("latest_quarter"),
            "datasetPublicationDate": sync_report.get("publication_date"),
            "lastCheckedAt": sync_report.get("metadata", {}).get("last_checked_at"),
            "checkIntervalDays": sync_report.get("metadata", {}).get("check_interval_days", 14)
        }
    }


def plot_forecast(
    historical: pd.Series,
    forecast: pd.Series,
    conf_int: pd.DataFrame,
    region_name: str = "Athens",
    save_path: Optional[Path] = None,
    show: bool = False
) -> None:
    """
    Renders historical and forecasted apartment price index curves with confidence bands.
    """
    fig, ax = plt.subplots(figsize=(12, 6), dpi=150)

    plot_hist = historical.iloc[-40:] if len(historical) > 40 else historical
    ax.plot(plot_hist.index, plot_hist.values, label=f"Bank of Greece Historical Index ({region_name})", color="#1e3a8a", linewidth=2.2)
    ax.plot(forecast.index, forecast.values, label="SARIMA 2-Year Forecast", color="#dc2626", linestyle="--", linewidth=2.2)

    ax.fill_between(
        forecast.index,
        conf_int["Lower 95% PI"],
        conf_int["Upper 95% PI"],
        color="#dc2626",
        alpha=0.15,
        label="95% Prediction Interval"
    )

    ax.set_title(f"Bank of Greece Apartment Price Index Forecast - {region_name.upper()} (Quarterly)", fontsize=14, fontweight="bold", pad=12)
    ax.set_xlabel("Quarter", fontsize=11)
    ax.set_ylabel("Price Index Level (2007=100)", fontsize=11)
    ax.grid(True, linestyle=":", alpha=0.6)
    ax.legend(loc="upper left", frameon=True)
    fig.tight_layout()

    if save_path:
        save_path.parent.mkdir(parents=True, exist_ok=True)
        fig.savefig(save_path, dpi=300)
        logging.info(f"Saved forecast plot to: {save_path}")

    if show:
        plt.show()
    plt.close(fig)


def main() -> None:
    parser = argparse.ArgumentParser(description="Bank of Greece Apartment Price Index SARIMA Forecasting Microservice")
    parser.add_argument("--force-update", action="store_true", help="Force re-checking and downloading latest dataset from Bank of Greece API")
    parser.add_argument("--no-update", action="store_true", help="Skip Bank of Greece API check and use local cached file immediately")
    parser.add_argument("--check-interval-days", type=int, default=14, help="Days before re-checking Bank of Greece API (default: 14)")
    parser.add_argument("--export-json", nargs="?", const=str(OUTPUTS_DIR / "macro_predictions.json"), default=str(OUTPUTS_DIR / "macro_predictions.json"), help="Export macro predictions to JSON file")
    parser.add_argument("--periods", type=int, default=DEFAULT_FORECAST_PERIODS, help="Forecast horizon in quarters (default: 8)")
    parser.add_argument("--show-plot", action="store_true", help="Display matplotlib GUI window interactively")
    parser.add_argument("--summary", action="store_true", help="Print forecast metrics table to console")

    args = parser.parse_args()
    OUTPUTS_DIR.mkdir(parents=True, exist_ok=True)

    if args.no_update:
        local_file = None
        for candidate in [
            DATA_DIR / "bank_of_greece_apartment_index.csv",
            DATA_DIR / "bank_of_greece_apartment_index.xlsx",
            DATA_DIR / "bank_of_greece_apartment_index.xls",
        ]:
            if candidate.exists():
                local_file = candidate
                break

        if local_file is None:
            raise FileNotFoundError("No local Bank of Greece dataset found.")

        sync_report = {
            "status": "skipped_by_flag",
            "active_file": local_file,
            "csv_file": DATA_DIR / "bank_of_greece_apartment_index.csv" if (DATA_DIR / "bank_of_greece_apartment_index.csv").exists() else None,
            "latest_quarter": "LOCAL",
            "publication_date": "LOCAL",
            "metadata": {}
        }
        logging.info(f"Bypassing Bank of Greece API check (--no-update specified). Using {local_file.name}")
    else:
        sync_report = check_and_update_bog_dataset(
            target_dir=DATA_DIR,
            check_interval_days=args.check_interval_days,
            force=args.force_update
        )

    # Prefer normalized clean CSV if available, otherwise use active Excel file
    active_file = None
    if sync_report.get("csv_file") and Path(sync_report["csv_file"]).exists():
        active_file = Path(sync_report["csv_file"])
    elif sync_report.get("active_file") and Path(sync_report["active_file"]).exists():
        active_file = Path(sync_report["active_file"])
    else:
        for candidate in [
            DATA_DIR / "bank_of_greece_apartment_index.csv",
            DATA_DIR / "bank_of_greece_apartment_index.xlsx",
            DATA_DIR / "bank_of_greece_apartment_index.xls",
        ]:
            if candidate.exists():
                active_file = candidate
                break

    if active_file is None or not active_file.exists():
        raise FileNotFoundError("No valid Bank of Greece dataset found")

    ts_data, raw_df = load_and_clean_data(active_file, DEFAULT_TARGET_COLUMN)
    summary_path = OUTPUTS_DIR / "SARIMA_Model_Summary.txt"
    sarima_model, model_metrics = fit_sarima_model(ts_data, DEFAULT_SEASON_LENGTH, summary_file=summary_path)

    forecast_series, conf_df = generate_forecast(sarima_model, args.periods, ts_data.index[-1])

    # Enforce strictly positive, finite values on forecasts and valid prediction bounds
    if not (np.all(np.isfinite(forecast_series.values)) and np.all(forecast_series.values > 0)):
        raise RuntimeError("SARIMA produced non-finite or non-positive index forecasts; refusing to export.")
    if not (np.all(np.isfinite(conf_df.values)) and np.all(conf_df["Upper 95% PI"] >= conf_df["Lower 95% PI"])):
        raise RuntimeError("SARIMA produced non-finite or invalid prediction intervals; refusing to export.")

    payload = build_macro_predictions_payload(
        ts_data, raw_df, forecast_series, conf_df, sarima_model, model_metrics, sync_report, DEFAULT_TARGET_COLUMN
    )

    if args.export_json:
        export_path = Path(args.export_json)
        export_path.parent.mkdir(parents=True, exist_ok=True)
        with open(export_path, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2, allow_nan=False)
        logging.info(f"Exported macroeconomic predictions JSON to: {export_path}")

    plot_path = OUTPUTS_DIR / "macro_forecast_plot.png"
    plot_forecast(ts_data, forecast_series, conf_df, "Athens", save_path=plot_path, show=args.show_plot)

    macro = payload["macroForecast"]
    print("\n" + "=" * 75)
    print(f"BANK OF GREECE RESIDENTIAL MACRO FORECAST: {payload['region'].upper()}")
    print("=" * 75)
    print(f"Fitted Model: SARIMA {macro['modelOrder']} x {macro['seasonalOrder']}")
    print(f"Goodness of Fit: AIC = {macro['aic']} | BIC = {macro['bic']} | In-Sample Fit MAPE = {macro['inSampleMape']}% (fit diagnostic, not forecast accuracy)")
    print(f"Latest Historical Quarter: {macro['latestHistoricalQuarter']} (Index: {macro['latestHistoricalIndex']})")
    print(f"Projected 2-Year Horizon: {macro['projectedEndIndex']} ({macro['macroTrend']}, {macro['projectedAnnualGrowthRate']}% / year)")
    print("-" * 75)
    print("Quarterly Projections:")
    for q in macro["quarterlyForecast"]:
        yoy_str = f"{q['yoyGrowthPercentage']:+0.2f}%" if q['yoyGrowthPercentage'] is not None else "N/A"
        print(f"  {q['quarter']} ({q['date']}): {q['predictedPriceIndex']} "
              f"[95% PI: {q['lowerBoundIndex']} - {q['upperBoundIndex']}] (QoQ: {q['qoqGrowthPercentage']:+0.2f}%, YoY: {yoy_str})")
    print("=" * 75 + "\n")


if __name__ == "__main__":
    main()
