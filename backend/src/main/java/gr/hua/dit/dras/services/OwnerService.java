package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
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

    public List<Owner> getOwners() {
        return ownerRepository.findAll();
    }

    public Owner getOwner(Integer ownerId) {
        if (ownerId != null) {
            return ownerRepository.findById(ownerId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Owner not found with id: " + ownerId
                            )
                    );
        }

        Integer currentUserId = userService.getCurrentUserId();
        return ownerRepository.findByUserId(currentUserId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Owner not found for current user"
                        )
                );
    }

    public Owner createOwnerForUser(
            Integer userId,
            String firstName,
            String lastName,
            String phoneNumber
    ) {

        User user = userService.getUser(userId); //fetches user by ID

        ownerRepository.findByUserId(userId).ifPresent(o -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User already has an Owner profile"
            );
        });

        Owner owner = new Owner();
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setPhoneNumber(phoneNumber);
        owner.setUser(user); //associates owner with the user

        Owner savedOwner = ownerRepository.save(owner);

        assignOwnerRole(user);

        return savedOwner;
    }

    public Owner createOwnerForCurrentUser(
            String firstName,
            String lastName,
            String phoneNumber
    ) {
        Integer userId = userService.getCurrentUserId();
        return createOwnerForUser(userId, firstName, lastName, phoneNumber);
    }

    public Integer getOwnerIdForCurrentUser() {
        Integer userId = userService.getCurrentUserId();

        return ownerRepository.findByUserId(userId)
                .map(Owner::getId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Owner profile not found for current user"
                        )
                );
    }

    private void assignOwnerRole(User user) {

        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "OWNER role not found in database"
                        )
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));

        listing.setOwner(owner);

        Integer currentUserId = userService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Role ownerRole = roleRepository.findByName("OWNER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        if (!currentUser.getRoles().contains(ownerRole)) {
            currentUser.getRoles().add(ownerRole);
            userService.updateUser(currentUser); //saves the user
        }
        listingRepository.save(listing);
    }

    @Transactional
    public void deactivateOwner(Integer ownerId) {

        Owner owner = getOwner(ownerId);

        if (!owner.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Owner already deactivated"
            );
        }

        List<Listing> listings = listingRepository.findByOwner(owner);

        for (Listing listing : listings) {

            if (listing.isRented()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Owner has active rented listings. Cannot deactivate."
                );
            }

            listing.disable();
        }

        owner.deactivate();
    }

    @Transactional
    public void unassignOwnerFromListing(Integer listingId) {

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Listing not found"
                ));
        listing.setOwner(null);
        listing.disable();
        listingRepository.save(listing);
    }

}
