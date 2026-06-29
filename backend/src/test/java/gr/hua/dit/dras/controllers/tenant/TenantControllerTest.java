package gr.hua.dit.dras.controllers.tenant;

import gr.hua.dit.dras.dto.TenantCreateRequest;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.domain.ListingService;
import gr.hua.dit.dras.services.domain.TenantService;
import gr.hua.dit.dras.services.domain.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TenantControllerTest {

    @Mock
    private TenantService tenantService;
    @Mock
    private UserService userService;
    @Mock
    private ListingService listingService;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private TenantController tenantController;

    private User currentUser;
    private Listing listing;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        ReflectionTestUtils.setField(currentUser, "id", 1);

        listing = new Listing();
        ReflectionTestUtils.setField(listing, "id", 10);
    }

    @Test
    void rentListing_NotTenant_Success() {
        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(false);

        Map<String, String> payload = new HashMap<>();
        payload.put("firstName", "John");
        payload.put("lastName", "Doe");
        payload.put("phoneNumber", "1234567890");

        ResponseEntity<?> response = tenantController.rentListing(listing.getId(), payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tenantService).createTenantForCurrentUser("John", "Doe", "1234567890");
        verify(tenantService).submitApplication(listing.getId());
    }

    @Test
    void rentListing_ExistingTenantWithoutApplication_SubmitsOnly() {
        Tenant existingTenant = new Tenant();
        existingTenant.setId(currentUser.getId());
        listing.setApplicants(new java.util.HashSet<>());

        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(true);
        when(tenantService.getTenant(currentUser.getId())).thenReturn(existingTenant);

        ResponseEntity<?> response = tenantController.rentListing(listing.getId(), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tenantService, never()).createTenantForCurrentUser(anyString(), anyString(), anyString());
        verify(tenantService).submitApplication(listing.getId());
    }

    @Test
    void rentListing_NotTenant_ValidationError() {
        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(false);

        Map<String, String> payload = new HashMap<>();
        payload.put("firstName", "");
        
        ResponseEntity<?> response = tenantController.rentListing(listing.getId(), payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(tenantService, never()).createTenantForCurrentUser(anyString(), anyString(), anyString());
        verify(tenantService, never()).submitApplication(anyInt());
    }

    @Test
    void rentListing_AlreadyTenant_AppliedForListing_ReturnsBadRequest() {
        Tenant existingTenant = new Tenant();
        existingTenant.setId(currentUser.getId());
        Set<Tenant> applicants = Set.of(existingTenant);
        listing.setApplicants(applicants);

        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(true);
        when(tenantService.getTenant(currentUser.getId())).thenReturn(existingTenant);

        ResponseEntity<?> response = tenantController.rentListing(listing.getId(), null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createTenant_Success() {
        TenantCreateRequest request = new TenantCreateRequest();
        request.setUserId(2);
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setPhoneNumber("0987654321");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(tenantService.createTenantForUser(
                2, "Jane", "Doe", "0987654321")).thenReturn(new Tenant());

        ResponseEntity<?> response = tenantController.createTenant(request, bindingResult);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tenantService).createTenantForUser(2, "Jane", "Doe", "0987654321");
    }

    @Test
    void createTenant_ValidationErrors() {
        TenantCreateRequest request = new TenantCreateRequest();
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("tenantCreateRequest", "error")));

        ResponseEntity<?> response = tenantController.createTenant(request, bindingResult);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(tenantService, never()).createTenantForUser(anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void createTenant_Fails_ReturnsBadRequest() {
        TenantCreateRequest request = new TenantCreateRequest();
        request.setUserId(2);
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setPhoneNumber("0987654321");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(tenantService.createTenantForUser(
                2, "Jane", "Doe", "0987654321")).thenReturn(null);

        ResponseEntity<?> response = tenantController.createTenant(request, bindingResult);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
