package gr.hua.dit.dras.controllers.user;

import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.services.domain.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class ProfileController {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    public ProfileController(UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    /* Process password change */
    @PostMapping("/user/change-password/{id}")
    @Secured("USER")
    public ResponseEntity<?> changePassword(
            @PathVariable Integer id,
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword
    ) {
        validateProfileOwnership(id);
        User user = userService.getUser(id);

        /* Check old password */
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Old password is incorrect."));
        }

        /* Confirm new password */
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password cannot be the same as the old one."));
        }

        /* Check new password != old password */
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password and confirmation do not match."));
        }

        /* Save new password */
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.updateUser(user);

        return ResponseEntity.ok().build();
    }

    /* Helper Methods */

    private void validateProfileOwnership(Integer requestedId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.getUserByEmail(authentication.getName());

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));

        if (!isAdmin && !currentUser.getId().equals(requestedId)) {
            throw new AccessDeniedException("You do not have permission to modify this profile.");
        }
    }
}
