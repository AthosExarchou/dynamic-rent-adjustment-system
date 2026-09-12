package gr.hua.dit.dras.config;

/* imports */
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.repositories.RoleRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RoleInitializer {

    private final RoleRepository roleRepository;

    public RoleInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    @Transactional
    public void setup() {
        initializeRole("USER");
        initializeRole("ADMIN");
        initializeRole("TENANT");
        initializeRole("OWNER");
    }

    private void initializeRole(String roleName) {
        roleRepository.updateOrInsert(new Role(roleName));
    }
}
