package gr.hua.dit.dras.dto;

/* imports */
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static User user() {
        return new User("co", "co@gmail.com", "password");
    }

    @Test
    void serializationUsesIsSystemKeyAndOmitsPassword() throws Exception {
        UserDTO dto = new UserDTO();
        dto.setIsSystem(true);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertAll(
                () -> assertTrue(json.has("isSystem")),
                () -> assertFalse(json.has("system")),
                () -> assertFalse(json.has("password"))
        );
    }

    @Test
    void mapsIdentityFieldsRolesAndTimestamps() {
        User source = user();
        source.setId(39);
        source.setRoles(Set.of(new Role("USER"), new Role("TENANT")));
        LocalDateTime createdAt = LocalDateTime.of(2025, 12, 31, 23, 59);
        LocalDateTime lastLogin = LocalDateTime.of(2026, 1, 2, 3, 4);
        LocalDateTime passwordChangedAt = LocalDateTime.of(2026, 2, 3, 4, 5);
        ReflectionTestUtils.setField(source, "createdAt", createdAt);
        source.setLastLogin(lastLogin);
        source.setPasswordChangedAt(passwordChangedAt);

        UserDTO dto = new UserDTO(source);

        assertAll(
                () -> assertEquals(39, dto.getId()),
                () -> assertEquals("co", dto.getUsername()),
                () -> assertEquals("co@gmail.com", dto.getEmail()),
                () -> assertEquals(Set.of("USER", "TENANT"), new HashSet<>(dto.getRoles())),
                () -> assertEquals(createdAt, dto.getCreatedAt()),
                () -> assertEquals(lastLogin, dto.getLastLogin()),
                () -> assertEquals(passwordChangedAt, dto.getPasswordChangedAt())
        );
    }

    @Test
    void mapsEmptyRolesToEmptyList() {
        UserDTO dto = new UserDTO(user());

        assertNotNull(dto.getRoles());
        assertTrue(dto.getRoles().isEmpty());
    }

    @Test
    void nullRolesBecomeEmptyList() {
        User source = user();
        source.setRoles(null);

        UserDTO dto = new UserDTO(source);

        assertNotNull(dto.getRoles());
        assertTrue(dto.getRoles().isEmpty());
    }

    @Test
    void ownerNameTakesPrecedenceOverTenant() {
        User source = user();
        source.setOwner(new Owner("John", "Owner", "+123456789", false));
        source.setTenant(new Tenant("Jane", "Tenant", "+123456789"));

        UserDTO dto = new UserDTO(source);

        assertAll(
                () -> assertEquals("John", dto.getFirstName()),
                () -> assertEquals("Owner", dto.getLastName())
        );
    }

    @Test
    void tenantNameUsedWhenOwnerAbsent() {
        User source = user();
        source.setTenant(new Tenant("Jane", "Doe", "+123456789"));

        UserDTO dto = new UserDTO(source);

        assertAll(
                () -> assertEquals("Jane", dto.getFirstName()),
                () -> assertEquals("Doe", dto.getLastName())
        );
    }

    @Test
    void incompleteOwnerNameFallsBackToTenant() {
        User source = user();
        source.setOwner(new Owner("John", "", "+123456789", false));
        source.setTenant(new Tenant("Jane", "Doe", "+123456789"));

        UserDTO dto = new UserDTO(source);

        assertAll(
                () -> assertEquals("Jane", dto.getFirstName()),
                () -> assertEquals("Doe", dto.getLastName())
        );
    }

    @Test
    void blankOwnerNameFallsBackToTenant() {
        User source = user();
        source.setOwner(new Owner(" ", "Doe", "+123456789", false));
        source.setTenant(new Tenant("Jane", "Doe", "+123456789"));

        UserDTO dto = new UserDTO(source);

        assertAll(
                () -> assertEquals("Jane", dto.getFirstName()),
                () -> assertEquals("Doe", dto.getLastName())
        );
    }

    @Test
    void namesRemainNullWithoutOwnerOrTenant() {
        UserDTO dto = new UserDTO(user());

        assertAll(
                () -> assertNull(dto.getFirstName()),
                () -> assertNull(dto.getLastName())
        );
    }

    @Test
    void systemFlagComesFromOwner() {
        User systemUser = user();
        systemUser.setOwner(new Owner("System", "Owner", "+123456789", true));
        User regularUser = user();
        regularUser.setOwner(new Owner("Regular", "Owner", "+123456789", false));

        assertAll(
                () -> assertTrue(new UserDTO(systemUser).getIsSystem()),
                () -> assertFalse(new UserDTO(regularUser).getIsSystem()),
                () -> assertFalse(new UserDTO(user()).getIsSystem())
        );
    }
}
