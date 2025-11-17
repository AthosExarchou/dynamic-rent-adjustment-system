package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.repositories.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    RoleRepository roleRepository;

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
    public String login() {
        return "auth/login";
    }
}
