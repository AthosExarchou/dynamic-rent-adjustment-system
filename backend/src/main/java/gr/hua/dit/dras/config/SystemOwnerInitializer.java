package gr.hua.dit.dras.config;

/* imports */
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class SystemOwnerInitializer {

    private static final Logger log = LoggerFactory.getLogger(SystemOwnerInitializer.class);

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final RoleRepository roleRepository;

    public SystemOwnerInitializer(
            BCryptPasswordEncoder passwordEncoder,
            UserRepository userRepository,
            OwnerRepository ownerRepository,
            RoleRepository roleRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.roleRepository = roleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createSystemOwner() {

        /* Checks if system owner already exists */
        Optional<Owner> existingOwner = ownerRepository.findBySystemOwnerTrue();
        if (existingOwner.isPresent()) {
            log.debug("System owner already exists. Skipping initialization.");
            return;
        }
        log.debug("System owner not found. Creating system owner...");

        /* Ensures system user exists */
        Optional<User> existingUser = userRepository.findByUsername("external-system");
        User systemUser;

        if (existingUser.isPresent()) {
            log.debug("System user already exists. Reusing existing user.");
            systemUser = existingUser.get();
        } else {
            systemUser = new User(
                    "external-system",
                    "system@external.local",
                    passwordEncoder.encode(UUID.randomUUID().toString()) // random secure password
            );

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("USER role not found in database"));

            systemUser.getRoles().add(userRole);
            userRepository.save(systemUser);

            log.info("System user created successfully.");
        }

        /* Creates system owner */
        Owner systemOwner = new Owner();
        systemOwner.setUser(systemUser);
        systemOwner.setSystemOwner(true);

        systemOwner.setFirstName("System");
        systemOwner.setLastName("Owner");
        systemOwner.setPhoneNumber("+0000000000");

        ownerRepository.save(systemOwner);

        log.info("System owner created successfully.");
    }

}
