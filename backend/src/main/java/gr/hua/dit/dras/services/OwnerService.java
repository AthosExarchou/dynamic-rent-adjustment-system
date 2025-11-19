package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.ListingRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;

@Service
public class OwnerService {

    @Autowired
    private OwnerRepository ownerRepository;
    private RoleRepository roleRepository;
    private ListingRepository listingRepository;
    @Autowired
    private UserService userService;

    public OwnerService(RoleRepository roleRepository, OwnerRepository ownerRepository, ListingRepository listingRepository, UserService userService) {
        this.ownerRepository = ownerRepository;
        this.listingRepository = listingRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public List<Owner> getOwners() {
        return ownerRepository.findAll();
    }

    @Transactional
    public Owner getOwner(Integer ownerId) {

        Optional<Owner> optionalOwner = ownerRepository.findById(ownerId);

        if (optionalOwner.isPresent()) {
            return optionalOwner.get();
        } else {
            Integer currentUserId = userService.getCurrentUserId();
            return ownerRepository.findByUserId(currentUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found with current user ID: " + currentUserId));
        }
    }

    @Transactional
    public Owner getOwnerByAdmin(Integer ownerId, String firstName, String lastName, String phoneNumber) {

        User user = (User) userService.getUser(ownerId); //fetches current user by ID
        /* owner creation */
        Optional<Owner> existingOwner = ownerRepository.findByUserId(ownerId);

        if (existingOwner.isEmpty()) {
            Owner owner = new Owner();
            owner.setFirstName(firstName);
            owner.setLastName(lastName);
            owner.setEmail(user.getEmail());
            owner.setUsername(user.getUsername());
            owner.setPhoneNumber(phoneNumber);
            owner.setUser(user); //associates owner with the current user
            user = owner.getUser();
            user.setOwner(owner);
            Optional<Role> optionalRole = roleRepository.findByName("OWNER");

            if (optionalRole.isPresent()) {
                Role ownerRole = optionalRole.get();
                System.out.println("User roles before adding: " + user.getRoles());
                if (!user.getRoles().contains(ownerRole)) {
                    user.getRoles().add(ownerRole);
                }
                userService.updateUser(user);
                System.out.println("User roles after adding: " + user.getRoles());
            }
            return owner;
        }
        return null;

    }

    @Transactional
    public Owner createOwnerForCurrentUser(String firstName, String lastName, String phoneNumber) {

        Integer userId = userService.getCurrentUserId();
        User user = (User) userService.getUser(userId); //fetches current user by ID
        /* owner creation */
        Optional<Owner> existingOwner = ownerRepository.findByUserId(userId);

        if (existingOwner.isEmpty()) {
            Owner owner = new Owner();
            owner.setFirstName(firstName);
            owner.setLastName(lastName);
            owner.setEmail(user.getEmail());
            owner.setUsername(user.getUsername());
            owner.setPhoneNumber(phoneNumber);
            owner.setUser(user); //associates owner with the current user
            return ownerRepository.save(owner);
        } else {
            System.out.println("User with id: " + userId + " is an owner");
            return null;
        }
    }

    @Transactional
    public Integer getOwnerIdForCurrentUser() {

        Integer userId = userService.getCurrentUserId();
        System.out.println("Current User id: " + userId);
        Optional<Owner> ownerOptional = ownerRepository.findByUserId(userId);

        if (ownerOptional.isPresent()) {
            System.out.println("Owner id: " + ownerOptional.get().getId());
            return ownerOptional.get().getId();
        }
        System.out.println("No owner found for user id: " + userId);
        return null;
    }

    @Transactional
    public void saveOwner(Owner owner) {
        ownerRepository.save(owner);
    }

}
