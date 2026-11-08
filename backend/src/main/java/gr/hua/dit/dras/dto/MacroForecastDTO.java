package gr.hua.dit.dras.dto;

/* imports */
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MacroForecastDTO {

    private String version;
    private String generatedAt;
    private String dataSource;
    private String sourceDatasetUrl;
    private String region;
    private MacroForecastInfo macroForecast;
    private SyncMetadataInfo syncMetadata;

    public MacroForecastDTO() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getSourceDatasetUrl() {
        return sourceDatasetUrl;
    }

    public void setSourceDatasetUrl(String sourceDatasetUrl) {
        this.sourceDatasetUrl = sourceDatasetUrl;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public MacroForecastInfo getMacroForecast() {
        return macroForecast;
    }

    public void setMacroForecast(MacroForecastInfo macroForecast) {
        this.macroForecast = macroForecast;
    }

    public SyncMetadataInfo getSyncMetadata() {
        return syncMetadata;
    }

    public void setSyncMetadata(SyncMetadataInfo syncMetadata) {
        this.syncMetadata = syncMetadata;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MacroForecastInfo {
        private String modelStatus;
        private String modelOrder;
        private String seasonalOrder;
        private Double predictionIntervalLevel;
        private Double aic;
        private Double bic;
        private Double aicc;
        private Double logLikelihood;
        private Double inSampleRmse;
        private Double inSampleMape;
        private Integer numObservations;
        private String latestHistoricalQuarter;
        private String latestHistoricalDate;
        private Double latestHistoricalIndex;
        private Double projectedEndIndex;
        private Double projectedTotalGrowthPercentage;
        private Double projectedAnnualGrowthRate;
        private String macroTrend;
        private Integer horizonQuarters;
        private List<HistoricalQuarterItem> recentHistoricalSeries = new ArrayList<>();
        private List<QuarterlyForecastItem> quarterlyForecast = new ArrayList<>();

        public MacroForecastInfo() {
        }

        public String getModelStatus() {
            return modelStatus;
        }

        public void setModelStatus(String modelStatus) {
            this.modelStatus = modelStatus;
        }

        public String getModelOrder() {
            return modelOrder;
        }

        public void setModelOrder(String modelOrder) {
            this.modelOrder = modelOrder;
        }

        public String getSeasonalOrder() {
            return seasonalOrder;
        }

        public void setSeasonalOrder(String seasonalOrder) {
            this.seasonalOrder = seasonalOrder;
        }

        public Double getPredictionIntervalLevel() {
            return predictionIntervalLevel;
        }

        public void setPredictionIntervalLevel(Double predictionIntervalLevel) {
            this.predictionIntervalLevel = predictionIntervalLevel;
        }

        public Double getAic() {
            return aic;
        }

        public void setAic(Double aic) {
            this.aic = aic;
        }

        public Double getBic() {
            return bic;
        }

        public void setBic(Double bic) {
            this.bic = bic;
        }

        public Double getAicc() {
            return aicc;
        }

        public void setAicc(Double aicc) {
            this.aicc = aicc;
        }

        public Double getLogLikelihood() {
            return logLikelihood;
        }

        public void setLogLikelihood(Double logLikelihood) {
            this.logLikelihood = logLikelihood;
        }

        public Double getInSampleRmse() {
            return inSampleRmse;
        }

        public void setInSampleRmse(Double inSampleRmse) {
            this.inSampleRmse = inSampleRmse;
        }

        public Double getInSampleMape() {
            return inSampleMape;
        }

        public void setInSampleMape(Double inSampleMape) {
            this.inSampleMape = inSampleMape;
        }

        public Integer getNumObservations() {
            return numObservations;
        }

        public void setNumObservations(Integer numObservations) {
            this.numObservations = numObservations;
        }

        public String getLatestHistoricalQuarter() {
            return latestHistoricalQuarter;
        }

        public void setLatestHistoricalQuarter(String latestHistoricalQuarter) {
            this.latestHistoricalQuarter = latestHistoricalQuarter;
        }

        public String getLatestHistoricalDate() {
            return latestHistoricalDate;
        }

        public void setLatestHistoricalDate(String latestHistoricalDate) {
            this.latestHistoricalDate = latestHistoricalDate;
        }

        public Double getLatestHistoricalIndex() {
            return latestHistoricalIndex;
        }

        public void setLatestHistoricalIndex(Double latestHistoricalIndex) {
            this.latestHistoricalIndex = latestHistoricalIndex;
        }

        public Double getProjectedEndIndex() {
            return projectedEndIndex;
        }

        public void setProjectedEndIndex(Double projectedEndIndex) {
            this.projectedEndIndex = projectedEndIndex;
        }

        public Double getProjectedTotalGrowthPercentage() {
            return projectedTotalGrowthPercentage;
        }

        public void setProjectedTotalGrowthPercentage(Double projectedTotalGrowthPercentage) {
            this.projectedTotalGrowthPercentage = projectedTotalGrowthPercentage;
        }

        public Double getProjectedAnnualGrowthRate() {
            return projectedAnnualGrowthRate;
        }

        public void setProjectedAnnualGrowthRate(Double projectedAnnualGrowthRate) {
            this.projectedAnnualGrowthRate = projectedAnnualGrowthRate;
        }

        public String getMacroTrend() {
            return macroTrend;
        }

        public void setMacroTrend(String macroTrend) {
            this.macroTrend = macroTrend;
        }

        public Integer getHorizonQuarters() {
            return horizonQuarters;
        }

        public void setHorizonQuarters(Integer horizonQuarters) {
            this.horizonQuarters = horizonQuarters;
        }

        public List<HistoricalQuarterItem> getRecentHistoricalSeries() {
            return recentHistoricalSeries;
        }

        public void setRecentHistoricalSeries(List<HistoricalQuarterItem> recentHistoricalSeries) {
            this.recentHistoricalSeries = recentHistoricalSeries;
        }

        public List<QuarterlyForecastItem> getQuarterlyForecast() {
            return quarterlyForecast;
        }

        public void setQuarterlyForecast(List<QuarterlyForecastItem> quarterlyForecast) {
            this.quarterlyForecast = quarterlyForecast;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoricalQuarterItem {
        private String quarter;
        private String date;
        private Double priceIndex;
        private Double qoqGrowthPercentage;
        private Double yoyGrowthPercentage;
        private String status;

        public HistoricalQuarterItem() {
        }

        public String getQuarter() {
            return quarter;
        }

        public void setQuarter(String quarter) {
            this.quarter = quarter;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Double getPriceIndex() {
            return priceIndex;
        }

        public void setPriceIndex(Double priceIndex) {
            this.priceIndex = priceIndex;
        }

        public Double getQoqGrowthPercentage() {
            return qoqGrowthPercentage;
        }

        public void setQoqGrowthPercentage(Double qoqGrowthPercentage) {
            this.qoqGrowthPercentage = qoqGrowthPercentage;
        }

        public Double getYoyGrowthPercentage() {
            return yoyGrowthPercentage;
        }

        public void setYoyGrowthPercentage(Double yoyGrowthPercentage) {
            this.yoyGrowthPercentage = yoyGrowthPercentage;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuarterlyForecastItem {
        private String quarter;
        private String date;
        private Double predictedPriceIndex;
        private Double lowerBoundIndex;
        private Double upperBoundIndex;
        private Double qoqGrowthPercentage;
        private Double yoyGrowthPercentage;

        public QuarterlyForecastItem() {
        }

        public String getQuarter() {
            return quarter;
        }

        public void setQuarter(String quarter) {
            this.quarter = quarter;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Double getPredictedPriceIndex() {
            return predictedPriceIndex;
        }

        public void setPredictedPriceIndex(Double predictedPriceIndex) {
            this.predictedPriceIndex = predictedPriceIndex;
        }

        public Double getLowerBoundIndex() {
            return lowerBoundIndex;
        }

        public void setLowerBoundIndex(Double lowerBoundIndex) {
            this.lowerBoundIndex = lowerBoundIndex;
        }

        public Double getUpperBoundIndex() {
            return upperBoundIndex;
        }

        public void setUpperBoundIndex(Double upperBoundIndex) {
            this.upperBoundIndex = upperBoundIndex;
        }

        public Double getQoqGrowthPercentage() {
            return qoqGrowthPercentage;
        }

        public void setQoqGrowthPercentage(Double qoqGrowthPercentage) {
            this.qoqGrowthPercentage = qoqGrowthPercentage;
        }

        public Double getYoyGrowthPercentage() {
            return yoyGrowthPercentage;
        }

        public void setYoyGrowthPercentage(Double yoyGrowthPercentage) {
            this.yoyGrowthPercentage = yoyGrowthPercentage;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SyncMetadataInfo {
        private String status;
        private String activeFile;
        private String latestAvailableQuarter;
        private String datasetPublicationDate;
        private String lastCheckedAt;
        private Integer checkIntervalDays;

        public SyncMetadataInfo() {
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getActiveFile() {
            return activeFile;
        }

        public void setActiveFile(String activeFile) {
            this.activeFile = activeFile;
        }

        public String getLatestAvailableQuarter() {
            return latestAvailableQuarter;
        }

        public void setLatestAvailableQuarter(String latestAvailableQuarter) {
            this.latestAvailableQuarter = latestAvailableQuarter;
        }

        public String getDatasetPublicationDate() {
            return datasetPublicationDate;
        }

        public void setDatasetPublicationDate(String datasetPublicationDate) {
            this.datasetPublicationDate = datasetPublicationDate;
        }

        public String getLastCheckedAt() {
            return lastCheckedAt;
        }

        public void setLastCheckedAt(String lastCheckedAt) {
            this.lastCheckedAt = lastCheckedAt;
        }

        public Integer getCheckIntervalDays() {
            return checkIntervalDays;
        }

        public void setCheckIntervalDays(Integer checkIntervalDays) {
            this.checkIntervalDays = checkIntervalDays;
        }
    }
}
