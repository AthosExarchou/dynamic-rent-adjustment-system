package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.OwnerCreateRequest;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.services.OwnerService;
import gr.hua.dit.dras.services.UserService;
import gr.hua.dit.dras.repositories.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("owner")
public class OwnerController {

    private final OwnerService ownerService;
    private final UserService userService;
    private final RoleRepository roleRepository;

    public OwnerController(
            OwnerService ownerService,
            UserService userService,
            RoleRepository roleRepository
    ) {
        this.ownerService = ownerService;
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String createOwner(
            @Valid @ModelAttribute OwnerCreateRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "owner/ownerform";
        }

        ownerService.createOwnerForUser(
                request.getUserId(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
        );

        /* Filters out system user from the user list */
        List<User> users = userService.getUsers()
                .stream()
                .filter(u -> !"external-system".equals(u.getUsername()))
                .toList();

        model.addAttribute("users", users);
        model.addAttribute("roles", roleRepository.findAll());

        return "auth/users";
    }

    @GetMapping("/{id}/listings")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public String showListings(@PathVariable Integer id, Model model) {

        Owner owner = ownerService.getOwner(id);

        Integer currentUserId = userService.getCurrentUserId();
        boolean isAdmin = userService.currentUserHasRole("ADMIN");

        if (!isAdmin && !owner.getUser().getId().equals(currentUserId)) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        /* Protects system owner from direct UI access */
        if (owner.isSystemOwner()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "System owner listings cannot be viewed"
            );
        }

        /* Filters external listings */
        List<Listing> visibleListings = owner.getListings()
                .stream()
                .filter(l -> !l.isExternal() && l.getStatus() == ListingStatus.APPROVED)
                .collect(Collectors.toList());

        model.addAttribute("listings", visibleListings);
        return "listing/listings";
    }

}
