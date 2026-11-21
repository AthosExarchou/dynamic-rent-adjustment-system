package gr.hua.dit.dras.dto;

/* imports */
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Safe DTO for returning user data via REST API.
 * Excludes password, breaks circular entity references.
 */
public class UserDTO {
    private Integer id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private LocalDateTime passwordChangedAt;
    private boolean isSystem;

    public UserDTO() {}

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();

        Owner owner = user.getOwner();
        Tenant tenant = user.getTenant();

        String resolvedFirstName = null;
        String resolvedLastName = null;

        if (owner != null) {
            resolvedFirstName = owner.getFirstName();
            resolvedLastName = owner.getLastName();
        }

        boolean ownerNameMissing = isBlank(resolvedFirstName) || isBlank(resolvedLastName);

        if (ownerNameMissing && tenant != null) {
            resolvedFirstName = tenant.getFirstName();
            resolvedLastName = tenant.getLastName();
        }

        this.firstName = resolvedFirstName;
        this.lastName = resolvedLastName;

        this.roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(r -> r.getName())
                        .collect(Collectors.toList());
        this.createdAt = user.getCreatedAt();
        this.lastLogin = user.getLastLogin();
        this.passwordChangedAt = user.getPasswordChangedAt();
        this.isSystem = owner != null && owner.isSystemOwner();
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public List<String> getRoles() {
        return roles;
    }
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }
    public boolean getIsSystem() {
        return isSystem;
    }
    public void setIsSystem(boolean isSystem) {
        this.isSystem = isSystem;
    }
}
