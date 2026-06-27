package gr.hua.dit.dras.controllers.auth;

/* imports */
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.repositories.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final RoleRepository roleRepository;

    public AuthController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void setup() {
        initializeRole("USER");
        initializeRole("ADMIN");
        initializeRole("TENANT");
        initializeRole("OWNER");
    }

    private void initializeRole(String roleName) {
        roleRepository.updateOrInsert(new Role(roleName));
    }

    @GetMapping("/login")
    public org.springframework.http.ResponseEntity<?> login() {
        return org.springframework.http.ResponseEntity.status(401)
                .body(java.util.Map.of("error", "Unauthorized", "message", "Please authenticate via /api/auth/login"));
    }
}
