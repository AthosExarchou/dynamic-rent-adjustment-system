package gr.hua.dit.dras.controllers.auth;

/* imports */
import gr.hua.dit.dras.dto.ContactForm;
import gr.hua.dit.dras.dto.UserDTO;
import gr.hua.dit.dras.entities.PasswordResetToken;
import gr.hua.dit.dras.entities.User;
import gr.hua.dit.dras.repositories.PasswordResetTokenRepository;
import gr.hua.dit.dras.repositories.UserRepository;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class RestAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletRequest request) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));

            // Prevent session fixation: invalidate old session, create new one
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            SecurityContext sc = SecurityContextHolder.getContext();
            sc.setAuthentication(authentication);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);

            // Update lastLogin
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                return ResponseEntity.ok(new UserDTO(user));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "User not found after authentication"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return ResponseEntity.ok(new UserDTO(user));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        }

        String cleanEmail = email.trim();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No active account found with this email address."));
        }

        User user = userOpt.get();

        boolean isSystem = user.getOwner() != null && user.getOwner().isSystemOwner();
        if (isSystem) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No active account found with this email address."));
        }

        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));
        if (isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Password recovery is disabled for administrator accounts. Please use server console access."));
        }

        SecureRandom random = new SecureRandom();
        int codeNum = 100000 + random.nextInt(900000);
        String resetCode = String.valueOf(codeNum);

        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);
        PasswordResetToken resetToken;
        if (existingToken.isPresent()) {
            resetToken = existingToken.get();
            resetToken.setToken(resetCode);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        } else {
            resetToken = new PasswordResetToken(resetCode, user, LocalDateTime.now().plusMinutes(15));
        }
        passwordResetTokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetCodeEmail(user.getEmail(), user, resetCode);
            return ResponseEntity.ok(Map.of(
                    "message", "A 6-digit confirmation code has been sent to your email address.",
                    "email", user.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send confirmation code. Please try again later."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String code = payload.get("code");
        String newPassword = payload.get("newPassword");
        String confirmPassword = payload.get("confirmPassword");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        }
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Confirmation code is required."));
        }
        if (newPassword == null || newPassword.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password and confirmation are required."));
        }

        String cleanEmail = email.trim();
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No active account found with this email address."));
        }

        User user = userOpt.get();

        boolean isSystem = user.getOwner() != null && user.getOwner().isSystemOwner();
        if (isSystem) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No active account found with this email address."));
        }

        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName()));
        if (isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Password recovery is disabled for administrator accounts. Please use server console access."));
        }

        String cleanCode = code.trim();

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByUser(user);
        if (tokenOpt.isEmpty() || !tokenOpt.get().getToken().equals(cleanCode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid confirmation code."));
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Confirmation code has expired. Please request a new one."));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password and confirmation do not match."));
        }

        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$")) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Password must contain 8+ characters, including uppercase, lowercase, a number, and a special character."));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        return ResponseEntity.ok(Map.of(
                "message", "Your password has been reset successfully. You can now sign in with your new password."
        ));
    }
}
