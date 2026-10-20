package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.ContactForm;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private HomeController homeController;

    @Test
    @DisplayName("Should successfully send contact message when form is valid")
    void sendContactMessage_success() {
        ContactForm form = new ContactForm();
        form.setName("John Doe");
        form.setEmail("john@example.com");
        form.setSubject("General Inquiry");
        form.setMessage("Hello DRAS team");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(emailService.sendContactUsEmail(any(ContactForm.class))).thenReturn(true);

        ResponseEntity<?> response = homeController.sendContactMessage(form, bindingResult);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(emailService).sendContactUsEmail(form);
    }

    @Test
    @DisplayName("Should return bad request when binding result has validation errors")
    void sendContactMessage_validationErrors() {
        ContactForm form = new ContactForm();

        when(bindingResult.hasErrors()).thenReturn(true);

        ResponseEntity<?> response = homeController.sendContactMessage(form, bindingResult);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(emailService, never()).sendContactUsEmail(any());
    }

    @Test
    @DisplayName("Should return 500 when email service fails to send")
    void sendContactMessage_failure() {
        ContactForm form = new ContactForm();
        form.setEmail("john@example.com");
        form.setSubject("Inquiry");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(emailService.sendContactUsEmail(any(ContactForm.class))).thenReturn(false);

        ResponseEntity<?> response = homeController.sendContactMessage(form, bindingResult);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
