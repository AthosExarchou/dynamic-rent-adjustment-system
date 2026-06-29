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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    @Mock
    private OwnerRepository ownerRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserService userService;
    @Mock
    private ListingRepository listingRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OwnerService ownerService;

    private User testUser;
    private Owner testOwner;
    private Role ownerRole;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", 1);
        testUser.setUsername("testuser");

        testOwner = new Owner();
        ReflectionTestUtils.setField(testOwner, "id", 10);
        testOwner.setUser(testUser);
        
        ownerRole = new Role("OWNER");
        
        testListing = new Listing();
        ReflectionTestUtils.setField(testListing, "id", 100);
    }

    @Test
    @DisplayName("Should successfully get Owner by ID")
    void shouldGetOwnerById() {
        when(ownerRepository.findById(10)).thenReturn(Optional.of(testOwner));

        Owner result = ownerService.getOwner(10);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
        verify(ownerRepository).findById(10);
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should get current user's Owner when ID is null")
    void shouldGetOwnerForCurrentUserWhenIdIsNull() {
        when(userService.getCurrentUserId()).thenReturn(1);
        when(ownerRepository.findByUserId(1)).thenReturn(Optional.of(testOwner));

        Owner result = ownerService.getOwner(null);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
        verify(ownerRepository).findByUserId(1);
    }

    @Test
    @DisplayName("Should create Owner for user and assign role")
    void shouldCreateOwnerForUser() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ownerRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(ownerRepository.save(any(Owner.class))).thenAnswer(i -> {
            Owner o = i.getArgument(0);
            ReflectionTestUtils.setField(o, "id", 20);
            return o;
        });
        when(roleRepository.findByName("OWNER")).thenReturn(Optional.of(ownerRole));

        Owner savedOwner = ownerService.createOwnerForUser(
                1, "John", "Doe", "1234567890");

        assertThat(savedOwner).isNotNull();
        assertThat(savedOwner.getId()).isEqualTo(20);
        assertThat(savedOwner.getFirstName()).isEqualTo("John");
        assertThat(testUser.getRoles()).contains(ownerRole);
        verify(ownerRepository).save(any(Owner.class));
        verify(userService).updateUser(testUser);
    }

    @Test
    @DisplayName("Should throw if User already has an Owner profile")
    void shouldThrowIfUserAlreadyHasOwnerProfile() {
        when(userService.getUser(1)).thenReturn(testUser);
        when(ownerRepository.findByUserId(1)).thenReturn(Optional.of(testOwner));

        assertThatThrownBy(() -> ownerService.createOwnerForUser(
                1, "John", "Doe", "1234567890"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User already has an Owner profile");
        
        verify(ownerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should assign Owner to Listing and ensure user has role")
    void shouldAssignOwnerToListing() {
        when(listingRepository.findById(100)).thenReturn(Optional.of(testListing));

        when(roleRepository.findByName("OWNER")).thenReturn(Optional.of(ownerRole));

        ownerService.assignOwnerToListing(100, testOwner);

        assertThat(testListing.getOwner()).isEqualTo(testOwner);
        assertThat(testUser.getRoles()).contains(ownerRole);
        verify(userService).updateUser(testUser);
        verify(listingRepository).save(testListing);
    }

    @Test
    @DisplayName("Should unassign Owner from Listing and disable it")
    void shouldUnassignOwnerFromListing() {
        testListing.setOwner(testOwner);
        when(listingRepository.findById(100)).thenReturn(Optional.of(testListing));

        ownerService.unassignOwnerFromListing(100);

        assertThat(testListing.getOwner()).isNull();
        assertThat(testListing.isDisabled()).isTrue();
        verify(listingRepository).save(testListing);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when owner not found by ID")
    void shouldThrowWhenOwnerNotFound() {
        when(ownerRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerService.getOwner(999))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Owner not found");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when current user has no owner profile")
    void shouldThrowWhenCurrentUserHasNoOwnerProfile() {
        when(userService.getCurrentUserId()).thenReturn(999);
        when(ownerRepository.findByUserId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerService.getOwner(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Owner not found for current user");
    }
}
