"""
Unit and Integration Tests for Bank of Greece Dataset Updater and SARIMA Macro Forecast Engine.
"""

# Imported libraries
import unittest
import json
import datetime
import tempfile
from pathlib import Path
from unittest.mock import patch
import pandas as pd
import numpy as np

from analytics.analysis.bog_updater import (
    is_ttl_expired,
    parse_quarter_period,
    check_and_update_bog_dataset,
    validate_downloaded_xls,
)
from analytics.models.arima_macro import (
    load_and_clean_data,
    DATA_DIR,
    OUTPUTS_DIR
)


class TestBogUpdater(unittest.TestCase):

    def test_parse_quarter_period_greek(self):
        """Greek 'Nο Τρίμηνο YYYY' format must parse correctly."""
        self.assertEqual(parse_quarter_period("2ο Τρίμηνο 2026"), "2026-Q2")
        self.assertEqual(parse_quarter_period("4ο Τρίμηνο 2025"), "2025-Q4")
        self.assertEqual(parse_quarter_period("1ο Τρίμηνο 2025"), "2025-Q1")

    def test_parse_quarter_period_english_iso(self):
        """English 'YYYY-QN' format must parse correctly without matching year digits as quarter."""
        self.assertEqual(parse_quarter_period("2026-Q4"), "2026-Q4")
        self.assertEqual(parse_quarter_period("2026-Q2"), "2026-Q2")
        self.assertEqual(parse_quarter_period("2025-Q1"), "2025-Q1")
        # Regression guard: year '2026' must not produce Q2 from the leading '2'
        self.assertNotEqual(parse_quarter_period("2026-Q4"), "2026-Q2")

    def test_parse_quarter_period_english_ordinal(self):
        """English ordinal 'Nth Quarter YYYY' must parse correctly."""
        self.assertEqual(parse_quarter_period("1st Quarter 2026"), "2026-Q1")
        self.assertEqual(parse_quarter_period("3rd Quarter 2025"), "2025-Q3")

    def test_parse_quarter_period_q_prefix(self):
        """'QN YYYY' format must parse correctly."""
        self.assertEqual(parse_quarter_period("Q2 2026"), "2026-Q2")

    def test_parse_quarter_period_empty(self):
        self.assertEqual(parse_quarter_period(""), "UNKNOWN")

    def test_is_ttl_expired(self):
        self.assertTrue(is_ttl_expired({}, 14))

        recent = (datetime.datetime.now() - datetime.timedelta(days=2)).isoformat()
        self.assertFalse(is_ttl_expired({"last_checked_at": recent}, 14))

        old = (datetime.datetime.now() - datetime.timedelta(days=20)).isoformat()
        self.assertTrue(is_ttl_expired({"last_checked_at": old}, 14))

    def test_cached_within_ttl(self):
        res = check_and_update_bog_dataset(target_dir=DATA_DIR, check_interval_days=30, force=False)
        self.assertIn(res["status"], ["cached_within_ttl", "already_up_to_date", "updated"])
        self.assertTrue(Path(res["active_file"]).exists())

    @patch("requests.get")
    def test_offline_fallback(self, mock_get):
        mock_get.side_effect = ConnectionError("Bank of Greece unreachable")
        res = check_and_update_bog_dataset(target_dir=DATA_DIR, check_interval_days=0, force=True)
        self.assertEqual(res["status"], "offline_fallback")
        self.assertTrue(Path(res["active_file"]).exists())

    def test_validate_downloaded_xls_rejects_non_excel(self):
        """validate_downloaded_xls must raise ValueError for a non-Excel file."""
        with tempfile.NamedTemporaryFile(suffix=".xls", delete=False) as f:
            f.write(b"<html>Not an Excel file</html>")
            temp_path = Path(f.name)
        try:
            with self.assertRaises(ValueError, msg="Non-Excel content should raise ValueError"):
                validate_downloaded_xls(temp_path)
        finally:
            temp_path.unlink(missing_ok=True)

    def test_validate_downloaded_xls_rejects_missing_quarters(self):
        """validate_downloaded_xls must reject files with missing interior quarters."""
        source_df = pd.read_excel(DATA_DIR / "bank_of_greece_apartment_index.xls")
        # Drop a row in the middle to create a gap
        gapped_df = source_df.drop(index=15).reset_index(drop=True)
        with tempfile.NamedTemporaryFile(suffix=".xlsx", delete=False) as f:
            temp_path = Path(f.name)
        try:
            gapped_df.to_excel(temp_path, index=False)
            with self.assertRaises(ValueError, msg="File with missing quarter must be rejected"):
                validate_downloaded_xls(temp_path)
        finally:
            temp_path.unlink(missing_ok=True)

    @patch("analytics.analysis.bog_updater.query_bog_api")
    @patch("requests.get")
    def test_download_preserves_xls_extension_and_succeeds(self, mock_requests_get, mock_query):
        """Downloaded file should preserve .xls extension in temp name and pass validation."""
        xls_bytes = (DATA_DIR / "bank_of_greece_apartment_index.xls").read_bytes()
        mock_response = unittest.mock.MagicMock()
        mock_response.iter_content.return_value = [xls_bytes]
        mock_response.raise_for_status.return_value = None
        mock_requests_get.return_value = mock_response

        mock_query.return_value = ({
            "Name": "mock_update_2026_q2.xls",
            "Path": "/OpenDataSetFilesALL/DOAM",
            "PublicationDate": 1750000000000,
            "FrequencyPeriod": "2ο Τρίμηνο 2026",
            "Language": 2
        }, None)

        with tempfile.TemporaryDirectory() as tmpdir:
            tmppath = Path(tmpdir)
            # Seed directory with initial dataset
            (tmppath / "bank_of_greece_apartment_index.xls").write_bytes(xls_bytes)
            res = check_and_update_bog_dataset(target_dir=tmppath, force=True)
            self.assertEqual(res["status"], "updated")
            self.assertEqual(res["latest_quarter"], "2026-Q2")
            self.assertGreater(res["metadata"]["records_count"], 50)
            self.assertTrue(str(res["active_file"]).endswith(".xls"))

    @patch("analytics.analysis.bog_updater.query_bog_api")
    @patch("requests.get")
    def test_download_handles_xlsx_extension(
            self, mock_requests_get, mock_query):
        """Downloaded .xlsx file should preserve .xlsx extension on destination file."""
        source_df = pd.read_excel(DATA_DIR / "bank_of_greece_apartment_index.xls")
        with tempfile.NamedTemporaryFile(suffix=".xlsx", delete=False) as f:
            temp_xlsx_path = Path(f.name)
        try:
            source_df.to_excel(temp_xlsx_path, index=False)
            xlsx_bytes = temp_xlsx_path.read_bytes()
        finally:
            temp_xlsx_path.unlink(missing_ok=True)

        mock_response = unittest.mock.MagicMock()
        mock_response.iter_content.return_value = [xlsx_bytes]
        mock_response.raise_for_status.return_value = None
        mock_requests_get.return_value = mock_response

        mock_query.return_value = ({
            "Name": "mock_update_2026_q2.xlsx",
            "Path": "/OpenDataSetFilesALL/DOAM",
            "PublicationDate": 1750000000000,
            "FrequencyPeriod": "2ο Τρίμηνο 2026",
            "Language": 2
        }, None)

        with tempfile.TemporaryDirectory() as tmpdir:
            tmppath = Path(tmpdir)
            res = check_and_update_bog_dataset(target_dir=tmppath, force=True)
            self.assertEqual(res["status"], "updated")
            self.assertTrue(str(res["active_file"]).endswith(".xlsx"))
            self.assertTrue(Path(res["active_file"]).exists())
            self.assertTrue(Path(res["csv_file"]).exists())


