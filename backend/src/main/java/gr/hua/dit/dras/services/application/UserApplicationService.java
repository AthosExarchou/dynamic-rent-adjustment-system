package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.dto.UserEditRequest;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/**
 * Application service responsible for orchestrating user-related account workflows.
 *
 * This service coordinates multiple domain services (UserService, ListingService,
 * TenantService) and infrastructure components (EmailService, PasswordEncoder)
 * to execute user account management use cases.
 *
 * Responsibilities include:
 * - Handling secure self-service account deletion / editing
 * - Allowing administrators to delete user accounts
 * - Performing security checks such as password re-authentication
 * - Ensuring proper cleanup of associated domain data (listings, tenant data)
 * - Triggering notification emails related to account deletion
 * - Recording security-relevant audit logs
 *
 * The service operates at the application layer and defines the transactional
 * boundary for user account deletion workflows. Core domain logic is delegated
 * to the appropriate domain services.
 *
 * Email notifications are treated as non-critical side effects; failures in
 * email delivery are logged but do not interrupt the primary transaction.
 */
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

    @Transactional
    public void deleteCurrentUserAccount(AccountDeletionRequest request) {

        /* Get current user */
        User currentUser = userService.getCurrentUserOptional()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        final Integer actorId = currentUser.getId();
        final String actorEmail = currentUser.getEmail();

        log.info("SECURITY_AUDIT | event=SELF_DELETE | stage=ATTEMPT | actorId={} | actorEmail={}",
                actorId, actorEmail);

        try {
            userService.assertNotAdmin(currentUser);

            if (!CONFIRMATION_PHRASE.equals(request.getConfirmationPhrase())) {
                throw new IllegalArgumentException("Confirmation phrase does not match.");
            }

            if (!passwordEncoder.matches(request.getPassword(), currentUser.getPassword())) {
                throw new IllegalArgumentException("Incorrect password.");
            }

            /* Clean up domain footprint */
            cleanupUserFootprint(currentUser);
            userService.deleteUser(actorId);

            log.info("SECURITY_AUDIT | event=SELF_DELETE | result=SUCCESS | actorId={} | actorEmail={}",
                    actorId, actorEmail);

        } catch (IllegalArgumentException | AccessDeniedException e) {
            log.warn("SECURITY_AUDIT | event=SELF_DELETE | result=DENIED | actorId={} | actorEmail={} | reason={}",
                    actorId, actorEmail, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("SECURITY_AUDIT | event=SELF_DELETE | result=FAILED | actorId={} | actorEmail={}",
                    actorId, actorEmail, e);
            throw e;
        }

        /* Send Email */
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendAccountDeletionEmail(actorEmail, currentUser);
                } catch (Exception e) {
                    log.error("SECURITY_AUDIT | event=SELF_DELETE | result=EMAIL_FAILED | actorId={} | actorEmail={}",
                            actorId, actorEmail, e);
                }
            }
        });
    }

    @Transactional
    public void deleteUserAsAdmin(Integer targetUserId) {

        /* Get current admin actor */
        User adminUser = userService.getCurrentUserOptional()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));

        /* Get target user to delete */
        User targetUser = userService.getUser(targetUserId);

        final Integer actorId = adminUser.getId();
        final String actorEmail = adminUser.getEmail();
        final Integer targetId = targetUser.getId();
        final String targetEmail = targetUser.getEmail();

        log.info("SECURITY_AUDIT | event=ADMIN_DELETE | stage=ATTEMPT | actorId={} " +
                        "| actorEmail={} | targetId={} | targetEmail={}",
                actorId, actorEmail, targetId, targetEmail);

        try {
            userService.assertNotAdmin(targetUser);

            /* Clean up domain footprint */
            cleanupUserFootprint(targetUser);
            userService.deleteUser(targetUser.getId());

            log.info("SECURITY_AUDIT | event=ADMIN_DELETE | result=SUCCESS | actorId={} " +
                            "| actorEmail={} | targetId={} | targetEmail={}",
                    actorId, actorEmail, targetId, targetEmail);

        } catch (IllegalArgumentException | AccessDeniedException e) {
            log.warn("SECURITY_AUDIT | event=ADMIN_DELETE | result=DENIED | actorId={} " +
                            "| actorEmail={} | targetId={} | targetEmail={} | reason={}",
                    actorId, actorEmail, targetId, targetEmail, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("SECURITY_AUDIT | event=ADMIN_DELETE | result=FAILED | actorId={} " +
                            "| actorEmail={} | targetId={} | targetEmail={}",
                    actorId, actorEmail, targetId, targetEmail, e);
            throw e;
        }

        /* Send Email */
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailService.sendAccountDeletionEmail(targetUser.getEmail(), targetUser);
                } catch (Exception e) {
                    log.error("SECURITY_AUDIT | event=ADMIN_DELETE | result=EMAIL_FAILED | actorId={} " +
                                    "| actorEmail={} | targetId={} | targetEmail={}",
                            actorId, actorEmail, targetId, targetEmail, e);
                }
            }
        });
    }

    @Transactional
    public boolean editUser(Integer targetUserId, UserEditRequest request) {

        /* Get current user */
        User currentUser = userService.getCurrentUserOptional()
                .orElseThrow(() -> new AccessDeniedException("Unauthenticated"));

        final Integer actorId = currentUser.getId();

        log.info("SECURITY_AUDIT | event=PROFILE_EDIT | stage=ATTEMPT | actorId={} | targetId={}",
                actorId, targetUserId);

        User targetUser;
        String oldUsername;
        String oldEmail;
        boolean usernameChanged;
        boolean emailChanged;

        try {
            targetUser = userService.getUser(targetUserId);

            /* Authorization Check */
            boolean isAdmin = currentUser.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ADMIN"));
            boolean isSelfEdit = actorId.equals(targetUser.getId());

            if (!isAdmin && !isSelfEdit) {
                throw new AccessDeniedException("You cannot edit another user's profile.");
            }

            userService.assertNotAdmin(targetUser);

            /* Check for changes */
            oldUsername = targetUser.getUsername();
            oldEmail = targetUser.getEmail();
            usernameChanged = !oldUsername.equals(request.getUsername());
            emailChanged = !oldEmail.equals(request.getEmail());

            if (!usernameChanged && !emailChanged) {
                log.info("SECURITY_AUDIT | event=PROFILE_EDIT | result=NO_CHANGES | actorId={} | targetId={}",
                        actorId, targetUserId);
                return false; // no changes made
            }

            /* Update Domain Entity */
            targetUser.setUsername(request.getUsername());
            targetUser.setEmail(request.getEmail());
            userService.updateUser(targetUser);

            log.info("SECURITY_AUDIT | event=PROFILE_EDIT | result=SUCCESS | actorId={} " +
                            "| targetId={} | usernameChanged={} | emailChanged={}",
                    actorId, targetUserId, usernameChanged, emailChanged);

        } catch (AccessDeniedException | IllegalArgumentException e) {
            log.warn("SECURITY_AUDIT | event=PROFILE_EDIT | result=DENIED | actorId={} | targetId={} | reason={}",
                    actorId, targetUserId, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("SECURITY_AUDIT | event=PROFILE_EDIT | result=FAILED | actorId={} | targetId={}",
                    actorId, targetUserId, e);
            throw e;
        }

        /* Send Email */
        try {
            if (emailChanged) {
                /* Notify old email */
                emailService.sendUserDetailsChangedEmail(oldEmail, targetUser.getUsername(),
                        targetUser.getEmail(), oldUsername, oldEmail, usernameChanged, true);
                /* Notify new email */
                emailService.sendUserDetailsChangedEmail(targetUser.getEmail(), targetUser.getUsername(),
                        targetUser.getEmail(), oldUsername, oldEmail, usernameChanged, true);
            } else {
                /* Notify current email of username change */
                emailService.sendUserDetailsChangedEmail(targetUser.getEmail(), targetUser.getUsername(),
                        targetUser.getEmail(), oldUsername, oldEmail, usernameChanged, false);
            }
        } catch (Exception e) {
            log.error("SECURITY_AUDIT | event=PROFILE_EDIT | result=EMAIL_FAILED | actorId={} " +
                            "| targetId={} | oldEmail={}",
                    actorId, targetUserId, oldEmail, e);
        }

        return true; // changes made successfully
    }

    /**
     * Helper method to ensure all domain relationships are cleanly severed
     * before a User account is permanently removed from the database.
     */
    private void cleanupUserFootprint(User user) {

        if (user.getOwner() != null) {
            listingService.deleteAllListingsForOwner(user.getOwner());
        }

        if (user.getTenant() != null) {
            tenantService.prepareTenantForDeletion(user.getTenant());
        }
    }

}
