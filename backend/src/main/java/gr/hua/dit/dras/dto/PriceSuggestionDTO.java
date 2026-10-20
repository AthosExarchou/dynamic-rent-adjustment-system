package gr.hua.dit.dras.dto;

/* imports */
import java.util.ArrayList;
import java.util.List;

public class PriceSuggestionDTO {

    private Integer suggestedPrice;
    private Double suggestedPricePerM2;
    private Integer minPrice;
    private Integer maxPrice;
    private Double minPricePerM2;
    private Double maxPricePerM2;

    private String priceRangeType = "ESTIMATED_MARKET_RANGE";
    private Double marginPercentage;
    private String marginDescription;

    private ArimaForecastInfo arimaForecast;
    private MarketBenchmarkInfo marketBenchmark;
    private List<String> appliedAdjustments = new ArrayList<>();
    private String explanation;

    public PriceSuggestionDTO() {
    }

    public static class ArimaForecastInfo {
        private String modelStatus;
        private Double confidenceLevel;
        private String intervalType;
        private String intervalDescription;
        private Double predictedPricePerM2;
        private Double lowerBoundPerM2;
        private Double upperBoundPerM2;
        private String trend;
        private Double trendPercentage;
        private Integer horizonWeeks;

        public ArimaForecastInfo() {
        }

        public String getModelStatus() {
            return modelStatus;
        }

        public void setModelStatus(String modelStatus) {
            this.modelStatus = modelStatus;
        }

        public Double getConfidenceLevel() {
            return confidenceLevel;
        }

        public void setConfidenceLevel(Double confidenceLevel) {
            this.confidenceLevel = confidenceLevel;
        }

        public String getIntervalType() {
            return intervalType;
        }

        public void setIntervalType(String intervalType) {
            this.intervalType = intervalType;
        }

        public String getIntervalDescription() {
            return intervalDescription;
        }

        public void setIntervalDescription(String intervalDescription) {
            this.intervalDescription = intervalDescription;
        }

        public Double getPredictedPricePerM2() {
            return predictedPricePerM2;
        }

        public void setPredictedPricePerM2(Double predictedPricePerM2) {
            this.predictedPricePerM2 = predictedPricePerM2;
        }

        public Double getLowerBoundPerM2() {
            return lowerBoundPerM2;
        }

        public void setLowerBoundPerM2(Double lowerBoundPerM2) {
            this.lowerBoundPerM2 = lowerBoundPerM2;
        }

        public Double getUpperBoundPerM2() {
            return upperBoundPerM2;
        }

        public void setUpperBoundPerM2(Double upperBoundPerM2) {
            this.upperBoundPerM2 = upperBoundPerM2;
        }

        public String getTrend() {
            return trend;
        }

        public void setTrend(String trend) {
            this.trend = trend;
        }

        public Double getTrendPercentage() {
            return trendPercentage;
        }

        public void setTrendPercentage(Double trendPercentage) {
            this.trendPercentage = trendPercentage;
        }

        public Integer getHorizonWeeks() {
            return horizonWeeks;
        }

        public void setHorizonWeeks(Integer horizonWeeks) {
            this.horizonWeeks = horizonWeeks;
        }
    }

    public static class MarketBenchmarkInfo {
        private String overallBenchmarkStatus;
        private String benchmarkStatus;
        private Double avgPricePerM2;
        private Double medianPricePerM2;
        private String propertyType;
        private Double propertyTypeAvgPerM2;
        private String propertyTypeStatus;
        private Integer propertyTypeSampleCount;
        private Integer comparableCount;
        private String dataSource;

        public MarketBenchmarkInfo() {
        }

        public String getOverallBenchmarkStatus() {
            return overallBenchmarkStatus;
        }

        public void setOverallBenchmarkStatus(String overallBenchmarkStatus) {
            this.overallBenchmarkStatus = overallBenchmarkStatus;
        }

