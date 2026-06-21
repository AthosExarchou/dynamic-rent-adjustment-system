package gr.hua.dit.dras.controllers.owner;

/* imports */
import gr.hua.dit.dras.dto.OwnerCreateRequest;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.repositories.RoleRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OwnerControllerTest {

    @Mock
    private OwnerService ownerService;
    @Mock
    private UserService userService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ListingApplicationService listingApplicationService;

    @Mock
    private Model model;
    @Mock
    private RedirectAttributes redirectAttributes;
    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private OwnerController ownerController;

    private User testUser;
    private Owner testOwner;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");

        testOwner = new Owner();
        testOwner.setId(10);
        testOwner.setUser(testUser);

        testListing = new Listing();
        testListing.setId(100);
        testListing.setStatus(ListingStatus.APPROVED);
        testListing.setExternal(false);
        testOwner.setListings(List.of(testListing));
    }

    @Test
    @DisplayName("Should return users page for admin")
    void shouldReturnUsersPage() {
        User externalSystem = new User();
        externalSystem.setUsername("external-system");
        Role userRole = new Role("USER");
        when(userService.getUsers()).thenReturn(List.of(testUser, externalSystem));
        when(roleRepository.findAll()).thenReturn(List.of(userRole));

        String view = ownerController.usersPage(model);

        assertThat(view).isEqualTo("auth/users");
        verify(model).addAttribute("users", List.of(testUser));
        verify(model).addAttribute("roles", List.of(userRole));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when accessing system owner listings")
    void showListings_SystemOwner_ThrowsAccessDenied() {
        Owner systemOwner = new Owner();
        systemOwner.setSystemOwner(true);
        systemOwner.setUser(testUser);

        when(userService.getCurrentUserId()).thenReturn(testUser.getId());
        when(ownerService.getOwner(1)).thenReturn(systemOwner);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> ownerController.showListings(1, model))
            .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(model);
    }

    @Test
    @DisplayName("Should filter external and non-approved listings")
    void showListings_FiltersExternalAndNonApprovedListings() {
        Listing approvedLocal = new Listing();
        approvedLocal.setId(1);
        approvedLocal.setStatus(ListingStatus.APPROVED);
        approvedLocal.setExternal(false);

        Listing pendingLocal = new Listing();
        pendingLocal.setId(2);
        pendingLocal.setStatus(ListingStatus.PENDING);
        pendingLocal.setExternal(false);

        Listing externalListing = new Listing();
        externalListing.setId(3);
        externalListing.setStatus(ListingStatus.APPROVED);
        externalListing.setExternal(true);

        testOwner.setListings(List.of(approvedLocal, pendingLocal, externalListing));

        when(userService.getCurrentUserId()).thenReturn(testUser.getId());
        when(ownerService.getOwner(10)).thenReturn(testOwner);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        ownerController.showListings(10, model);

        verify(model).addAttribute(eq("listings"), argThat((List<Listing> list) -> 
            list.size() == 1 && list.contains(approvedLocal)
        ));
        verify(model, never()).addAttribute(eq("listings"), argThat((List<Listing> list) ->
                list.contains(pendingLocal) || list.contains(externalListing)
        ));
    }

    @Test
    @DisplayName("Should create owner and redirect on success")
    void shouldCreateOwnerOnSuccess() {
        when(bindingResult.hasErrors()).thenReturn(false);
        OwnerCreateRequest request = new OwnerCreateRequest();
        request.setUserId(1);
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhoneNumber("123");

        String view = ownerController.createOwner(request, bindingResult, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/users");
        verify(ownerService).createOwnerForUser(1, "John", "Doe", "123");
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "Owner created successfully.");
    }

    @Test
    @DisplayName("Should redirect back on validation errors when creating owner")
    void shouldRedirectBackOnValidationErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);
        OwnerCreateRequest request = new OwnerCreateRequest();

        String view = ownerController.createOwner(request, bindingResult, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/owner/new");
        verify(redirectAttributes).addFlashAttribute(
                "org.springframework.validation.BindingResult.ownerCreateRequest", bindingResult);
        verify(redirectAttributes).addFlashAttribute("ownerCreateRequest", request);
        verify(redirectAttributes).addFlashAttribute(
                "errorMessage", "Please correct the highlighted errors.");
        verifyNoInteractions(ownerService);
    }

    @Test
    @DisplayName("Should show listings if user is the owner")
    void shouldShowListingsForOwner() {
        when(ownerService.getOwner(10)).thenReturn(testOwner);
        when(userService.getCurrentUserId()).thenReturn(1);
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        String view = ownerController.showListings(10, model);

        assertThat(view).isEqualTo("listing/listings");
        verify(model).addAttribute(eq("listings"), argThat((List<Listing> list) -> 
            list.size() == 1 && list.contains(testListing)
        ));
    }

    @Test
    @DisplayName("Should throw AccessDenied if user is not the owner and not admin")
    void shouldThrowAccessDeniedIfNotOwnerAndNotAdmin() {
        when(ownerService.getOwner(10)).thenReturn(testOwner);
        when(userService.getCurrentUserId()).thenReturn(2); // different user
        when(userService.currentUserHasRole("ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> ownerController.showListings(10, model))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should show listings if user is admin even if not owner")
    void shouldShowListingsForAdmin() {
        when(ownerService.getOwner(10)).thenReturn(testOwner);
        when(userService.getCurrentUserId()).thenReturn(2); // different user
        when(userService.currentUserHasRole("ADMIN")).thenReturn(true);

        String view = ownerController.showListings(10, model);

        assertThat(view).isEqualTo("listing/listings");
        verify(model).addAttribute(eq("listings"), argThat((List<Listing> list) ->
                list.size() == 1 && list.contains(testListing)
        ));
    }

    @Test
    @DisplayName("Should approve tenant application and redirect")
    void shouldApproveTenantApplication() {
        String view = ownerController.approveTenantApplication(100, 5, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/listings/mylisting");
        verify(listingApplicationService).approveTenantApplication(100, 5);
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "Application approved successfully.");
    }

    @Test
    @DisplayName("Should reject tenant application and redirect")
    void shouldRejectTenantApplication() {
        String view = ownerController.rejectTenantApplication(100, 5, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/listings/mylisting");
        verify(listingApplicationService).rejectTenantApplication(100, 5);
        verify(redirectAttributes).addFlashAttribute(
                "successMessage", "Tenant application rejected successfully.");
    }
}
