package gr.hua.dit.dras.config;

/* imports */
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class AdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminInitializer(
            BCryptPasswordEncoder passwordEncoder,
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultAdminUser() {
        Optional<User> defaultAdmin = userRepository.findByUsername("admin");

        if (defaultAdmin.isPresent()) {
            log.debug("Default admin already exists. Skipping initialization.");
            return;
        }

        /* Create default admin user if not exists */
        log.warn("Default admin user not found. Creating default admin user.");

        User admin = new User(
                "admin",
                "admin@gmail.com",
                passwordEncoder.encode("admin")
        );

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("USER role not found"));

        admin.getRoles().add(adminRole);
        admin.getRoles().add(userRole);

        userRepository.save(admin);

        log.info("Default admin user created.");
    }

}
