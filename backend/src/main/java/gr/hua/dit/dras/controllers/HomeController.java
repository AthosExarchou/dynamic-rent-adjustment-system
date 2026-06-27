package gr.hua.dit.dras.controllers;

import gr.hua.dit.dras.dto.ContactForm;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
            @Valid @ModelAttribute ContactForm contactForm,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Validation failed", 
                "details", bindingResult.getAllErrors()
            ));
        }

        try {
            emailService.sendContactUsEmail(contactForm);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to send message. Please try again."
            ));
        }
    }
}
