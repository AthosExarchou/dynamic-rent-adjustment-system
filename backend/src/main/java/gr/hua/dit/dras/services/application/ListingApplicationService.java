package gr.hua.dit.dras.services.application;

/* imports */
import gr.hua.dit.dras.dto.ListingCreateDTO;
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.*;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.OwnerService;
import gr.hua.dit.dras.services.domain.TenantService;
import gr.hua.dit.dras.services.domain.UserService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpSession;

/**
 * Application service responsible for orchestrating listing-related workflows.
 *
 * This service coordinates interactions between Listing, Owner, Tenant,
 * and User domain services to execute use cases such as:
 *
 * - Creating and deleting listings
 * - Assigning owners and tenants
 * - Approving or rejecting tenant applications
 * - Managing listing approval workflows
 *
 * The service operates at the application layer and does not contain
 * core domain business logic. Domain rules are delegated to the
 * corresponding domain services.
 *
 * Email notifications may be triggered as side effects, but failures
 * in notification delivery do not affect the main business transaction.
 */
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
        try {
            Owner owner = ownerService.getOwner(null);
            return listingService.getListingsByOwner(owner);
        } catch (IllegalStateException e) {
            return java.util.Collections.emptyList();
        }
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

        if (user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()))) {
            throw new AccessDeniedException("Administrators are not permitted to create listings.");
        }

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


        } else {
            if (ownerId == null) {
                owner = ownerService.getOwner(null);
            } else {
                owner = ownerService.getOwner(ownerId);
            }
            if (owner == null) {
                throw new IllegalStateException("Owner not found.");
            }
        }

        /* Initializes listing state */
        listing.setStatus(ListingStatus.PENDING);
        listing.setTenant(null);
        listing.setExternal(false);

        assignOwnerRoleIfFirstListing(owner, session);

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

    public Listing editListing(Integer listingId, ListingCreateDTO dto) {
        User user = requireUser();
        Listing listing = requireListing(listingId);

        validateModificationRights(listing, user);

        boolean wasRejected = listing.getStatus() == ListingStatus.REJECTED;
        boolean wasApproved = listing.getStatus() == ListingStatus.APPROVED;
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));

        listing.setTitle(dto.getTitle());
        listing.setSubtitle(dto.getSubtitle());
        listing.setDescription(dto.getDescription());
        listing.setPrice(dto.getPrice());
        listing.setPricePerM2(dto.getPricePerM2());
        listing.setAddress(dto.getAddress());
        listing.setPropertyType(dto.getPropertyType());
        listing.setRentalDuration(dto.getRentalDuration());
        listing.setYearBuilt(dto.getYearBuilt());
        listing.setSizeM2(dto.getSizeM2());
        listing.setFloor(dto.getFloor());
        listing.setBedrooms(dto.getBedrooms());
        listing.setBathrooms(dto.getBathrooms());
        if (dto.getImages() != null) {
            listing.setImages(new ArrayList<>(
                    dto.getImages().stream()
                            .filter(img -> img != null && !img.isBlank())
                            .toList()
            ));
        }

        /* Reset status to PENDING for admin review if an owner edits an APPROVED or REJECTED listing */
        if (!isAdmin && (wasRejected || wasApproved)) {
            listing.setStatus(ListingStatus.PENDING);
            if (wasRejected) {
                listing.setRejectionReason(null);
            }
        }

        Listing updated = listingService.updateListing(listing);

        if (wasRejected) {
            notifyAdminsOfResubmittedListing(updated);
        }

        return updated;
    }

    private void notifyAdminsOfResubmittedListing(Listing listing) {
        List<User> admins = userService.getAdminUsers();
        if (admins == null || admins.isEmpty()) {
            trySendEmail(() -> emailService.sendEmailNotification(
                    "admin@gmail.com",
                    "Administrator",
                    listing,
                    "listingResubmittedAdmin"
            ));
            return;
        }

        for (User admin : admins) {
            if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                String adminName = admin.getUsername() != null ? admin.getUsername() : "Administrator";
                trySendEmail(() -> emailService.sendEmailNotification(
                        admin.getEmail(),
                        adminName,
                        listing,
                        "listingResubmittedAdmin"
                ));
            }
        }
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
            userService.refreshAuthenticationRoles(user);
        }
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

    public void rejectListing(Integer listingId, String reason) {

        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        if (reason.trim().length() > 500) {
            throw new IllegalArgumentException("Rejection reason cannot exceed 500 characters");
        }

        Listing listing = listingService.rejectListing(listingId, reason.trim());

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

    public void rejectListing(Integer listingId) {
        rejectListing(listingId, "Listing does not meet publication criteria");
    }

    /* Disable / Enable Workflows */

    public void disableListing(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        validateModificationRights(listing, user);

        listingService.disableListing(listingId);
    }

    public void enableListing(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        validateModificationRights(listing, user);

        listingService.enableListing(listingId);
    }

    public void toggleListingStatus(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        validateModificationRights(listing, user);

        if (listing.getStatus() == ListingStatus.DISABLED) {
            listingService.enableListing(listingId);
        } else if (listing.getStatus() == ListingStatus.APPROVED) {
            listingService.disableListing(listingId);
        } else {
            throw new IllegalStateException("Only approved or disabled listings can have their status toggled.");
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
        
        if (listing.getTenant() != null) {
            throw new IllegalStateException("Cannot unassign owner from a rented listing.");
        }

        ownerService.unassignOwnerFromListing(listingId);
    }

    /* Tenant Unassignment */
    public void unassignTenant(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);

        listingService.validateListingOwnership(listing, user); // validates permissions

        if (listing.getTenant() == null) {
            throw new IllegalStateException("This listing has no tenant assigned.");
        }
        Integer tenantId = listing.getTenant().getId();
        tenantService.unassignTenantFromListing(listingId, tenantId);
    }

    /* Application Workflows */

    @Transactional
    public void approveTenantApplication(Integer listingId, Integer tenantId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        Tenant tenant = tenantService.getTenant(tenantId);

        validateModificationRights(listing, user); // validates permissions

        if (!listing.getApplicants().contains(tenant)) {
            throw new IllegalStateException("This tenant did not apply for this listing.");
        }

        performTenantAssignment(listing, tenant);

        /* Send Email */
        User tenantUser = tenant.getUser();
        if (tenantUser != null) {
            trySendEmail(() -> emailService.sendEmailNotification(
                    tenantUser.getEmail(),
                    tenant.getFirstName() + " " + tenant.getLastName(),
                    listing,
                    "tenantApproval"
            ));
        }
    }

    private void performTenantAssignment(Listing listing, Tenant approvedTenant) {

        /* Execute Domain Behavior */
        List<Tenant> rejectedApplicants = listing.rentTo(approvedTenant);

        /* Process Rejections */
        for (Tenant applicant : rejectedApplicants) {

            /* Updates the Tenant's domain state and removes DB join records */
            applicant.processRejection(listing);

            /* Send Email */
            if (applicant.getUser() != null) {
                trySendEmail(() -> emailService.sendEmailNotification(
                        applicant.getUser().getEmail(),
                        applicant.getFirstName() + " " + applicant.getLastName(),
                        listing,
                        "listingRentedToSomeoneElse"
                ));
            }
        }

        grantPlatformAccessRole(approvedTenant.getUser(), "TENANT");
    }

    private void grantPlatformAccessRole(User user, String roleName) {

        if (user == null) {
            return;
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role not found"));

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            userService.updateUser(user);
            userService.refreshAuthenticationRoles(user);
        }
    }

    @Transactional
    public void rejectTenantApplication(Integer listingId, Integer tenantId) {

        User currentUser = requireUser();
        Listing listing = requireListing(listingId);
        Tenant tenant = tenantService.getTenant(tenantId);

        validateModificationRights(listing, currentUser); // validates permissions

        listingService.rejectApplicant(listing, tenant); // executes domain logic

        /* Send Email */
        if (tenant.getUser() != null) {
            trySendEmail(() -> emailService.sendEmailNotification(
                    tenant.getUser().getEmail(),
                    tenant.getFirstName() + " " + tenant.getLastName(),
                    listing,
                    "ownerRejectedApplication"
            ));
        }
    }

    /* Application View */
    @Transactional(readOnly = true)
    public Listing viewApplications(Integer listingId) {

        User user = requireUser();
        Listing listing = requireListing(listingId);
        listingService.validateListingOwnership(listing, user);
        return listing;
    }
}
