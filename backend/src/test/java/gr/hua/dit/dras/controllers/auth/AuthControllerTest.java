package gr.hua.dit.dras.controllers.auth;

/* imports */
import gr.hua.dit.dras.repositories.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Should return unauthorized response")
    void shouldReturnLoginView() {
        ResponseEntity<?> response = authController.login();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
