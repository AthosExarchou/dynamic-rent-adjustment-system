package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.entities.Owner;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.Tenant;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.repositories.OwnerRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.repositories.TenantRepository;
import gr.hua.dit.dras.services.EmailService;
import gr.hua.dit.dras.services.OwnerService;
import gr.hua.dit.dras.services.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final OwnerService ownerService;
    private final OwnerRepository ownerRepository;
    private final TenantRepository tenantRepository;
    private UserService userService;
    private RoleRepository roleRepository;
    private EmailService emailService;

    public UserController(UserRepository userRepository, UserService userService,
                          RoleRepository roleRepository, OwnerService ownerService,
                          OwnerRepository ownerRepository, TenantRepository tenantRepository,
                          EmailService emailService) {

        this.userRepository = userRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.ownerService = ownerService;
        this.ownerRepository = ownerRepository;
        this.tenantRepository = tenantRepository;
        this.emailService = emailService;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model) {

        /* Get current user info */
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
        User user = new User();
        model.addAttribute("user", user);
        return "auth/register";
    }

    @PostMapping("/saveUser")
    public String saveUser(@Valid @ModelAttribute User user,
                           BindingResult bindingResult, Model model) {

        System.out.println("Roles: "+user.getRoles());

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
            return "auth/register";
        }

        Integer id = userService.saveUser(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user);
        } catch (Exception e) {
            model.addAttribute("emailError", "User saved, but notification email could not be sent.");
        }

        String message = "User '"+id+"' saved successfully !";
        model.addAttribute("msg", message);
        return "auth/login";
    }

    @Secured("ADMIN")
    @GetMapping("/users")
    public String showUsers(Model model) {

        model.addAttribute("users", userService.getUsers());
        model.addAttribute("roles", roleRepository.findAll());
        Integer currentUserId = userService.getCurrentUserId();
        model.addAttribute("currentUserId", currentUserId);
        return "auth/users";
    }

    @GetMapping("/user/{user_id}")
    public String showUser(@PathVariable Integer user_id, Model model) {

        User user = (User) userService.getUser(user_id);
        System.out.println(user);
        model.addAttribute("user", user);
        return "auth/user";
    }

    @PostMapping("/user/{user_id}")
    public String editUser(@PathVariable Integer user_id,
                           @ModelAttribute("user") User user,
                           Model model, HttpSession session) {

        User the_user = (User) userService.getUser(user_id);

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            model.addAttribute("errorMessage", "Username cannot be empty or just spaces.");
            model.addAttribute("user", the_user);
            return "profile/edit-profile"; //return to form
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            model.addAttribute("errorMessage", "Email cannot be empty or just spaces.");
            model.addAttribute("user", the_user);
            return "profile/edit-profile"; //return to form
        }

        String oldUsername = the_user.getUsername();
        String oldEmail = the_user.getEmail();

        /* Who is currently logged in? */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        /* checks for changes */
        boolean usernameChanged = !the_user.getUsername().equals(user.getUsername());
        boolean emailChanged = !the_user.getEmail().equals(user.getEmail());

        /* If nothing changed, add an info message and return early */
        if (!usernameChanged && !emailChanged) {
            model.addAttribute("infoMessage", "No changes were detected for user '" + the_user.getUsername() + "'.");
            model.addAttribute("users", userService.getUsers());
            if (isAdmin) {
                return "auth/users"; //admin stays on user management page
            } else {
                session.invalidate(); //force logout if user changed own credentials
                return "auth/login";
            }
        }

        /* updates the user's information */
        the_user.setEmail(user.getEmail());
        the_user.setUsername(user.getUsername());
        userService.updateUser(the_user);
        System.out.println("Edited: "+ the_user);

        /* sends email notification to user */
        try {
            if (emailChanged) {
                /* Notify the old email address that details have changed */
                emailService.sendUserDetailsChangedEmail(
                        oldEmail, the_user.getUsername(), user.getEmail(),
                        oldUsername, oldEmail, usernameChanged, emailChanged
                );

                /* Also notify the new email address */
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
            if (!isAdmin && currentUsername.equals(oldUsername)) {
                session.invalidate(); //only invalidate if user changed own credentials
            }
        } catch (Exception e) {
            model.addAttribute("emailError", "User edited, but notification email could not be sent.");
        }

        model.addAttribute("users", userService.getUsers());
        return isAdmin ? "auth/users" : "auth/login";
    }

    @Secured("ADMIN")
    @GetMapping("/user/role/delete/{user_id}/{role_id}")
    public String deleteRolefromUser(@PathVariable Integer user_id, @PathVariable Integer role_id, Model model) {

        User user = (User) userService.getUser(user_id);
        Role role = roleRepository.findById(role_id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + role_id));
        user.getRoles().remove(role);
        System.out.println("Roles: "+user.getRoles());
        userService.updateUser(user);

        model.addAttribute("users", userService.getUsers());
        model.addAttribute("roles", roleRepository.findAll());
        return "auth/users";
    }

    @Secured("ADMIN")
    @GetMapping("/user/role/add/{user_id}/{role_id}")
    public String addRoletoUser(@PathVariable Integer user_id, @PathVariable Integer role_id, Model model) {

        User user = (User) userService.getUser(user_id);
        if (role_id.equals(4)) {
            if (user.getOwner() != null) {
                Optional<Role> optionalRole = roleRepository.findByName("OWNER");

                if (optionalRole.isPresent()) {
                    Role ownerRole = optionalRole.get();
                    System.out.println("User roles before adding: " + user.getRoles());

                    if (!user.getRoles().contains(ownerRole)) {
                        user.getRoles().add(ownerRole);
                    }
                    userService.updateUser(user);
                    System.out.println("User roles after adding: " + user.getRoles());
                    model.addAttribute("users", userService.getUsers());
                    model.addAttribute("roles", roleRepository.findAll());
                }
                return "auth/users";
            }

            Owner owner = new Owner();
            model.addAttribute("owner", owner);
            model.addAttribute("userId", user_id);
            return "owner/ownerform";
        } else if (role_id.equals(3)) {
            if (user.getTenant() != null) {
                Optional<Role> optionalRole = roleRepository.findByName("TENANT");

                if (optionalRole.isPresent()) {
                    Role tenantRole = optionalRole.get();
                    System.out.println("User roles before adding: " + user.getRoles());
                    if (!user.getRoles().contains(tenantRole)) {
                        user.getRoles().add(tenantRole);
                    }
                    userService.updateUser(user);
                    System.out.println("User roles after adding: " + user.getRoles());
                    model.addAttribute("users", userService.getUsers());
                    model.addAttribute("roles", roleRepository.findAll());
                }
                return "auth/users";
            }

            Tenant tenant = new Tenant();
            tenant.setId(user.getId());
            tenant.setEmail(user.getEmail());
            model.addAttribute("tenant", tenant);
            model.addAttribute("userId", user_id);
            return "tenant/tenantformforadmin";
        } else if (role_id.equals(1)) {
            if (user != null) {
                Optional<Role> optionalRole = roleRepository.findByName("USER");

                if (optionalRole.isPresent()) {
                    Role tenantRole = optionalRole.get();
                    System.out.println("User roles before adding: " + user.getRoles());

                    if (!user.getRoles().contains(tenantRole)) {
                        user.getRoles().add(tenantRole);
                    }
                    userService.updateUser(user);
                    System.out.println("User roles after adding: " + user.getRoles());
                    model.addAttribute("users", userService.getUsers());
                    model.addAttribute("roles", roleRepository.findAll());
                }
                return "auth/users";
            }
        }
        model.addAttribute("users", userService.getUsers());
        model.addAttribute("roles", roleRepository.findAll());
        return "auth/users";
    }

    /* Admin deletes a user's account */
    @Secured("ADMIN")
    @GetMapping("/user/delete/{user_id}")
    public String deleteUser(@PathVariable Integer user_id, Model model) {

        User user = (User) userService.getUser(user_id);
        Optional<Role> adminRole = roleRepository.findByName("ADMIN");

        if (adminRole.isPresent() && user.getRoles().contains(adminRole.get())) {
            model.addAttribute("errorMessage", "You do not have the permission to delete this user!.");
            return "index";
        }

        /* sends email before deleting the user */
        try {
            emailService.sendAccountDeletionEmail(user.getEmail(), user);
            System.out.println("Account deletion email sent to user.");
        } catch (Exception e) {
            model.addAttribute("emailError", "User deleted, but email could not be sent.");
            System.out.println("Failed to send account deletion email.");
            e.printStackTrace();
        }

        userService.deleteUser(user_id);
        return "index";
    }

    /* Allows users to delete their own account */
    @Secured("USER")
    @PostMapping("/user/delete/self")
    public String deleteOwnAccount(Authentication authentication, Model model, HttpSession session) {

        String email = authentication.getName();
        User currentUser = userService.getUserByEmail(email);

        Optional<Role> adminRole = roleRepository.findByName("ADMIN");
        if (adminRole.isPresent() && currentUser.getRoles().contains(adminRole.get())) {
            model.addAttribute("errorMessage", "You do not have the permission to delete this user!.");
            return "index";
        }

        try {
            emailService.sendAccountDeletionEmail(currentUser.getEmail(), currentUser);
        } catch (Exception e) {
            model.addAttribute("emailError", "Account deleted, but email could not be sent.");
            e.printStackTrace();
        }
        session.invalidate(); //force logout
        userService.deleteUser(currentUser.getId());

        return "index";
    }

}
