package gr.hua.dit.dras.controllers.owner;

import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.services.application.ListingApplicationService;
import gr.hua.dit.dras.services.domain.OwnerService;
import gr.hua.dit.dras.services.domain.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerControllerTest {

    @Mock
    private OwnerService ownerService;
    @Mock
    private UserService userService;
    @Mock
    private ListingApplicationService listingApplicationService;

    @InjectMocks
    private OwnerController ownerController;

    private User testUser;
    private Owner testOwner;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", 1);
        testUser.setUsername("testuser");

        testOwner = new Owner();
        ReflectionTestUtils.setField(testOwner, "id", 10);
        testOwner.setUser(testUser);

        testListing = new Listing();
        ReflectionTestUtils.setField(testListing, "id", 100);
        testListing.setTitle("Test Listing");
        testListing.setStatus(ListingStatus.APPROVED);
        testListing.setExternal(false);
        testOwner.setListings(List.of(testListing));
    }

    @Test
    @DisplayName("Should return 403 when accessing system owner listings")
    void showListings_SystemOwner_Returns403() {
        Owner systemOwner = new Owner();
        systemOwner.setSystemOwner(true);
        systemOwner.setUser(testUser);

        when(userService.getCurrentUserId()).thenReturn(testUser.getId());
        when(ownerService.getOwner(1)).thenReturn(systemOwner);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        ResponseEntity<?> response = ownerController.showListings(1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should filter external and non-approved listings")
    void showListings_FiltersExternalAndNonApprovedListings() {
        Listing approvedLocal = new Listing();
        ReflectionTestUtils.setField(approvedLocal, "id", 1);
        approvedLocal.setTitle("Approved Local");
        approvedLocal.setStatus(ListingStatus.APPROVED);
        approvedLocal.setExternal(false);

        Listing pendingLocal = new Listing();
        ReflectionTestUtils.setField(pendingLocal, "id", 2);
        pendingLocal.setStatus(ListingStatus.PENDING);
        pendingLocal.setExternal(false);

        Listing externalListing = new Listing();
        ReflectionTestUtils.setField(externalListing, "id", 3);
        externalListing.setStatus(ListingStatus.APPROVED);
        externalListing.setExternal(true);

        testOwner.setListings(List.of(approvedLocal, pendingLocal, externalListing));

        when(userService.getCurrentUserId()).thenReturn(testUser.getId());
        when(ownerService.getOwner(10)).thenReturn(testOwner);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        ResponseEntity<?> response = ownerController.showListings(10);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> body = (List<?>) response.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    @DisplayName("Should create owner and return 200 on success")
    void shouldCreateOwnerOnSuccess() {
        Map<String, String> request = new HashMap<>();
        request.put("userId", "1");
        request.put("firstName", "John");
        request.put("lastName", "Doe");
        request.put("phoneNumber", "123");

        when(userService.getCurrentUserId()).thenReturn(1);

        ResponseEntity<?> response = ownerController.createOwner(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(ownerService).createOwnerForUser(1, "John", "Doe", "123");
    }

    @Test
    @DisplayName("Should return bad request on missing fields")
    void shouldReturnBadRequestOnMissingFields() {
        Map<String, String> request = new HashMap<>();
        request.put("userId", "1");

        ResponseEntity<?> response = ownerController.createOwner(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(ownerService);
    }

    @Test
    @DisplayName("Should show listings if user is the owner")
    void shouldShowListingsForOwner() {
        when(ownerService.getOwner(10)).thenReturn(testOwner);
        when(userService.getCurrentUserId()).thenReturn(1);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        ResponseEntity<?> response = ownerController.showListings(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should return 403 if user is not the owner and not admin")
    void shouldThrowAccessDeniedIfNotOwnerAndNotAdmin() {
        when(ownerService.getOwner(10)).thenReturn(testOwner);
        when(userService.getCurrentUserId()).thenReturn(2); // different user
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        ResponseEntity<?> response = ownerController.showListings(10);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should show listings if user is admin even if not owner")
    void shouldShowListingsForAdmin() {
        when(ownerService.getOwner(10)).thenReturn(testOwner);
        when(userService.getCurrentUserId()).thenReturn(2); // different user
        when(userService.currentUserHasRole("ADMIN")).thenReturn(true);

        ResponseEntity<?> response = ownerController.showListings(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should approve tenant application and return 200")
    void shouldApproveTenantApplication() {
        ResponseEntity<?> response = ownerController.approveTenantApplication(100, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(listingApplicationService).approveTenantApplication(100, 5);
    }

    @Test
    @DisplayName("Should reject tenant application and return 200")
    void shouldRejectTenantApplication() {
        ResponseEntity<?> response = ownerController.rejectTenantApplication(100, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(listingApplicationService).rejectTenantApplication(100, 5);
    }
}
