package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.*;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.OwnerService;
import gr.hua.dit.dras.services.domain.TenantService;
import gr.hua.dit.dras.services.domain.UserService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpSession;

@Service
@Transactional
public class ListingApplicationService {

    private final ListingService listingService;
    private final OwnerService ownerService;
    private final TenantService tenantService;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    public ListingApplicationService(
            ListingService listingService,
            OwnerService ownerService,
            TenantService tenantService,
            UserService userService,
            RoleRepository roleRepository,
            EmailService emailService
    ) {
        this.listingService = listingService;
        this.ownerService = ownerService;
        this.tenantService = tenantService;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
    }

    /* Security / Helper methods */

    private User requireUser() {
        return userService.getCurrentUserOptional()
                .orElseThrow(() ->
                        new IllegalStateException("Authentication required"));
    }

    private Listing requireListing(Integer id) {
        return listingService.getListing(id);
    }

    private void validateModificationRights(Listing listing, User user) {
        listingService.validateListingModificationRights(listing, user);
    }

    private void trySendEmail(Runnable emailAction) {
        try {
            emailAction.run();
        } catch (Exception ignored) {
            // email failure does not break business flow
        }
    }

    @Transactional(readOnly = true)
    public List<Listing> getOwnerListingsForCurrentUser() {

        User user = requireUser();
        Owner owner = ownerService.getOwner(user.getId());
        return listingService.getListingsByOwner(owner);
    }

    /* Create Listing*/
    public void createListing(
            Listing listing,
            Integer ownerId,
            String firstName,
            String lastName,
            String phoneNumber,
            HttpSession session
    ) {
        User user = requireUser();

        Owner owner;
        if (!userService.isUserOwner()) {

            if (Stream.of(firstName, lastName, phoneNumber)
                    .anyMatch(v -> v == null || v.isBlank())) {
                throw new IllegalStateException(
                        "First name, last name, and phone number are required for new owner.");
            }

            if (!phoneNumber.matches("^\\+?[0-9. ()-]{7,25}$")) {
                throw new IllegalStateException("Invalid phone number format.");
            }

            owner = ownerService.createOwnerForCurrentUser(
                    firstName.trim(),
                    lastName.trim(),
                    phoneNumber.trim()
            );

            if (owner == null) {
                throw new IllegalStateException(
                        "Your OWNER role has been revoked by the Administrator.");
            }

            assignOwnerRoleIfFirstListing(owner, session);

        } else {
            if (ownerId == null) {
                ownerId = user.getId();
            }

            owner = ownerService.getOwner(ownerId);
            if (owner == null) {
                throw new IllegalStateException("Owner not found.");
            }
        }

        /* Initializes listing state */
        listing.setStatus(ListingStatus.PENDING);
        listing.setTenant(null);
        listing.setExternal(false);

        listingService.saveListing(listing);
        ownerService.assignOwnerToListing(listing.getId(), owner);

        Owner finalOwner = owner;
        trySendEmail(() ->
                emailService.sendEmailNotification(
                        finalOwner.getUser().getEmail(),
                        finalOwner.getFirstName() + " " + finalOwner.getLastName(),
                        listing,
                        "ownerCreated"
                )
        );
    }

    private void assignOwnerRoleIfFirstListing(Owner owner, HttpSession session) {

        if (!listingService.isFirstListing(owner)) return;

        User user = owner.getUser();

        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() ->
                        new IllegalStateException("Role 'OWNER' not found"));

        if (!user.getRoles().contains(ownerRole)) {
            user.getRoles().add(ownerRole);
            userService.updateUser(user);
        }

        session.invalidate();
    }

    /* Delete Listing */
    public void deleteListing(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);

        validateModificationRights(listing, user);

        /* Rented listings cannot be manually deleted */
        if (listing.getTenant() != null) {
            throw new IllegalStateException(
                    "Listing has an active rental and cannot be deleted.");
        }

        String ownerEmail = listing.getOwner().getUser().getEmail();

        trySendEmail(() ->
                emailService.sendListingDeletionEmail(ownerEmail, listing)
        );

        /* Delegates deletion to domain service */
        listingService.deleteListing(listingId);
    }

    /* Approval Workflows */
    public void approveListing(Integer listingId) {

        Listing listing = requireListing(listingId);
        listingService.approveListing(listingId);

        Owner owner = listing.getOwner();
        if (owner != null && owner.getUser() != null) {
            trySendEmail(() ->
                    emailService.sendEmailNotification(
                            owner.getUser().getEmail(),
                            owner.getFirstName() + " " + owner.getLastName(),
                            listing,
                            "adminApproved"
                    )
            );
        }
    }

    public void rejectListing(Integer listingId) {

        Listing listing = requireListing(listingId);
        listingService.rejectListing(listingId);

        Owner owner = listing.getOwner();
        if (owner != null && owner.getUser() != null) {
            trySendEmail(() ->
                    emailService.sendEmailNotification(
                            owner.getUser().getEmail(),
                            owner.getFirstName() + " " + owner.getLastName(),
                            listing,
                            "adminRejected"
                    )
            );
        }
    }

    /* Owner Assignment */

    public void assignOwner(Integer listingId, Integer ownerId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        validateModificationRights(listing, user);

        Owner owner = ownerService.getOwner(ownerId);
        ownerService.assignOwnerToListing(listingId, owner);
    }

    public void unassignOwner(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        validateModificationRights(listing, user);

        ownerService.unassignOwnerFromListing(listingId);
    }

    /* Tenant Assignment */

    public void assignTenant(Integer listingId, Integer tenantId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        validateModificationRights(listing, user);

        if (listing.getTenant() != null) {
            throw new IllegalStateException("Listing already has a tenant assigned.");
        }

        Tenant tenant = tenantService.getTenant(tenantId);
        tenantService.assignTenantToListing(listingId, tenant, "TENANT");
    }

    public void unassignTenant(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        validateModificationRights(listing, user);

        Integer tenantId = tenantService.getTenantIdForCurrentUser();
        tenantService.unassignTenantFromListing(listingId, tenantId);
    }

    /* Application View */
    @Transactional(readOnly = true)
    public Listing viewApplications(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        validateModificationRights(listing, user);
        return listing;
    }
}
