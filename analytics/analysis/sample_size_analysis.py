# Imported Libraries
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import re
from pathlib import Path

# Configuration
POPULATION_CSV = Path("data/rentola_population.csv")
OUTPUT_PLOT = Path("outputs/sample_size_funnel.png")

ITERATIONS_PER_SIZE = 1000
TOLERANCE_RATIO = 0.05     # ±5%
STABILITY_CONSECUTIVE = 3  # requires 3 consecutive stable Ns
RANDOM_SEED = 42


# Helpers
def parse_numeric(x):
    if pd.isna(x):
        return np.nan
    s = str(x).strip()
    if s == "":
        return np.nan
    s = re.sub(r"[^\d.,\-]", "", s)
    if "." in s and "," in s:
        if s.rfind(".") < s.rfind(","):
            s = s.replace(".", "").replace(",", ".")
    elif "," in s and "." not in s:
        s = s.replace(",", ".")
    try:
        return float(s)
    except Exception:
        return np.nan


def calculate_weighted_avg(df_sample):
    valid = df_sample.dropna(subset=["price_per_m2", "area_numeric"])
    if valid.empty:
        return np.nan
    total_area = valid["area_numeric"].sum()
    if total_area == 0:
        return np.nan
    return (valid["price_per_m2"] * valid["area_numeric"]).sum() / total_area


def main():

    if not POPULATION_CSV.exists():
        print(f"Error: {POPULATION_CSV} not found.")
        return

    np.random.seed(RANDOM_SEED)

    print("Loading population data...")
    df_pop = pd.read_csv(POPULATION_CSV)

    # Clean
    df_pop["price_per_m2"] = pd.to_numeric(df_pop["price_per_m2"], errors="coerce")
    df_pop["area_numeric"] = df_pop["Μέγεθος"].apply(parse_numeric)

    df_pop = df_pop.dropna(subset=["price_per_m2", "area_numeric"])
    max_available = len(df_pop)

    if max_available < 50:
        print("Population too small for meaningful analysis.")
        return

    print(f"Valid population size: {max_available}")

    true_mean = calculate_weighted_avg(df_pop)
    print(f"True weighted mean: {true_mean:.4f} €/m²")

    # Dynamic sample sizes
    small = np.arange(20, 200, 20)
    medium = np.arange(200, 600, 50)
    large = np.arange(600, max_available + 1, 100)

    SAMPLE_SIZES = np.unique(np.concatenate([small, medium, large]))
    SAMPLE_SIZES = SAMPLE_SIZES[SAMPLE_SIZES <= max_available]

    results = {
        "sample_size": [],
        "mean_price": [],
        "std_error": [],
        "lower_bound": [],
        "upper_bound": [],
        "bias": [],
        "biased_low": [],
        "biased_high": []
    }

    print("\nRunning Monte Carlo simulation...")

    for size in SAMPLE_SIZES:

        print(f"Simulating N={size}")
        sample_means = []

        for _ in range(ITERATIONS_PER_SIZE):

            sample_df = df_pop.sample(
                n=size,
                replace=False,
                random_state=np.random.randint(0, 1_000_000)
            )

            avg = calculate_weighted_avg(sample_df)
            if not np.isnan(avg):
                sample_means.append(avg)

        if len(sample_means) < ITERATIONS_PER_SIZE * 0.9:
            print(f"Warning: High NaN rate at N={size}")

        sample_means = np.array(sample_means)

        # Monte Carlo statistics
        mean_est = np.mean(sample_means)
        std_error = np.std(sample_means, ddof=1)
        lower = np.percentile(sample_means, 5)
        upper = np.percentile(sample_means, 95)
        bias = (mean_est - true_mean) / true_mean

        # Pagination bias simulation
        sorted_low = df_pop.sort_values("price_per_m2").head(size)
        sorted_high = df_pop.sort_values("price_per_m2").tail(size)

        biased_low_avg = calculate_weighted_avg(sorted_low)
        biased_high_avg = calculate_weighted_avg(sorted_high)

        # Store
        results["sample_size"].append(size)
        results["mean_price"].append(mean_est)
        results["std_error"].append(std_error)
        results["lower_bound"].append(lower)
        results["upper_bound"].append(upper)
        results["bias"].append(bias)
        results["biased_low"].append(biased_low_avg)
        results["biased_high"].append(biased_high_avg)

    # Stability Detection
    stable_count = 0
    min_stable_n = None

    for i in range(len(results["sample_size"])):

        ci_width = results["upper_bound"][i] - results["lower_bound"][i]
        relative_width = ci_width / true_mean

        if relative_width <= TOLERANCE_RATIO:
            stable_count += 1
            if stable_count >= STABILITY_CONSECUTIVE:
                min_stable_n = results["sample_size"][i - (STABILITY_CONSECUTIVE - 1)]
                break
        else:
            stable_count = 0

    if min_stable_n:
        print(f"\nMinimum representative sample size (±{TOLERANCE_RATIO*100:.1f}% tolerance): {min_stable_n}")
    else:
        print("\nNo stable sample size found under tolerance criterion.")

    # SE Decay Exponent (log-log)
    log_n = np.log(results["sample_size"])
    log_se = np.log(results["std_error"])

    slope, _ = np.polyfit(log_n, log_se, 1)
    print(f"Empirical SE decay exponent: {slope:.3f} (ideal ≈ -0.5)")

    # Visualization
    print("\nGenerating plots...")
    OUTPUT_PLOT.parent.mkdir(exist_ok=True)

    plt.figure(figsize=(12, 7))

    # Monte Carlo interval
    plt.fill_between(
        results["sample_size"],
        results["lower_bound"],
        results["upper_bound"],
        alpha=0.3,
        label="Empirical 90% Interval"
    )

    plt.plot(
        results["sample_size"],
        results["mean_price"],
        marker="o",
        label="Monte Carlo Mean"
    )

    # True mean
    plt.axhline(
        y=true_mean,
        linestyle="--",
        linewidth=2,
        label=f"True Mean ({true_mean:.2f})"
    )

    # Pagination bias lines
    plt.plot(
        results["sample_size"],
        results["biased_low"],
        linestyle=":",
        label="Ascending Price Bias"
    )

    plt.plot(
        results["sample_size"],
        results["biased_high"],
        linestyle=":",
        label="Descending Price Bias"
    )

    plt.xlabel("Sample Size (N)")
    plt.ylabel("Weighted Average Price (€/m²)")
    plt.title("Sample Size Stability & Bias Analysis")
    plt.legend()
    plt.grid(True, linestyle=":")

    plt.tight_layout()
    plt.savefig(OUTPUT_PLOT, dpi=300)
    plt.show()

    print(f"Plot saved to {OUTPUT_PLOT}")


if __name__ == "__main__":
    main()
