package gr.hua.dit.dras.dto;

/* imports */
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserEditRequest {

    @NotBlank(message = "Username cannot be empty.")
    @Size(max = 20, message = "Username cannot exceed 20 characters.")
    private String username;

    @NotBlank(message = "Email cannot be empty.")
    @Email(message = "Must be a valid email format.")
    @Size(max = 50, message = "Email cannot exceed 50 characters.")
    private String email;

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

}
