package gr.hua.dit.dras.dto;

/* imports */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gr.hua.dit.dras.entities.Notification;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static Notification notification() {
        Notification notification = new Notification();
        notification.setId(7);
        notification.setTitle("Rent due");
        notification.setMessage("Your rent payment is due soon.");
        notification.setIsRead(true);
        notification.setCreatedAt(LocalDateTime.of(2026, 5, 1, 10, 20, 30));
        return notification;
    }

    @Test
    void mapsEntityFields() {
        NotificationDTO dto = new NotificationDTO(notification());

        assertAll(
                () -> assertEquals(7, dto.getId()),
                () -> assertEquals("Rent due", dto.getTitle()),
                () -> assertEquals("Your rent payment is due soon.", dto.getMessage()),
                () -> assertTrue(dto.getRead()),
                () -> assertEquals(LocalDateTime.of(2026, 5, 1, 10, 20, 30), dto.getCreatedAt())
        );
    }

    @Test
    void serializesReadFlagAndCreatedAtPattern() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(new NotificationDTO(notification())));

        assertAll(
                () -> assertTrue(json.get("read").asBoolean()),
                () -> assertEquals("2026-05-01T10:20:30", json.get("createdAt").asText())
        );
    }
}
