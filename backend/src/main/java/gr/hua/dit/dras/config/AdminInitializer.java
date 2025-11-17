package gr.hua.dit.dras.config;


/* imports */
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class AdminInitializer {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminInitializer(BCryptPasswordEncoder passwordEncoder, UserRepository userRepository, RoleRepository roleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultAdminUser() {
        Optional<User> defaultAdmin = userRepository.findByUsername("admin");

        /* creates default admin user if not exists */
        if (defaultAdmin.isEmpty()) {
            User admin = new User("admin", "admin@gmail.com", passwordEncoder.encode("admin"));
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ADMIN not found in database"));

            admin.getRoles().add(adminRole);
            userRepository.save(admin);
            System.out.println("Default ADMIN user created.");
        }
    }
}


