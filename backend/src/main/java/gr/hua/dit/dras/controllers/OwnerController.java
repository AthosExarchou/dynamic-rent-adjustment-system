package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.model.enums.ListingStatus;
import gr.hua.dit.dras.services.OwnerService;
import gr.hua.dit.dras.services.UserService;
import gr.hua.dit.dras.repositories.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("owner")
public class OwnerController {

    private final OwnerService ownerService;
    private final UserService userService;
    private final RoleRepository roleRepository;

    public OwnerController(OwnerService ownerService, UserService userService, RoleRepository roleRepository) {
        this.ownerService = ownerService;
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/new")
    public String saveOwner(@Valid @RequestParam("userId") Integer userId,
                            @RequestParam(value = "firstName", required = false) String firstName,
                            @RequestParam(value = "lastName", required = false) String lastName,
                            @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
                            Model model) {

        if (firstName == null || lastName == null || phoneNumber == null) {
            model.addAttribute("errorMessage",
                    "First name, last name, and phone number are required for new owner.");
            return "owner/ownerform";
        }

        User user = userService.getUser(userId);
        if (user == null || "external-system".equals(user.getUsername())) {
            model.addAttribute("errorMessage", "User not found or cannot assign system owner.");
            return "auth/users";
        }

        Owner owner = ownerService.getOwnerByAdmin(userId, firstName, lastName, phoneNumber);
        if (owner == null) {
            model.addAttribute("errorMessage",
                    "Your role as 'OWNER' has been revoked by the Administrator. " +
                            "Please contact us for further details.");
            return "listing/listing";
        }

        ownerService.saveOwner(owner);

        /* Filters out system user from the user list */
        List<User> users = userService.getUsers()
                .stream()
                .filter(u -> !"external-system".equals(u.getUsername()))
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        model.addAttribute("roles", roleRepository.findAll());
        return "auth/users";
    }

    @GetMapping("/{id}/listings")
    public String showListings(@PathVariable("id") Integer id, Model model) {

        Owner owner = ownerService.getOwner(id);

        if (owner == null) {
            model.addAttribute("errorMessage", "Owner not found.");
            return "error/404";
        }

        /* Protects system owner from direct UI access */
        if (owner.isSystemOwner()) {
            model.addAttribute("errorMessage", "External/System listings cannot be viewed directly.");
            return "error/403";
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
