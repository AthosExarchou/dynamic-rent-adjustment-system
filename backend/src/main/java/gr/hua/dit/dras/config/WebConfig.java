package gr.hua.dit.dras.config;

/* imports */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${dras.upload-dir:uploads/images}")
    private String uploadDirStr;

    @Value("${dras.analytics-images-dir:analytics/data/images}")
    private String analyticsImagesDirStr;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path analyticsImages = resolvePath(analyticsImagesDirStr);
        Path uploadsImages = resolvePath(uploadDirStr);

        log.info("Registering local image resource handlers: analytics={}, uploads={}",
                analyticsImages, uploadsImages);

        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        toLocationUri(analyticsImages),
                        toLocationUri(uploadsImages)
                )
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());
    }

    private Path resolvePath(String relative) {
        Path p = Paths.get(relative).toAbsolutePath().normalize();
        if (!Files.exists(p)) {
            Path parentCandidate = Paths.get("..", relative).toAbsolutePath().normalize();
            if (Files.exists(parentCandidate)) {
                p = parentCandidate;
            }
        }
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            log.warn("Could not create directory for path: {}", p);
        }
        return p;
    }

    private String toLocationUri(Path path) {
        String uri = path.toUri().toString();
        if (!uri.endsWith("/")) {
            uri = uri + "/";
        }
        return uri;
    }
}
