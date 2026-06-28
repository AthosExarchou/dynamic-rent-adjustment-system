package gr.hua.dit.dras.controllers.auth;

/* imports */

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    public AuthController() {
    }

    @GetMapping("/login")
    public ResponseEntity<?> login() {
        return ResponseEntity.status(401)
                .body(java.util.Map.of(
                        "error", "Unauthorized", "message", "Please authenticate via /api/auth/login")
                );
    }
}
