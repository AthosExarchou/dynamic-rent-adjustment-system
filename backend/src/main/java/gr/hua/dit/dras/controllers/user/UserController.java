package gr.hua.dit.dras.controllers.user;

/* imports */
import gr.hua.dit.dras.dto.AccountDeletionRequest;
import gr.hua.dit.dras.dto.UserEditRequest;
import gr.hua.dit.dras.entities.Role;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.repositories.RoleRepository;
import gr.hua.dit.dras.services.application.UserApplicationService;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import gr.hua.dit.dras.services.domain.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final UserApplicationService userApplicationService;
    private final SessionRegistry sessionRegistry;

    public UserController(
            UserRepository userRepository,
            UserService userService,
            RoleRepository roleRepository,
            EmailService emailService,
            UserApplicationService userApplicationService,
            SessionRegistry sessionRegistry
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.userApplicationService = userApplicationService;
        this.sessionRegistry = sessionRegistry;
    }

    @PostMapping("/saveUser")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> saveUser(
            @Valid @ModelAttribute User user,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return org.springframework.http.ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Validation failed", "details", bindingResult.getAllErrors())
            );
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return org.springframework.http.ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Username already taken!")
            );
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return org.springframework.http.ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Email already registered!")
            );
        }

        Integer id = userService.saveUser(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user);
        } catch (Exception e) {
            // Log it, but user is saved
        }

        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Secured("ADMIN")
    @GetMapping("/users")
    @ResponseBody
    public List<gr.hua.dit.dras.dto.UserDTO> showUsers() {
        return userService.getUsers().stream()
                .map(gr.hua.dit.dras.dto.UserDTO::new)
                .collect(java.util.stream.Collectors.toList());
    }

    @PreAuthorize("hasAuthority('ADMIN') or @userService.getCurrentUserId() == #user_id")
    @GetMapping("/user/{user_id}")
    @ResponseBody
    public gr.hua.dit.dras.dto.UserDTO showUser(@PathVariable Integer user_id) {
        User user = userService.getUser(user_id);
        userService.assertNotAdmin(user);
        return new gr.hua.dit.dras.dto.UserDTO(user);
    }

    @Secured("USER")
    @PostMapping("/user/{user_id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> editUser(
            @PathVariable("user_id") Integer targetUserId,
            @Valid @ModelAttribute("userEditRequest") UserEditRequest request,
            BindingResult bindingResult,
            HttpSession session
    ) {
        // BUG-B04 FIX: Perform the authorization check FIRST, before any DB lookups.
        // This prevents a non-admin user from probing username/email existence
        // for arbitrary user IDs via the uniqueness-check queries below.
        Authentication authEarly = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminEarly = authEarly.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        boolean isSelfEditEarly = userService.getCurrentUserId().equals(targetUserId);

        if (!isAdminEarly && !isSelfEditEarly) {
            return org.springframework.http.ResponseEntity.status(403)
                    .body(java.util.Map.of("error", "Access denied."));
        }

        if (userService.isUsernameTaken(request.getUsername(), targetUserId)) {
            bindingResult.rejectValue("username",
                    "error.userEditRequest", "This username is already taken.");
        }

        if (userService.isEmailTaken(request.getEmail(), targetUserId)) {
            bindingResult.rejectValue("email",
                    "error.userEditRequest", "This email is already registered.");
        }

        if (bindingResult.hasErrors()) {
            return org.springframework.http.ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Validation failed", "details", bindingResult.getAllErrors())
            );
        }

        boolean changesMade = userApplicationService.editUser(targetUserId, request);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        boolean isSelfEdit = userService.getCurrentUserId().equals(targetUserId);

        /* Handle Session Invalidation */
        if (!isAdmin && isSelfEdit) {
            session.invalidate();
            return org.springframework.http.ResponseEntity.ok(
                    java.util.Map.of("message", "Profile updated. Please log in again.", "requiresLogin", true)
            );
        } else if (isAdmin && !isSelfEdit) {
            User targetUser = userService.getUser(targetUserId);
            for (Object principal : sessionRegistry.getAllPrincipals()) {
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    org.springframework.security.core.userdetails.UserDetails userDetails = (org.springframework.security.core.userdetails.UserDetails) principal;
                    if (userDetails.getUsername().equals(targetUser.getEmail())) {
                        for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                            info.expireNow();
                        }
                    }
                } else if (principal instanceof String) {
                    if (principal.equals(targetUser.getEmail())) {
                        for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                            info.expireNow();
                        }
                    }
                }
            }
        }

        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Secured("ADMIN")
    @PostMapping("/user/role/delete/{user_id}/{role_id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteRolefromUser(
            @PathVariable Integer user_id,
            @PathVariable Integer role_id
    ) {
        User user = userService.getUser(user_id);
        userService.assertNotAdmin(user);

        Role role = roleRepository.findById(role_id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + role_id));

        user.getRoles().remove(role);
        userService.updateUser(user);

        return org.springframework.http.ResponseEntity.ok().build();
    }

    @Secured("ADMIN")
    @PostMapping("/user/role/add/{user_id}/{role_id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> addRoletoUser(
            @PathVariable Integer user_id,
            @PathVariable Integer role_id
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
                    return org.springframework.http.ResponseEntity.ok().build();
                }
                return org.springframework.http.ResponseEntity.badRequest().body(
                        java.util.Map.of("message", "OWNER_PROFILE_REQUIRED")
                );

            case "TENANT":
                if (user.getTenant() != null) {
                    assignRole(user, "TENANT");
                    return org.springframework.http.ResponseEntity.ok().build();
                }
                return org.springframework.http.ResponseEntity.badRequest().body(
                        java.util.Map.of("message", "TENANT_PROFILE_REQUIRED")
                );

            case "USER":
                assignRole(user, "USER");
                return org.springframework.http.ResponseEntity.ok().build();

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

    @Secured("ADMIN")
    @PostMapping("/user/delete/{user_id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteUser(@PathVariable Integer user_id) {

        userApplicationService.deleteUserAsAdmin(user_id);

        return org.springframework.http.ResponseEntity.ok().build();
    }



    /* Allows users to delete their own account */
    @Secured("USER")
    @PostMapping("/user/delete/self")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteOwnAccount(
            @Valid @ModelAttribute("deletionRequest") AccountDeletionRequest request,
            BindingResult bindingResult,
            HttpSession session
    ) {
        if (bindingResult.hasErrors()) {
            return org.springframework.http.ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Please fill out all required fields.")
            );
        }

        userApplicationService.deleteCurrentUserAccount(request);
        session.invalidate(); // force logout

        return org.springframework.http.ResponseEntity.ok().build();
    }

}
