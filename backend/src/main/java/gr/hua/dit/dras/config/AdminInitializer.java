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
import java.security.SecureRandom;
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

        // BUG-B01 FIX: Generate a random secure password instead of the hardcoded "admin" default.
        // The generated password is printed ONCE at WARN level. Change it immediately after first login.
        String generatedPassword = generateSecurePassword(16);
        log.warn("========================================================");
        log.warn("  ADMIN INITIAL PASSWORD: {}", generatedPassword);
        log.warn("  Change this password immediately after first login!");
        log.warn("========================================================");

        User admin = new User(
                "admin",
                "admin@gmail.com",
                passwordEncoder.encode(generatedPassword)
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

    private String generateSecurePassword(int length) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

}
