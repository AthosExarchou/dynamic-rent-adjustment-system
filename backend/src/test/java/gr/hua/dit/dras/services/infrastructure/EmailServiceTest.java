package gr.hua.dit.dras.services.infrastructure;

import gr.hua.dit.dras.dto.ContactForm;
import gr.hua.dit.dras.services.domain.NotificationService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationService notificationService;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix("");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        emailService = new EmailService(mailSender, templateEngine, notificationService);
    }

    @Test
    @DisplayName("Should successfully render contact-us template and send email without SpEL errors")
    void sendContactUsEmail_rendersTemplateSuccessfully() {
        ContactForm form = new ContactForm();
        form.setName("Jane Smith");
        form.setEmail("jane.smith@example.com");
        form.setSubject("Question about listing");
        form.setMessage("I would like to know if pets are allowed.");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean result = emailService.sendContactUsEmail(form);

        assertThat(result).isTrue();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("Should simulate success without throwing exception when mail credentials are not configured")
    void sendContactUsEmail_unconfiguredCredentials_simulatesSuccess() {
        emailService.setMailCredentials("", "");
        ContactForm form = new ContactForm();
        form.setName("Jane Smith");
        form.setEmail("jane.smith@example.com");
        form.setSubject("Question about listing");
        form.setMessage("I would like to know if pets are allowed.");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean result = emailService.sendContactUsEmail(form);

        assertThat(result).isTrue();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should return false and log warning when mail sender throws exception")
    void sendContactUsEmail_mailException_returnsFalse() {
        emailService.setMailCredentials("dras.system@gmail.com", "secret");
        ContactForm form = new ContactForm();
        form.setName("Jane Smith");
        form.setEmail("jane.smith@example.com");
        form.setSubject("Question about listing");
        form.setMessage("I would like to know if pets are allowed.");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailAuthenticationException("Auth failed"))
                .when(mailSender).send(any(MimeMessage.class));

        boolean result = emailService.sendContactUsEmail(form);

        assertThat(result).isFalse();
    }
}
