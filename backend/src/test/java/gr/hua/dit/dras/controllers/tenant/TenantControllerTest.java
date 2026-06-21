package gr.hua.dit.dras.controllers.tenant;

/* imports */
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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private Model model;
    @Mock
    private BindingResult bindingResult;
    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private TenantController tenantController;

    private User currentUser;
    private Listing listing;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1);

        listing = new Listing();
        listing.setId(10);
    }

    @Test
    void showTenantForm_NotAlreadyTenant_Success() {
        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(false);
        when(model.containsAttribute("tenant")).thenReturn(false);

        String viewName = tenantController.showTenantForm(listing.getId(), model);

        assertEquals("tenant/tenantform", viewName);
        verify(tenantService).validateRentalApplicationRights(currentUser, listing);
        verify(model).addAttribute(eq("tenant"), any(Tenant.class));
        verify(model).addAttribute("listingId", listing.getId());
        verify(model).addAttribute("isAlreadyTenant", false);
    }

    @Test
    void showTenantForm_AlreadyTenantWithNoListing_Success() {
        Tenant existingTenant = new Tenant();
        existingTenant.setId(currentUser.getId());

        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(true);
        when(tenantService.getTenant(currentUser.getId())).thenReturn(existingTenant);

        String viewName = tenantController.showTenantForm(listing.getId(), model);

        assertEquals("tenant/tenantform", viewName);
        verify(tenantService).validateRentalApplicationRights(currentUser, listing);
        verify(model, never()).addAttribute(eq("tenant"), any(Tenant.class));
        verify(model).addAttribute("listingId", listing.getId());
        verify(model).addAttribute("isAlreadyTenant", true);
    }

    @Test
    void showTenantForm_AlreadyTenantWithListing_ThrowsException() {
        Tenant existingTenant = new Tenant();
        existingTenant.setId(currentUser.getId());
        existingTenant.setListing(new Listing());

        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(true);
        when(tenantService.getTenant(currentUser.getId())).thenReturn(existingTenant);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            tenantController.showTenantForm(listing.getId(), model);
        });

        assertEquals("You already rent a listing.", exception.getMessage());
    }

    @Test
    void rentListing_NotTenant_Success() {
        Tenant tenant = new Tenant();
        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(false);
        when(bindingResult.hasErrors()).thenReturn(false);

        String viewName = tenantController.rentListing(
                listing.getId(), tenant, bindingResult,
                " John ", " Doe ", " 1234567890 ", redirectAttributes);

        assertEquals("redirect:/listings", viewName);
        verify(tenantService).createTenantForCurrentUser(
                "John", "Doe", "1234567890");
        verify(tenantService).submitApplication(listing.getId());
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "Application submitted successfully.");
    }

    @Test
    void rentListing_ExistingTenantWithoutApplication_SubmitsOnly() {
        Tenant tenant = new Tenant();
        Tenant existingTenant = new Tenant();
        existingTenant.setId(currentUser.getId());
        listing.setApplicants(new java.util.HashSet<>());

        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(true);
        when(tenantService.getTenant(currentUser.getId())).thenReturn(existingTenant);

        String viewName = tenantController.rentListing(
                listing.getId(), tenant, bindingResult,
                null, null, null, redirectAttributes);

        assertEquals("redirect:/listings", viewName);
        verify(tenantService, never()).createTenantForCurrentUser(anyString(), anyString(), anyString());
        verify(tenantService).submitApplication(listing.getId());
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "Application submitted successfully.");
    }

    @Test
    void rentListing_NotTenant_ValidationError() {
        Tenant tenant = new Tenant();
        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(false);
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = tenantController.rentListing(
                listing.getId(), tenant, bindingResult,
                "John", "Doe", "1234567890", redirectAttributes);

        assertEquals("redirect:/tenant/rent/" + listing.getId(), viewName);
        verify(redirectAttributes).addFlashAttribute(
                "org.springframework.validation.BindingResult.tenant", bindingResult);
        verify(redirectAttributes).addFlashAttribute("tenant", tenant);
        verify(redirectAttributes).addFlashAttribute(
                "errorMessage", "Invalid form data. All fields are required.");
        verify(tenantService, never()).createTenantForCurrentUser(anyString(), anyString(), anyString());
        verify(tenantService, never()).submitApplication(anyInt());
    }

    @Test
    void rentListing_AlreadyTenant_AppliedForListing_ThrowsException() {
        Tenant tenant = new Tenant();
        Tenant existingTenant = new Tenant();
        existingTenant.setId(currentUser.getId());
        Set<Tenant> applicants = Set.of(existingTenant);
        listing.setApplicants(applicants);

        when(userService.getCurrentUserId()).thenReturn(currentUser.getId());
        when(userService.getUser(currentUser.getId())).thenReturn(currentUser);
        when(listingService.getListing(listing.getId())).thenReturn(listing);
        when(tenantService.isUserTenant()).thenReturn(true);
        when(tenantService.getTenant(currentUser.getId())).thenReturn(existingTenant);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            tenantController.rentListing(
                    listing.getId(), tenant, bindingResult,
                    null, null, null, redirectAttributes
            );
        });

        assertEquals("You have already applied for this listing.", exception.getMessage());
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

        String viewName = tenantController.createTenant(request, bindingResult, redirectAttributes);

        assertEquals("redirect:/users", viewName);
        verify(tenantService).createTenantForUser(2, "Jane", "Doe", "0987654321");
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "Tenant created successfully.");
    }

    @Test
    void createTenant_ValidationErrors() {
        TenantCreateRequest request = new TenantCreateRequest();
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = tenantController.createTenant(request, bindingResult, redirectAttributes);

        assertEquals("redirect:/tenants/new", viewName);
        verify(redirectAttributes).addFlashAttribute(
                "org.springframework.validation.BindingResult.tenantCreateRequest", bindingResult);
        verify(redirectAttributes).addFlashAttribute("tenantCreateRequest", request);
        verify(redirectAttributes).addFlashAttribute(
                "errorMessage", "Please correct the highlighted errors.");
        verify(tenantService, never()).createTenantForUser(anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void createTenant_Fails_ThrowsException() {
        TenantCreateRequest request = new TenantCreateRequest();
        request.setUserId(2);
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setPhoneNumber("0987654321");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(tenantService.createTenantForUser(
                2, "Jane", "Doe", "0987654321")).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            tenantController.createTenant(request, bindingResult, redirectAttributes);
        });

        assertEquals("Tenant role revoked or creation failed.", exception.getMessage());
    }
}
