package gr.hua.dit.dras.services.domain;

/* imports */
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.hua.dit.dras.dto.MacroForecastDTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class MacroAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(MacroAnalyticsService.class);

    private final ObjectMapper objectMapper;
    private MacroForecastDTO cachedForecast;
    private File activeModelFile;
    private long lastLoadedTimestamp = 0L;

    public MacroAnalyticsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadModelData();
    }

    /**
     * Loads the Bank of Greece macroeconomic prediction model JSON from disk.
     */
    public synchronized void loadModelData() {
        try {
            List<Path> candidatePaths = List.of(
                    Paths.get("analytics", "outputs", "macro_predictions.json"),
                    Paths.get("..", "analytics", "outputs", "macro_predictions.json"),
                    Paths.get("outputs", "macro_predictions.json")
            );

            for (Path candidate : candidatePaths) {
                File file = candidate.toFile();
                if (file.exists() && file.canRead()) {
                    cachedForecast = objectMapper.readValue(file, MacroForecastDTO.class);
                    activeModelFile = file;
                    lastLoadedTimestamp = file.lastModified();
                    log.info("Loaded macroeconomic prediction model from filesystem: {}", file.getAbsolutePath());
                    return;
                }
            }

            log.warn("Macroeconomic predictions JSON not found on filesystem. Model is currently uninitialized.");
            cachedForecast = null;
        } catch (Exception e) {
            log.error("Failed to load macroeconomic prediction model JSON: {}", e.getMessage(), e);
            cachedForecast = null;
        }
    }

    /**
     * Checks if the prediction model JSON file on disk has been updated
     * and automatically reloads it into memory.
     */
    private void checkAndReloadModelData() {
        if (activeModelFile != null && activeModelFile.exists()) {
            if (activeModelFile.lastModified() > lastLoadedTimestamp) {
                log.info("Detected updated macro_predictions.json on disk. Reloading model dynamically.");
                loadModelData();
            }
        } else if (cachedForecast == null) {
            loadModelData();
        }
    }

    /**
     * Returns the current macroeconomic forecast for Athens apartment prices.
     * Throws 404 NOT_FOUND if the model has not yet been computed statistically.
     */
    public MacroForecastDTO getMacroForecast() {
        checkAndReloadModelData();
        if (cachedForecast == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Macroeconomic forecast model is not available. Please run arima_macro.py to generate statistical forecasts from Bank of Greece data."
            );
        }
        return cachedForecast;
    }
}
