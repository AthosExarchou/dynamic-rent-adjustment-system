package gr.hua.dit.dras.services.domain;

/* imports */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.hua.dit.dras.dto.PriceSuggestionDTO;
import gr.hua.dit.dras.dto.PriceSuggestionRequestDTO;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.model.enums.PropertyType;
import gr.hua.dit.dras.repositories.ListingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class PriceSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(PriceSuggestionService.class);

    private final ListingRepository listingRepository;
    private final ObjectMapper objectMapper;

    private JsonNode cachedModelNode;

    private static final Map<String, List<String>> AREA_KEYWORDS = Map.ofEntries(
            Map.entry("kolonaki", List.of("κολωνάκι", "kolonaki", "λυκαβηττ")),
            Map.entry("glyfada", List.of("γλυφάδα", "glyfada")),
            Map.entry("kifisia", List.of("κηφισιά", "kifisia")),
            Map.entry("koukaki", List.of("κουκάκι", "koukaki", "φιλοπάππου", "μακρυγιάννη")),
            Map.entry("pagkrati", List.of("παγκράτι", "pagkrati", "παγκρατι")),
            Map.entry("marousi", List.of("μαρούσι", "marousi")),
            Map.entry("kypseli", List.of("κυψέλη", "kypseli")),
            Map.entry("kallithea", List.of("καλλιθέα", "kallithea")),
            Map.entry("ampelokipoi", List.of("αμπελόκηποι", "ampelokipoi", "αμπελοκηποι")),
            Map.entry("exarcheia", List.of("εξάρχεια", "exarcheia")),
            Map.entry("neos_kosmos", List.of("νέος κόσμος", "neos kosmos"))
    );

    private static final Map<String, Double> PRIOR_AREA_FACTORS = Map.ofEntries(
            Map.entry("kolonaki", 1.25),
            Map.entry("glyfada", 1.25),
            Map.entry("kifisia", 1.25),
            Map.entry("koukaki", 1.10),
            Map.entry("pagkrati", 1.10),
            Map.entry("marousi", 1.10),
            Map.entry("kypseli", 0.95),
            Map.entry("kallithea", 0.95),
            Map.entry("ampelokipoi", 1.00),
            Map.entry("exarcheia", 1.00),
            Map.entry("neos_kosmos", 1.00)
    );

    private long lastLoadedTimestamp = 0;
    private File activeModelFile;

    public PriceSuggestionService(ListingRepository listingRepository, ObjectMapper objectMapper) {
        this.listingRepository = listingRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadModelData();
    }

    /**
     * Loads the ARIMA and scraped real estate prediction model.
     * Looks first at the analytics/outputs directory on disk, then falls back to classpath.
     */
    public synchronized void loadModelData() {
        try {
            // Check filesystem relative paths (for development/analytics integration)
            List<Path> candidatePaths = List.of(
                    Paths.get("analytics", "outputs", "price_predictions.json"),
                    Paths.get("..", "analytics", "outputs", "price_predictions.json"),
                    Paths.get("outputs", "price_predictions.json")
            );

            for (Path candidate : candidatePaths) {
                File file = candidate.toFile();
                if (file.exists() && file.canRead()) {
                    cachedModelNode = objectMapper.readTree(file);
                    activeModelFile = file;
                    lastLoadedTimestamp = file.lastModified();
                    log.info("Loaded price prediction model from filesystem: {}", file.getAbsolutePath());
                    return;
                }
            }

            // Fallback to classpath resource
            ClassPathResource resource = new ClassPathResource("data/price_predictions.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    cachedModelNode = objectMapper.readTree(is);
                    activeModelFile = null;
                    lastLoadedTimestamp = System.currentTimeMillis();
                    log.info("Loaded price prediction model from classpath: data/price_predictions.json");
                    return;
                }
            }

            log.warn("Price predictions model JSON file not found. Built-in analytical defaults will be used.");
        } catch (Exception e) {
            log.error("Failed to load price prediction model JSON: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if the prediction model JSON file on disk has been updated (e.g. by weekly retraining)
     * and automatically reloads it into memory so suggestions stay live without server restarts.
     */
    private void checkAndReloadModelData() {
        if (activeModelFile != null && activeModelFile.exists()) {
            if (activeModelFile.lastModified() > lastLoadedTimestamp) {
                log.info("Detected updated price_predictions.json on disk (timestamp changed). Reloading model dynamically.");
                loadModelData();
            }
        }
    }

    /**
     * Calculates a rent price recommendation for property owners.
     */
    public PriceSuggestionDTO suggestPrice(PriceSuggestionRequestDTO request) {
        if (request == null || request.getSizeM2() == null || request.getSizeM2() <= 0) {
            throw new IllegalArgumentException("Property size (sizeM2) must be greater than 0");
        }

        checkAndReloadModelData();

        int sizeM2 = request.getSizeM2();
        PropertyType propertyType = request.getPropertyType() != null ? request.getPropertyType() : PropertyType.APARTMENT;

        // Calculate live database statistics if external listings exist
        List<Listing> externalListings = listingRepository.findByExternalTrue();
        double liveDbAvg = 14.50;
        int liveDbCount = 0;
        if (externalListings != null && !externalListings.isEmpty()) {
            OptionalDouble avgOpt = externalListings.stream()
                    .filter(l -> l.getPricePerM2() != null && l.getPricePerM2() > 0)
                    .mapToInt(Listing::getPricePerM2)
                    .average();
            if (avgOpt.isPresent()) {
                liveDbAvg = round(avgOpt.getAsDouble());
                liveDbCount = externalListings.size();
            }
        }

        // Extract ARIMA forecast metrics
        double arimaPpm2 = liveDbAvg;
        double arimaLower = round(liveDbAvg * 0.95);
        double arimaUpper = round(liveDbAvg * 1.05);
        String arimaTrend = "STABLE";
        double arimaTrendPct = 0.0;
        int arimaHorizon = 5;
        String modelStatus = "ARIMA";
        Double confidenceLevel = 0.95;
        String intervalType = "ARIMA_PREDICTION_INTERVAL";

        if (cachedModelNode != null && cachedModelNode.has("arimaForecast")) {
            JsonNode arimaNode = cachedModelNode.get("arimaForecast");
            arimaPpm2 = arimaNode.path("predictedPricePerM2").asDouble(arimaPpm2);
            arimaLower = arimaNode.path("lowerBoundPerM2").asDouble(arimaLower);
            arimaUpper = arimaNode.path("upperBoundPerM2").asDouble(arimaUpper);
            arimaTrend = arimaNode.path("trend").asText(arimaTrend);
            arimaTrendPct = arimaNode.path("trendPercentage").asDouble(arimaTrendPct);
            arimaHorizon = arimaNode.path("horizonWeeks").asInt(arimaHorizon);
            modelStatus = arimaNode.path("modelStatus").asText(arimaHorizon > 0 ? "ARIMA" : "STATISTICAL_BASELINE");
            if (arimaNode.has("confidenceLevel") && !arimaNode.get("confidenceLevel").isNull()) {
                confidenceLevel = arimaNode.get("confidenceLevel").asDouble();
            } else {
                confidenceLevel = "ARIMA".equalsIgnoreCase(modelStatus) ? 0.95 : null;
            }
            intervalType = arimaNode.path("intervalType").asText(
                    "ARIMA".equalsIgnoreCase(modelStatus) ? "ARIMA_PREDICTION_INTERVAL" : "HISTORICAL_VARIABILITY_SPREAD"
            );
        }

        // Extract Scraped Market Benchmarks
        double marketAvgPpm2 = liveDbAvg;
        double marketMedianPpm2 = liveDbAvg;
        double propertyTypeAvgPpm2 = marketAvgPpm2;
        int comparableCount = liveDbCount > 0 ? liveDbCount : 1000;
        String overallStatus = "MEASURED";
        String propertyTypeStatus = "ESTIMATED_PRIOR";
        int propertyTypeCount = 0;
        double detailedOverallAvg = marketAvgPpm2;

        String dataSource = cachedModelNode != null && cachedModelNode.has("dataSource")
                ? cachedModelNode.get("dataSource").asText("Scraped market listings (Spitogatos.gr) & Auto-ARIMA")
                : "Scraped market listings (Spitogatos.gr) & Auto-ARIMA";

        if (cachedModelNode != null && cachedModelNode.has("scrapedMarketBenchmarks")) {
            JsonNode bm = cachedModelNode.get("scrapedMarketBenchmarks");
            marketAvgPpm2 = bm.path("overallAvgPricePerM2").asDouble(marketAvgPpm2);
            marketMedianPpm2 = bm.path("overallMedianPricePerM2").asDouble(marketMedianPpm2);
            comparableCount = bm.path("totalScrapedListings").asInt(comparableCount);
            overallStatus = bm.path("overallBenchmarkStatus").asText(bm.path("benchmarkStatus").asText("MEASURED"));
            detailedOverallAvg = bm.path("detailedListingsOverallAvgPerM2").asDouble(marketAvgPpm2);

            JsonNode byType = bm.path("byPropertyType").path(propertyType.name());
            if (!byType.isMissingNode() && byType.has("avgPricePerM2")) {
                propertyTypeAvgPpm2 = byType.path("avgPricePerM2").asDouble(propertyTypeAvgPpm2);
                propertyTypeCount = byType.path("count").asInt(0);
                propertyTypeStatus = byType.path("status").asText(propertyTypeCount >= 5 ? "MEASURED" : "ESTIMATED_PRIOR");
                if (propertyTypeCount > 0) {
                    comparableCount = propertyTypeCount;
                }
            } else {
                propertyTypeAvgPpm2 = getDefaultPpm2ForPropertyType(propertyType, marketAvgPpm2);
            }
        } else {
            propertyTypeAvgPpm2 = getDefaultPpm2ForPropertyType(propertyType, marketAvgPpm2);
        }

        // Incorporate live database external listings if present
        if (externalListings != null && !externalListings.isEmpty()) {
            List<Listing> typeListings = externalListings.stream()
                    .filter(l -> l.getPropertyType() == propertyType && l.getPricePerM2() != null && l.getPricePerM2() > 0)
                    .toList();

            if (!typeListings.isEmpty()) {
                double dbTypeAvg = typeListings.stream()
                        .mapToInt(Listing::getPricePerM2)
                        .average()
                        .orElse(propertyTypeAvgPpm2);
                // Blend database scraped average with dataset benchmark
                propertyTypeAvgPpm2 = round((propertyTypeAvgPpm2 * 0.5) + (dbTypeAvg * 0.5));
                comparableCount += typeListings.size();
                dataSource = "Live DRAS external database (" + externalListings.size() + " listings) & Auto-ARIMA";
            }
        }

        // Baseline Price per m2: 40% ARIMA time-series projection + 60% scraped property-type benchmark
        double baselinePpm2 = (0.40 * arimaPpm2) + (0.60 * propertyTypeAvgPpm2);

        // Calculate Micro-Adjustments based on property features
        List<String> adjustments = new ArrayList<>();

        // Size tier adjustment
        // For studios, the compact size premium is already embedded in the studio baseline (+20%).
        double sizeMultiplier = 1.0;
        if (propertyType != PropertyType.STUDIO) {
            if (sizeM2 < 35) {
                sizeMultiplier = 1.15;
                adjustments.add("+15% compact space premium (<35m²)");
            } else if (sizeM2 < 55) {
                sizeMultiplier = 1.05;
                adjustments.add("+5% high-demand size (35-55m²)");
            } else if (sizeM2 > 120) {
                sizeMultiplier = 0.90;
                adjustments.add("-10% large area volume discount (>120m²)");
            } else if (sizeM2 > 90) {
                sizeMultiplier = 0.95;
                adjustments.add("-5% medium-large area adjustment (90-120m²)");
            }
        } else {
            if (sizeM2 > 50) {
                sizeMultiplier = 0.95;
                adjustments.add("-5% large studio volume adjustment (>50m²)");
            }
        }

        // Floor adjustment
        double floorMultiplier = 1.0;
        if (request.getFloor() != null) {
            int fl = request.getFloor();
            if (fl <= 0) {
                floorMultiplier = 0.92;
                adjustments.add("-8% ground/basement floor adjustment");
            } else if (fl >= 4) {
                floorMultiplier = 1.06;
                adjustments.add("+6% upper floor view premium (floor " + fl + ")");
            } else if (fl >= 2) {
                floorMultiplier = 1.03;
                adjustments.add("+3% elevated floor premium (floor " + fl + ")");
            }
        }

        // Construction year adjustment
        double yearMultiplier = 1.0;
        if (request.getYearBuilt() != null && request.getYearBuilt() > 1900) {
            int y = request.getYearBuilt();
            if (y >= 2015) {
                yearMultiplier = 1.10;
                adjustments.add("+10% contemporary construction (built " + y + ")");
            } else if (y >= 2000) {
                yearMultiplier = 1.05;
                adjustments.add("+5% modern build (built " + y + ")");
            } else if (y < 1980) {
                yearMultiplier = 0.95;
                adjustments.add("-5% vintage building adjustment (built " + y + ")");
            }
        }

        // Bedrooms layout efficiency adjustment
        double roomsMultiplier = 1.0;
        if (request.getBedrooms() != null && request.getBedrooms() > 0) {
            int beds = request.getBedrooms();
            if (beds >= 3 && sizeM2 >= 80) {
                roomsMultiplier = 1.04;
                adjustments.add("+4% high room utility (" + beds + " bedrooms)");
            } else if (beds == 2 && sizeM2 >= 55) {
                roomsMultiplier = 1.02;
                adjustments.add("+2% functional layout (2 bedrooms)");
            } else if (beds == 1 && sizeM2 > 85) {
                roomsMultiplier = 0.97;
                adjustments.add("-3% spacious 1-bedroom (lower room density)");
            }
        }

        // Bathrooms convenience adjustment
        double bathMultiplier = 1.0;
        if (request.getBathrooms() != null && request.getBathrooms() >= 2) {
            int baths = request.getBathrooms();
            bathMultiplier = 1.04;
            adjustments.add("+4% multi-bathroom convenience (" + baths + " baths)");
        }

        // Neighborhood / Area keyword matching
        double areaMultiplier = 1.0;
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            String addr = request.getAddress().trim().toLowerCase();
            for (Map.Entry<String, List<String>> entry : AREA_KEYWORDS.entrySet()) {
                String areaKey = entry.getKey();
                boolean matches = entry.getValue().stream().anyMatch(addr::contains);
                if (matches) {
                    boolean handledEmpirically = false;
                    if (cachedModelNode != null && cachedModelNode.has("scrapedMarketBenchmarks")) {
                        JsonNode bmNode = cachedModelNode.get("scrapedMarketBenchmarks");
                        JsonNode areaStatNode = bmNode.path("byArea").path(areaKey);
                        if (!areaStatNode.isMissingNode() && areaStatNode.has("avgPricePerM2")) {
                            double areaAvg = areaStatNode.path("avgPricePerM2").asDouble(0.0);
                            int areaCount = areaStatNode.path("count").asInt(0);
                            boolean isEmpirical = areaStatNode.path("isMeasured").asBoolean(areaCount >= 5);
                            if (isEmpirical && detailedOverallAvg > 0) {
                                double measuredRatio = areaStatNode.has("ratioToDetailedOverall")
                                        ? areaStatNode.path("ratioToDetailedOverall").asDouble(areaAvg / detailedOverallAvg)
                                        : (areaAvg / detailedOverallAvg);
                                double priorFactor = PRIOR_AREA_FACTORS.getOrDefault(areaKey, 1.0);
                                double blendedFactor = round((0.70 * measuredRatio) + (0.30 * priorFactor));
                                areaMultiplier = Math.max(0.75, Math.min(1.45, blendedFactor));
                                int pctDiff = (int) Math.round((areaMultiplier - 1.0) * 100);
                                String sign = pctDiff >= 0 ? "+" : "";
                                adjustments.add(String.format(
                                        Locale.US,
                                        "%s%d%% empirical area adjustment (%s, avg €%.2f/m², n=%d)",
                                        sign, pctDiff, capitalize(areaKey), areaAvg, areaCount
                                ));
                                handledEmpirically = true;
                            }
                        }
                    }

                    if (!handledEmpirically) {
                        double priorFactor = PRIOR_AREA_FACTORS.getOrDefault(areaKey, 1.0);
                        areaMultiplier = priorFactor;
                        int pctDiff = (int) Math.round((areaMultiplier - 1.0) * 100);
                        String sign = pctDiff >= 0 ? "+" : "";
                        adjustments.add(String.format("%s%d%% location factor (%s, domain prior)", sign, pctDiff, capitalize(areaKey)));
                    }
                    break;
                }
            }
        }

        // Final adjusted Price per m2 & total Rent
        double finalPpm2 = baselinePpm2 * sizeMultiplier * floorMultiplier * yearMultiplier * roomsMultiplier * bathMultiplier * areaMultiplier;
        finalPpm2 = Math.max(3.0, Math.min(finalPpm2, 80.0)); // sanity bounds

        int suggestedPrice = (int) Math.round(finalPpm2 * sizeM2);
        suggestedPrice = Math.max(50, Math.min(suggestedPrice, 20000));

        // Estimated market range spread derived from time-series uncertainty
        double spread = Math.max(0.08, Math.min(0.15, (arimaUpper - arimaLower) / (2.0 * Math.max(1.0, arimaPpm2))));
        int minPrice = (int) Math.round(suggestedPrice * (1.0 - spread));
        int maxPrice = (int) Math.round(suggestedPrice * (1.0 + spread));
        double marginPercentage = round(spread * 100.0);

        PriceSuggestionDTO dto = new PriceSuggestionDTO();
        dto.setSuggestedPrice(suggestedPrice);
        dto.setSuggestedPricePerM2(round(finalPpm2));
        dto.setMinPrice(minPrice);
        dto.setMaxPrice(maxPrice);
        dto.setMinPricePerM2(round((double) minPrice / sizeM2));
        dto.setMaxPricePerM2(round((double) maxPrice / sizeM2));
        dto.setPriceRangeType("ESTIMATED_MARKET_RANGE");
        dto.setMarginPercentage(marginPercentage);
        dto.setMarginDescription("Heuristic market range bounded at ±8% to ±15% based on forecast volatility");

        // ARIMA details
        PriceSuggestionDTO.ArimaForecastInfo arimaInfo = new PriceSuggestionDTO.ArimaForecastInfo();
        arimaInfo.setModelStatus(modelStatus);
        arimaInfo.setConfidenceLevel(confidenceLevel);
        arimaInfo.setIntervalType(intervalType);
        arimaInfo.setIntervalDescription("ARIMA".equalsIgnoreCase(modelStatus)
                ? "Auto-ARIMA 95% prediction interval for future weekly observations"
                : "Historical ±1.96 standard deviation window over recent observations");
        arimaInfo.setPredictedPricePerM2(round(arimaPpm2));
        arimaInfo.setLowerBoundPerM2(round(arimaLower));
        arimaInfo.setUpperBoundPerM2(round(arimaUpper));
        arimaInfo.setTrend(arimaTrend);
        arimaInfo.setTrendPercentage(round(arimaTrendPct));
        arimaInfo.setHorizonWeeks(arimaHorizon);
        dto.setArimaForecast(arimaInfo);

        // Scraped market benchmark details
        PriceSuggestionDTO.MarketBenchmarkInfo mbInfo = new PriceSuggestionDTO.MarketBenchmarkInfo();
        mbInfo.setOverallBenchmarkStatus(overallStatus);
        mbInfo.setBenchmarkStatus(overallStatus);
        mbInfo.setAvgPricePerM2(round(marketAvgPpm2));
        mbInfo.setMedianPricePerM2(round(marketMedianPpm2));
        mbInfo.setPropertyType(propertyType.name());
        mbInfo.setPropertyTypeAvgPerM2(round(propertyTypeAvgPpm2));
        mbInfo.setPropertyTypeStatus(propertyTypeStatus);
        mbInfo.setPropertyTypeSampleCount(propertyTypeCount);
        mbInfo.setComparableCount(comparableCount);
        mbInfo.setDataSource(dataSource);
        dto.setMarketBenchmark(mbInfo);

        dto.setAppliedAdjustments(adjustments);

        String projectionText;
        if ("ARIMA".equalsIgnoreCase(modelStatus)) {
            projectionText = String.format(Locale.US, "Spitogatos Auto-ARIMA %d-week projection (€%.2f/m², %s trend)", arimaHorizon, arimaPpm2, arimaTrend);
        } else {
            projectionText = String.format(Locale.US, "Spitogatos statistical baseline rolling average (€%.2f/m², %s trend)", arimaPpm2, arimaTrend);
        }

        String explanation = String.format(
                Locale.US,
                "Suggested monthly rent of €%d (€%.2f/m²) for a %dm² %s. Based on %s synthesized with %d+ scraped market listings (market avg €%.2f/m²). Estimated market range is ±%.1f%%.",
                suggestedPrice, finalPpm2, sizeM2, propertyType.name(), projectionText, comparableCount, propertyTypeAvgPpm2, marginPercentage
        );
        dto.setExplanation(explanation);

        return dto;
    }

    private double getDefaultPpm2ForPropertyType(PropertyType type, double fallbackAvg) {
        return switch (type) {
            case STUDIO -> round(fallbackAvg * 1.25);
            case APARTMENT -> round(fallbackAvg * 1.0);
            case HOUSE -> round(fallbackAvg * 0.88);
            case MAISONETTE -> round(fallbackAvg * 1.05);
            case LOFT -> round(fallbackAvg * 1.15);
            case VILLA -> round(fallbackAvg * 1.40);
            case OTHER -> fallbackAvg;
        };
    }

    private double round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
