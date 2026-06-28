package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.ContactForm;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import java.util.Map;

@RestController
@RequestMapping("/")
public class HomeController {

    private final EmailService emailService;

    public HomeController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/contact/send")
    public ResponseEntity<?> sendContactMessage(
            @Valid @RequestBody ContactForm contactForm,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed", 
                "details", bindingResult.getAllErrors()
            ));
        }

        try {
            boolean success = emailService.sendContactUsEmail(contactForm);
            if (success) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to send message. Please try again."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to send message. Please try again."
            ));
        }
    }
}
