package gr.hua.dit.dras.services.domain;

/* imports */
import gr.hua.dit.dras.entities.*;
import gr.hua.dit.dras.repositories.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ListingRepository listingRepository;
    private final TenantRepository tenantRepository;
    private final OwnerRepository  ownerRepository;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            BCryptPasswordEncoder passwordEncoder,
            ListingRepository listingRepository,
            TenantRepository tenantRepository,
            OwnerRepository  ownerRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.listingRepository = listingRepository;
        this.tenantRepository = tenantRepository;
        this.ownerRepository = ownerRepository;
    }

    @Transactional
    public Integer saveUser(User user) {
        String passwd= user.getPassword();
        String encodedPassword = passwordEncoder.encode(passwd);
        user.setPassword(encodedPassword);

        Role role = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("USER role not found."));
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        userRepository.save(user);
        return user.getId();
    }

    @Transactional
    public Integer updateUser(User user) {
        userRepository.save(user);
        return user.getId();
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> opt = userRepository.findByEmail(username);

        if(opt.isEmpty())
            throw new UsernameNotFoundException("User with name: " +username +" not found !");
        else {
            User user = opt.get();
            return new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    user.getRoles()
                            .stream()
                            .map(role-> new SimpleGrantedAuthority(role.toString()))
                            .collect(Collectors.toSet())
            );
        }
    }

    @Transactional(readOnly = true)
    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("User is not authenticated.");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "User not found: " + authentication.getName()
                ));
        return user.getId();
    }

    @Transactional(readOnly = true)
    public boolean isUserOwner() {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new IllegalStateException("User id must not be null");
        }
        User currentUser = getUser(currentUserId);
        for (Role role : currentUser.getRoles()) {
            if ("OWNER".equals(role.getName())) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        Optional<Role> ownerRole = roleRepository.findByName("OWNER");
        if (ownerRole.isPresent() && user.getRoles().contains(ownerRole.get())) {
            if (user.getOwner() != null) {
                List<Listing> listings = user.getOwner().getListings();
                if (listings != null) {
                    for (Listing listing : listings) {
                        listing.setOwner(null);
                        if (listing.isRented()) {
                            Tenant tenant = listing.getTenant();
                            listing.setTenant(null);
                            listing.setApplicants(null);
                            tenant.setListing(null);
                        }
                        tenantRepository.deleteApplicationsByListingId(listing.getId());
                        listingRepository.save(listing);
                        listingRepository.delete(listing);
                    }
                }
                ownerRepository.delete(user.getOwner());
            }
        }

        Optional<Role> tenantRole = roleRepository.findByName("TENANT");
        if (tenantRole.isPresent() && user.getRoles().contains(tenantRole.get())) {
            if (user.getTenant() != null) {
                Tenant tenant = user.getTenant();
                if (tenant.getListing() != null) {
                    tenant.getListing().setTenant(null);
                    tenant.setListing(null);
                }
                tenantRepository.delete(user.getTenant());
            }
        }

        userRepository.delete(user);
    }


    @Transactional(readOnly = true)
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "User with id " + userId + " not found"
                ));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public boolean currentUserHasRole(String roleName) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        User currentUser = getUser(currentUserId);
        return currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().equals(roleName));
    }

    @Transactional(readOnly = true)
    public Optional<User> getCurrentUserOptional() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() ||
                auth instanceof AnonymousAuthenticationToken) {

            return Optional.empty(); // unauthenticated
        }
        return userRepository.findByEmail(auth.getName());
    }

    @Transactional(readOnly = true)
    public void assertNotAdmin(User user) {
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN"));

        if (isAdmin) {
            throw new IllegalStateException("Administrator account cannot be modified.");
        }
    }

}