        public String getBenchmarkStatus() {
            return benchmarkStatus;
        }

        public void setBenchmarkStatus(String benchmarkStatus) {
            this.benchmarkStatus = benchmarkStatus;
        }

        public Double getAvgPricePerM2() {
            return avgPricePerM2;
        }

        public void setAvgPricePerM2(Double avgPricePerM2) {
            this.avgPricePerM2 = avgPricePerM2;
        }

        public Double getMedianPricePerM2() {
            return medianPricePerM2;
        }

        public void setMedianPricePerM2(Double medianPricePerM2) {
            this.medianPricePerM2 = medianPricePerM2;
        }

        public String getPropertyType() {
            return propertyType;
        }

        public void setPropertyType(String propertyType) {
            this.propertyType = propertyType;
        }

        public Double getPropertyTypeAvgPerM2() {
            return propertyTypeAvgPerM2;
        }

        public void setPropertyTypeAvgPerM2(Double propertyTypeAvgPerM2) {
            this.propertyTypeAvgPerM2 = propertyTypeAvgPerM2;
        }

        public String getPropertyTypeStatus() {
            return propertyTypeStatus;
        }

        public void setPropertyTypeStatus(String propertyTypeStatus) {
            this.propertyTypeStatus = propertyTypeStatus;
        }

        public Integer getPropertyTypeSampleCount() {
            return propertyTypeSampleCount;
        }

        public void setPropertyTypeSampleCount(Integer propertyTypeSampleCount) {
            this.propertyTypeSampleCount = propertyTypeSampleCount;
        }

        public Integer getComparableCount() {
            return comparableCount;
        }

        public void setComparableCount(Integer comparableCount) {
            this.comparableCount = comparableCount;
        }

        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }
    }

    public Integer getSuggestedPrice() {
        return suggestedPrice;
    }

    public void setSuggestedPrice(Integer suggestedPrice) {
        this.suggestedPrice = suggestedPrice;
    }

    public Double getSuggestedPricePerM2() {
        return suggestedPricePerM2;
    }

    public void setSuggestedPricePerM2(Double suggestedPricePerM2) {
        this.suggestedPricePerM2 = suggestedPricePerM2;
    }

    public Integer getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Integer minPrice) {
        this.minPrice = minPrice;
    }

    public Integer getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Integer maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Double getMinPricePerM2() {
        return minPricePerM2;
    }

    public void setMinPricePerM2(Double minPricePerM2) {
        this.minPricePerM2 = minPricePerM2;
    }

    public Double getMaxPricePerM2() {
        return maxPricePerM2;
    }

    public void setMaxPricePerM2(Double maxPricePerM2) {
        this.maxPricePerM2 = maxPricePerM2;
    }

    public String getPriceRangeType() {
        return priceRangeType;
    }

    public void setPriceRangeType(String priceRangeType) {
        this.priceRangeType = priceRangeType;
    }

    public Double getMarginPercentage() {
        return marginPercentage;
    }

    public void setMarginPercentage(Double marginPercentage) {
        this.marginPercentage = marginPercentage;
    }

    public String getMarginDescription() {
        return marginDescription;
    }

    public void setMarginDescription(String marginDescription) {
        this.marginDescription = marginDescription;
    }

    public ArimaForecastInfo getArimaForecast() {
        return arimaForecast;
    }

    public void setArimaForecast(ArimaForecastInfo arimaForecast) {
        this.arimaForecast = arimaForecast;
    }

    public MarketBenchmarkInfo getMarketBenchmark() {
        return marketBenchmark;
    }

    public void setMarketBenchmark(MarketBenchmarkInfo marketBenchmark) {
        this.marketBenchmark = marketBenchmark;
    }

    public List<String> getAppliedAdjustments() {
        return appliedAdjustments;
    }

    public void setAppliedAdjustments(List<String> appliedAdjustments) {
        this.appliedAdjustments = appliedAdjustments;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
