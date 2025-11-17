package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ProfileController {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    public ProfileController(UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {

        /* Get the currently authenticated user entity */
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User is not authenticated.");
        }

        User currentUser = userService.getUserByEmail(authentication.getName());
        boolean isAdmin = currentUser.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));

        model.addAttribute("user", currentUser);
        model.addAttribute("isAdmin", isAdmin);
        return "profile/profile";
    }

    /* Show change password form */
    @GetMapping("/user/change-password/{id}")
    @Secured("USER")
    public String showChangePasswordForm(@PathVariable Integer id, Model model) {

        model.addAttribute("userId", id);
        return "profile/change-password";
    }

    /* Process password change */
    @PostMapping("/user/change-password/{id}")
    @Secured("USER")
    public String changePassword(@PathVariable Integer id,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {

        User user = (User) userService.getUser(id);

        /* Check old password */
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            model.addAttribute("errorMessage", "Old password is incorrect.");
            model.addAttribute("user", user);
            model.addAttribute("userId", id);
            return "profile/change-password";
        }

        /* Confirm new password */
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            model.addAttribute("errorMessage", "New password cannot be the same as the old one.");
            model.addAttribute("user", user);
            model.addAttribute("userId", id);
            return "profile/change-password";
        }

        /* Check new password != old password */
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "New password and confirmation do not match.");
            model.addAttribute("user", user);
            model.addAttribute("userId", id);
            return "profile/change-password";
        }

        /* Save new password */
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateUser(user);

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN"));

        model.addAttribute("successMessage", "Password changed successfully!");
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", isAdmin);

        return "profile/profile";
    }

    /* Show edit profile form */
    @GetMapping("/user/edit/{id}")
    @Secured("USER")
    public String showEditProfileForm(@PathVariable Integer id, Model model) {

        User user = (User) userService.getUser(id);
        model.addAttribute("user", user);
        return "profile/edit-profile"; //new template
    }

    @PostMapping("/user/edit/{id}")
    @Secured("USER")
    public String updateProfile(@Valid @PathVariable Integer id, @Valid @ModelAttribute("user") User updatedUser,
                                BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            return "profile/edit-profile";
        }

        User user = (User) userService.getUser(id);
        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        userService.updateUser(user);

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ADMIN"));

        model.addAttribute("successMessage", "Profile updated successfully!");
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", isAdmin);

        return "profile/profile"; //back to profile page
    }

}
