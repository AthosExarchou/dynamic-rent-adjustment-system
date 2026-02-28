package gr.hua.dit.dras.config;

/* imports */
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class SystemOwnerInitializer {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final RoleRepository roleRepository;

    public SystemOwnerInitializer(BCryptPasswordEncoder passwordEncoder,
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
            return;
        }
        System.out.println("Creating SYSTEM OWNER...");

        /* Ensures system user exists */
        Optional<User> existingUser = userRepository.findByUsername("external-system");
        User systemUser;

        if (existingUser.isPresent()) {
            systemUser = existingUser.get();
        } else {
            systemUser = new User(
                    "external-system",
                    "system@external.local",
                    passwordEncoder.encode(UUID.randomUUID().toString()) //random secure password
            );

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalStateException("USER role not found in database"));

            systemUser.getRoles().add(userRole);
            userRepository.save(systemUser);
        }

        /* Creates system owner */
        Owner systemOwner = new Owner();
        systemOwner.setUser(systemUser);
        systemOwner.setSystemOwner(true);

        systemOwner.setFirstName("System");
        systemOwner.setLastName("Owner");
        systemOwner.setPhoneNumber("+0000000000");

        ownerRepository.save(systemOwner);

        System.out.println("SYSTEM OWNER CREATED SUCCESSFULLY.");
    }
}
