package gr.hua.dit.dras.controllers.auth;

/* imports */
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.repositories.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Should initialize roles on setup")
    void shouldInitializeRoles() {
        authController.setup();

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, org.mockito.Mockito.times(4)).updateOrInsert(roleCaptor.capture());

        assertThat(roleCaptor.getAllValues())
                .extracting(Role::getName)
                .containsExactlyElementsOf(List.of("USER", "ADMIN", "TENANT", "OWNER"));
    }

    @Test
    @DisplayName("Should return unauthorized response")
    void shouldReturnLoginView() {
        org.springframework.http.ResponseEntity<?> response = authController.login();

        assertThat(response.getStatusCodeValue()).isEqualTo(401);
    }
}
