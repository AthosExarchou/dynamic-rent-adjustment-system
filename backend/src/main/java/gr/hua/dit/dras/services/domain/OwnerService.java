package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Transactional
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public OwnerService(
            OwnerRepository ownerRepository,
            RoleRepository roleRepository,
            UserService userService,
            ListingRepository listingRepository,
            UserRepository userRepository
    ) {
        this.ownerRepository = ownerRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Owner> getOwners() {
        return ownerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Owner getOwner(Integer ownerId) {
        if (ownerId != null) {
            return ownerRepository.findById(ownerId)
                    .orElseThrow(() ->
                            new IllegalStateException("Owner not found with id: " + ownerId)
                    );
        }

        Integer currentUserId = userService.getCurrentUserId();
        return ownerRepository.findByUserId(currentUserId)
                .orElseThrow(() ->
                        new IllegalStateException("Owner not found for current user")
                );
    }

    @Transactional
    public Owner createOwnerForUser(
            Integer userId,
            String firstName,
            String lastName,
            String phoneNumber
    ) {

        User user = userService.getUser(userId); // fetches user by ID

        ownerRepository.findByUserId(userId).ifPresent(o -> {
            throw new IllegalStateException("User already has an Owner profile");
        });

        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setPhoneNumber(phoneNumber);
        owner.setUser(user); // associates owner with the user

        Owner savedOwner = ownerRepository.save(owner);

        assignOwnerRole(user);

        return savedOwner;
    }

    @Transactional
    public Owner createOwnerForCurrentUser(
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        Integer userId = userService.getCurrentUserId();
        return createOwnerForUser(userId, firstName, lastName, phoneNumber);
    }

    @Transactional(readOnly = true)
    public Integer getOwnerIdForCurrentUser() {
        Integer userId = userService.getCurrentUserId();

        return ownerRepository.findByUserId(userId)
                .map(Owner::getId)
                .orElseThrow(() ->
                        new IllegalStateException("Owner profile not found for current user")
                );
    }

    private void assignOwnerRole(User user) {

        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() ->
                        new IllegalStateException("OWNER role not found in database")
                );

        if (!user.getRoles().contains(ownerRole)) {
            user.getRoles().add(ownerRole);
            userService.updateUser(user);
        }
    }

    @Transactional
    public void saveOwner(Owner owner) {
        ownerRepository.save(owner);
    }

    @Transactional
    public void assignOwnerToListing(Integer listingId, Owner owner) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() ->
                        new IllegalStateException("Listing not found")
                );

        listing.setOwner(owner);

        User ownerUser = owner.getUser();

        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() ->
                        new IllegalStateException("OWNER role not found in database")
                );

        if (!ownerUser.getRoles().contains(ownerRole)) {
            ownerUser.getRoles().add(ownerRole);
            userService.updateUser(ownerUser); // saves the user
        }
        listingRepository.save(listing);
    }

    @Transactional
    public void unassignOwnerFromListing(Integer listingId) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() ->
                        new IllegalStateException("Listing not found")
                );
        listing.setOwner(null);
        listing.disable();
        listingRepository.save(listing);
    }

}
