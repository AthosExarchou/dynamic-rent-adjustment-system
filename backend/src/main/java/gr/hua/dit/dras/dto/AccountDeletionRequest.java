package gr.hua.dit.dras.dto;

/* imports */
import jakarta.validation.constraints.NotBlank;

public class AccountDeletionRequest {

    @NotBlank(message = "Confirmation phrase required.")
    private String confirmationPhrase;

    @NotBlank(message = "Password required.")
    private String password;

    public String getConfirmationPhrase() {
        return confirmationPhrase;
    }

    public void setConfirmationPhrase(String confirmationPhrase) {
        this.confirmationPhrase = confirmationPhrase;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
