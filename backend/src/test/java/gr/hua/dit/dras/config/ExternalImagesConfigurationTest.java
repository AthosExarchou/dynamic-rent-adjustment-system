package gr.hua.dit.dras.config;

/* imports */
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalImagesConfigurationTest {

    @Test
    @DisplayName("Prod profile defines the root dras.external-images.enabled property")
    void prodProfileUsesRootLevelDrasProperty() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new FileSystemResource("src/main/resources/application.yml"));

        PropertySource<?> prod = sources.stream()
                .filter(source -> "prod".equals(source.getProperty("spring.config.activate.on-profile")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Loaded sources: " + sources.stream().map(PropertySource::getName).toList()));

        assertThat(prod.getProperty("dras.external-images.enabled"))
                .isEqualTo("${EXTERNAL_IMAGES_ENABLED:false}");
        assertThat(prod.getProperty("spring.dras.external-images.enabled")).isNull();
    }
}