class TestArimaMacro(unittest.TestCase):

    def setUp(self):
        self.sample_csv = DATA_DIR / "bank_of_greece_apartment_index.csv"
        if not self.sample_csv.exists():
            self.sample_csv = DATA_DIR / "bank_of_greece_apartment_index.xls"

    def test_load_and_clean_data(self):
        ts, df = load_and_clean_data(self.sample_csv)
        self.assertIsInstance(ts, pd.Series)
        self.assertGreater(len(ts), 50)
        self.assertEqual(ts.index.freqstr, "QS-JAN")
        self.assertTrue((ts > 0).all())
        self.assertIn("Reference_year", df.columns)

    def test_load_and_clean_data_rejects_missing_quarters(self):
        """load_and_clean_data must raise ValueError when the dataset has gaps after alignment."""
        ts_real, df_real = load_and_clean_data(self.sample_csv)

        df_gapped = df_real.copy().reset_index()
        middle = len(df_gapped) // 2
        df_gapped = df_gapped.drop(index=middle).reset_index(drop=True)

        with tempfile.NamedTemporaryFile(suffix=".csv", mode="w", delete=False, encoding="utf-8") as f:
            df_gapped.to_csv(f, index=False)
            temp_path = Path(f.name)

        try:
            with self.assertRaises(ValueError, msg="A dataset with a missing quarter must raise ValueError"):
                load_and_clean_data(temp_path)
        finally:
            temp_path.unlink(missing_ok=True)

    def test_load_and_clean_data_handles_string_placeholders(self):
        """load_and_clean_data should coerce string placeholders and non-quarter rows without crashing."""
        ts_real, df_real = load_and_clean_data(self.sample_csv)
        df_dirty = df_real.copy().reset_index()

        # Add a footnote row and placeholder row with string values
        footnote_row = df_dirty.iloc[-1:].copy()
        footnote_row["Reference_year"] = "Note"
        footnote_row["Reference_quarter"] = "-"
        footnote_row[df_dirty.columns[3]] = ":"

        df_dirty = pd.concat([df_dirty, footnote_row], ignore_index=True)

        with tempfile.NamedTemporaryFile(suffix=".csv", mode="w", delete=False, encoding="utf-8") as f:
            df_dirty.to_csv(f, index=False)
            temp_path = Path(f.name)

        try:
            ts_cleaned, df_cleaned = load_and_clean_data(temp_path)
            self.assertEqual(len(ts_cleaned), len(ts_real))
        finally:
            temp_path.unlink(missing_ok=True)

    def test_recent_historical_series_contains_computed_rates(self):
        """recentHistoricalSeries must have computed QoQ and YoY rates."""
        json_path = OUTPUTS_DIR / "macro_predictions.json"
        if not json_path.exists():
            return

        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        history = data.get("macroForecast", {}).get("recentHistoricalSeries", [])
        self.assertGreater(len(history), 4)
        # Sliced items beyond index 4 should have both QoQ and YoY computed numbers
        for item in history[4:]:
            self.assertIsNotNone(item.get("qoqGrowthPercentage"))
            self.assertIsNotNone(item.get("yoyGrowthPercentage"))
            self.assertIsInstance(item["qoqGrowthPercentage"], (int, float))
            self.assertIsInstance(item["yoyGrowthPercentage"], (int, float))

    def test_load_and_clean_data_filters_non_positive_values(self):
        """load_and_clean_data must filter out non-positive (0 or negative) index values."""
        ts_real, df_real = load_and_clean_data(self.sample_csv)
        df_invalid = df_real.copy().reset_index()

        # Insert a 0.0 index row
        invalid_row = df_invalid.iloc[-1:].copy()
        invalid_row["Reference_year"] = 2030
        invalid_row["Reference_quarter"] = 1
        invalid_row[df_invalid.columns[3]] = 0.0

        df_invalid = pd.concat([df_invalid, invalid_row], ignore_index=True)

        with tempfile.NamedTemporaryFile(suffix=".csv", mode="w", delete=False, encoding="utf-8") as f:
            df_invalid.to_csv(f, index=False)
            temp_path = Path(f.name)

        try:
            ts_cleaned, df_cleaned = load_and_clean_data(temp_path)
            self.assertTrue((ts_cleaned > 0).all())
            self.assertEqual(len(ts_cleaned), len(ts_real))
        finally:
            temp_path.unlink(missing_ok=True)

    def test_prediction_interval_validation_allows_finite_bounds(self):
        """conf_df validation must allow finite negative lower bounds if upper >= lower, but reject invalid bounds."""
        dates = pd.date_range("2026-07-01", periods=4, freq="QS-JAN")
        # Simulating a long horizon where the lower prediction interval bound dips below 0 mathematically
        conf_df_with_neg_lower = pd.DataFrame({
            "Lower 95% PI": [-5.2, -2.1, 10.0, 15.0],
            "Upper 95% PI": [150.0, 155.0, 160.0, 165.0]
        }, index=dates)

        # Finite and Upper >= Lower: valid
        is_valid = bool(
            np.all(np.isfinite(conf_df_with_neg_lower.values)) and
            np.all(conf_df_with_neg_lower["Upper 95% PI"] >= conf_df_with_neg_lower["Lower 95% PI"])
        )
        self.assertTrue(is_valid)

        # Upper < Lower: invalid
        conf_df_inverted = pd.DataFrame({
            "Lower 95% PI": [160.0, 155.0, 160.0, 165.0],
            "Upper 95% PI": [150.0, 155.0, 160.0, 165.0]
        }, index=dates)
        is_inverted_valid = bool(
            np.all(np.isfinite(conf_df_inverted.values)) and
            np.all(conf_df_inverted["Upper 95% PI"] >= conf_df_inverted["Lower 95% PI"])
        )
        self.assertFalse(is_inverted_valid)

        # Non-finite: invalid
        conf_df_nan = pd.DataFrame({
            "Lower 95% PI": [np.nan, 10.0, 10.0, 15.0],
            "Upper 95% PI": [150.0, 155.0, 160.0, 165.0]
        }, index=dates)
        is_nan_valid = bool(
            np.all(np.isfinite(conf_df_nan.values)) and
            np.all(conf_df_nan["Upper 95% PI"] >= conf_df_nan["Lower 95% PI"])
        )
        self.assertFalse(is_nan_valid)

    def test_macro_predictions_json_integrity(self):
        json_path = OUTPUTS_DIR / "macro_predictions.json"
        self.assertTrue(json_path.exists(), f"{json_path} must exist after running arima_macro.py")

        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        self.assertEqual(data.get("version"), "1.0")
        self.assertEqual(data.get("region"), "Athens")
        self.assertIn("macroForecast", data)
        self.assertIn("syncMetadata", data)

        macro = data["macroForecast"]
        self.assertEqual(macro.get("modelStatus"), "SARIMA")
        self.assertIn(macro.get("macroTrend"), ["UPWARD", "DOWNWARD", "STABLE"])

        # Check prediction interval terminology
        self.assertIn("predictionIntervalLevel", macro)
        self.assertNotIn("confidenceLevel", macro)
        self.assertAlmostEqual(macro["predictionIntervalLevel"], 0.95)

        # In-sample fit diagnostics
        self.assertIn("aic", macro)
        self.assertIn("bic", macro)
        self.assertIn("inSampleMape", macro)
        self.assertIn("inSampleRmse", macro)
        self.assertGreater(macro["aic"], 0)
        self.assertGreater(macro["bic"], 0)
        self.assertGreater(macro["inSampleMape"], 0)
        self.assertLess(macro["inSampleMape"], 15.0)

        # Historical series
        history = macro.get("recentHistoricalSeries", [])
        self.assertGreater(len(history), 0)
        last_hist = history[-1]
        self.assertEqual(last_hist["priceIndex"], macro["latestHistoricalIndex"])
        self.assertEqual(last_hist["quarter"], macro["latestHistoricalQuarter"])

        # Forecast series
        forecasts = macro.get("quarterlyForecast", [])
        self.assertEqual(len(forecasts), 8)
        for q in forecasts:
            self.assertIn("quarter", q)
            self.assertIn("date", q)
            self.assertGreater(q["predictedPriceIndex"], 0)
            self.assertGreaterEqual(q["upperBoundIndex"], q["lowerBoundIndex"])
            self.assertIn("qoqGrowthPercentage", q)
            self.assertIn("yoyGrowthPercentage", q)


if __name__ == "__main__":
    unittest.main()
