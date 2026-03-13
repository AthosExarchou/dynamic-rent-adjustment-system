package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gr.hua.dit.dras.dto.AccountDeletionRequest;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.domain.UserService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserApplicationService {

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);
    private static final String CONFIRMATION_PHRASE = "DELETE MY ACCOUNT";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ListingService listingService;
    private final TenantService tenantService;

    public UserApplicationService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            ListingService listingService,
            TenantService tenantService
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.listingService = listingService;
        this.tenantService = tenantService;
    }

    public void deleteCurrentUserAccount(AccountDeletionRequest request) {

        /* Get current user */
        User currentUser = userService.getCurrentUserOptional()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        userService.assertNotAdmin(currentUser);

        if (!CONFIRMATION_PHRASE.equals(request.getConfirmationPhrase())) {
            throw new IllegalArgumentException("Confirmation phrase does not match.");
        }

        /* Security Re-authentication */
        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Incorrect password.");
        }

        log.warn("Account deletion triggered for user ID: {} Email: {}",
                currentUser.getId(), currentUser.getEmail());

        /* Send Email */
        try {
            emailService.sendAccountDeletionEmail(currentUser.getEmail(), currentUser);
        } catch (Exception e) {
            log.error("Failed to send deletion email to {}", currentUser.getEmail());
        }

        userService.deleteUser(currentUser.getId()); // delegates pure deletion to Domain Service
    }

    @Transactional
    public void deleteUserAsAdmin(Integer userId) {

        User user = userService.getUser(userId);
        userService.assertNotAdmin(user);

        /* Send Email */
        try {
            emailService.sendAccountDeletionEmail(user.getEmail(), user);
        } catch (Exception e) {
            log.error("User deleted, but email could not be sent to {}", user.getEmail());
        }

        /* Clean up */
        if (user.getOwner() != null) {
            listingService.deleteAllListingsForOwner(user.getOwner());
        }

        if (user.getTenant() != null) {
            tenantService.prepareTenantForDeletion(user.getTenant());
        }

        userService.deleteUser(user.getId()); // deletes user and profiles
    }

}
