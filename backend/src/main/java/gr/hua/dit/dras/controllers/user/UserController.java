package gr.hua.dit.dras.controllers.user;

/* imports */
import gr.hua.dit.dras.dto.AccountDeletionRequest;
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.services.application.UserApplicationService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import gr.hua.dit.dras.services.domain.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final UserApplicationService userApplicationService;

    public UserController(
            UserRepository userRepository,
            UserService userService,
            RoleRepository roleRepository,
            EmailService emailService,
            UserApplicationService userApplicationService
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.userApplicationService = userApplicationService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model) {

        /* Gets current user info */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            User currentUser = userService.getUserByEmail(auth.getName());
            model.addAttribute("currentUserId", currentUser.getId());

            boolean currentUserIsAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN"));
            model.addAttribute("currentUserIsAdmin", currentUserIsAdmin);
        } else {
            /* Not logged in or anonymous */
            model.addAttribute("currentUserId", null);
            model.addAttribute("currentUserIsAdmin", false);
        }
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "auth/register";
    }

    @PostMapping("/saveUser")
    public String saveUser(
            @Valid @ModelAttribute User user,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        /* Check if username already exists */
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            bindingResult.rejectValue(
                    "username",
                    "error.user",
                    "Username already taken!"
            );
        }

        /* Check if email already exists */
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            bindingResult.rejectValue(
                    "email",
                    "error.user",
                    "Email already registered!"
            );
        }

        /* If there are errors, show the form again */
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.user", bindingResult);
            redirectAttributes.addFlashAttribute("user", user);

            return "redirect:/auth/register";
        }

        Integer id = userService.saveUser(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("emailError",
                    "User saved, but notification email could not be sent.");
        }

        String message = "User '"+id+"' saved successfully !";
        redirectAttributes.addFlashAttribute("successMessage", message);

        return "redirect:/auth/login";
    }

    @Secured("ADMIN")
    @GetMapping("/users")
    public String showUsers(Model model) {

        model.addAttribute("users", userService.getUsers());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("currentUserId", userService.getCurrentUserId());

        return "auth/users";
    }

    @GetMapping("/user/{user_id}")
    public String showUser(@PathVariable Integer user_id, Model model) {

        User user = userService.getUser(user_id);
        userService.assertNotAdmin(user);

        model.addAttribute("user", user);
        return "auth/user";
    }

    @PostMapping("/user/{user_id}")
    public String editUser(
            @PathVariable Integer user_id,
            @ModelAttribute("user") User user,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        User the_user = userService.getUser(user_id);
        userService.assertNotAdmin(the_user);

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty or just spaces.");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty or just spaces.");
        }

        String oldUsername = the_user.getUsername();
        String oldEmail = the_user.getEmail();

        /* Who is currently logged in? */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        /* Checks for changes */
        boolean usernameChanged = !the_user.getUsername().equals(user.getUsername());
        boolean emailChanged = !the_user.getEmail().equals(user.getEmail());

        /* If nothing changed, add an info message and return early */
        if (!usernameChanged && !emailChanged) {
            redirectAttributes.addFlashAttribute("infoMessage",
                    "No changes were detected for user '" + the_user.getUsername() + "'.");
            return isAdmin ? "redirect:/auth/users" : "redirect:/auth/login";
        }

        /* Updates the user's information */
        the_user.setEmail(user.getEmail());
        the_user.setUsername(user.getUsername());
        userService.updateUser(the_user);

        /* Sends email notification to user */
        try {
            if (emailChanged) {
                /* Notify the old email address that details have changed */
                emailService.sendUserDetailsChangedEmail(
                        oldEmail, the_user.getUsername(), user.getEmail(),
                        oldUsername, oldEmail, usernameChanged, emailChanged
                );

                /* Notify the new email address */
                emailService.sendUserDetailsChangedEmail(
                        user.getEmail(), the_user.getUsername(), user.getEmail(),
                        oldUsername, oldEmail, usernameChanged, emailChanged
                );
            } else {
                /* Runs if only the username has changed */
                emailService.sendUserDetailsChangedEmail(
                        the_user.getEmail(), user.getUsername(), user.getEmail(),
                        oldUsername, oldEmail, usernameChanged, emailChanged
                );
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("emailError",
                    "User edited, but notification email could not be sent.");
        }

        if (!isAdmin && currentUsername.equals(oldUsername)) {
            session.invalidate();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Profile updated. Please log in again.");

            return "redirect:/auth/login";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "User updated successfully.");

        return "redirect:/auth/users";
    }

    @Secured("ADMIN")
    @PostMapping("/user/role/delete/{user_id}/{role_id}")
    public String deleteRolefromUser(
            @PathVariable Integer user_id,
            @PathVariable Integer role_id,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser(user_id);
        userService.assertNotAdmin(user);

        Role role = roleRepository.findById(role_id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + role_id));

        user.getRoles().remove(role);
        userService.updateUser(user);

        redirectAttributes.addFlashAttribute("successMessage",
                "Role removed successfully.");

        return "redirect:/auth/users";
    }

    @Secured("ADMIN")
    @PostMapping("/user/role/add/{user_id}/{role_id}")
    public String addRoletoUser(
            @PathVariable Integer user_id,
            @PathVariable Integer role_id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        /* Fetch user and assert they’re not admin */
        User user = userService.getUser(user_id);
        userService.assertNotAdmin(user);

        Role role = roleRepository.findById(role_id)
                .orElseThrow(() -> new IllegalStateException(
                        "Role not configured in the system: ID " + role_id)
                );

        switch (role.getName()) {
            case "OWNER":
                if (user.getOwner() != null) {
                    assignRole(user, "OWNER");
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Owner role added successfully.");

                    return "redirect:/auth/users";
                }
                model.addAttribute("owner", new Owner());
                model.addAttribute("userId", user_id);
                return "owner/ownerform";

            case "TENANT":
                if (user.getTenant() != null) {
                    assignRole(user, "TENANT");
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Tenant role added successfully.");

                    return "redirect:/auth/users";
                }
                Tenant tenant = new Tenant();
                tenant.setId(user.getId());
                model.addAttribute("tenant", tenant);
                model.addAttribute("userId", user_id);
                return "tenant/tenantformforadmin";

            case "USER":
                assignRole(user, "USER");
                redirectAttributes.addFlashAttribute("successMessage",
                        "User role added successfully.");

                return "redirect:/auth/users";

            default:
                throw new IllegalStateException("Unhandled role type: " + role.getName());
        }
    }

    /**
     * Safely assigns a role to a user if not already present, then persists the user.
     */
    private void assignRole(User user, String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not configured: " + roleName));
        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            userService.updateUser(user);
        }
    }

    /* Admin deletes a user's account */
    @Secured("ADMIN")
    @PostMapping("/user/delete/{user_id}")
    public String deleteUser(@PathVariable Integer user_id, RedirectAttributes redirectAttributes) {

        userApplicationService.deleteUserAsAdmin(user_id);

        redirectAttributes.addFlashAttribute("successMessage",
                "User deleted successfully.");

        return "redirect:/auth/users";
    }

    @Secured("USER")
    @GetMapping("/user/delete/self")
    public String showDeleteAccountForm(Model model) {
        if (!model.containsAttribute("deletionRequest")) {
            model.addAttribute("deletionRequest", new AccountDeletionRequest());
        }
        return "profile/delete-account";
    }

    /* Allows users to delete their own account */
    @Secured("USER")
    @PostMapping("/user/delete/self")
    public String deleteOwnAccount(
            @Valid @ModelAttribute("deletionRequest") AccountDeletionRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.deletionRequest", bindingResult);
            redirectAttributes.addFlashAttribute("deletionRequest", request);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please fill out all required fields.");

            return "redirect:/user/delete/self";
        }

        userApplicationService.deleteCurrentUserAccount(request);

        session.invalidate(); // force logout

        return "redirect:/";
    }

}
