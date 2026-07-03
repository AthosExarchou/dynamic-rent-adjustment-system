package gr.hua.dit.dras.services.infrastructure;

/* imports */
import gr.hua.dit.dras.dto.ContactForm;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import gr.hua.dit.dras.services.domain.NotificationService;
import org.springframework.context.annotation.Lazy;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final NotificationService notificationService;

    public EmailService(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            @Lazy NotificationService notificationService
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.notificationService = notificationService;
    }

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private void sendHtmlEmail(
            String to,
            String subject,
            String template,
            Context context,
            String emailType
    ) {
        try {
            log.debug("Attempting to send email [type={}] to [{}]", emailType, to);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = templateEngine.process(template, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            log.info("Email sent successfully [type={}] to [{}]", emailType, to);

            // Mirror email as an in-app notification
            try {
                String notifMessage;
                if (emailType.equals("welcome")) notifMessage = "Welcome to Dynamic Rent Adjustment System!";
                else if (emailType.equals("tenantApproval")) notifMessage = "Your listing application has been approved.";
                else if (emailType.equals("ownerCreated")) notifMessage = "Your new listing was successfully created and is now pending admin approval.";
                else if (emailType.equals("adminApproved")) notifMessage = "Great news! Your listing has been approved by the administrator and is now live.";
                else if (emailType.equals("adminRejected")) notifMessage = "Unfortunately, your listing has been rejected by the administrator.";
                else if (emailType.equals("ownerRejectedApplication")) notifMessage = "Your application has been rejected by the listing owner.";
                else if (emailType.equals("listingDeleted")) notifMessage = "Your listing has been successfully deleted.";
                else if (emailType.equals("listingRentedToSomeoneElse")) notifMessage = "A listing you applied for has been rented to someone else.";
                else if (emailType.equals("userDetailsChanged")) notifMessage = "Your account details have been updated successfully.";
                else if (emailType.equals("accountDeleted")) notifMessage = "Your account has been deleted. We hope to see you again.";
                else notifMessage = subject;

                notificationService.createNotificationByEmail(to, subject, notifMessage);
            } catch (Exception notifEx) {
                log.warn("Failed to create in-app notification [type={}] to [{}]: {}", emailType, to, notifEx.getMessage());
            }

        } catch (MailException | MessagingException e) {
            log.warn("Failed to send email [type={}] to [{}]: {}",
                    emailType, to, e.getMessage(), e);
        }
    }

    public void sendWelcomeEmail(String recipientEmail, User user) {

        Context context = new Context();
        context.setVariable("username", user.getUsername());

        sendHtmlEmail(
                recipientEmail,
                "Welcome to Our Platform!",
                "email/new-user-welcome.html",
                context,
                "welcome"
        );
    }

    public void sendEmailNotification(
            String to, String name, Listing listing, String emailType) {

        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("listing", listing);

        String subject;
        String template;

        switch (emailType) {
            case "tenantApproval":
                subject = "Your listing application has been approved";
                template = "email/application-approved.html";
                break;
            case "ownerCreated":
                subject = "Your listing has been submitted for approval";
                template = "email/listing-created.html";
                break;
            case "adminApproved":
                subject = "Your listing has been approved by the administrator";
                template = "email/listing-approved-admin.html";
                break;
            case "adminRejected":
                subject = "Your listing has been rejected by the administrator";
                template = "email/listing-rejected-admin.html";
                break;
            case "ownerRejectedApplication":
                subject = "Your application has been rejected by the listing owner";
                template = "email/application-rejected-owner.html";
                break;
            case "listingRentedToSomeoneElse":
                subject = "A listing you applied for has been rented";
                template = "email/listing-rented-to-someone-else.html";
                break;
            default:
                log.error("Unsupported email type requested: {}", emailType);
                throw new IllegalArgumentException("Unsupported email type: " + emailType);
        }

        sendHtmlEmail(to, subject, template, context, emailType);
    }

    public void sendUserDetailsChangedEmail(
            String to,
            String newUsername,
            String newEmail,
            String oldUsername,
            String oldEmail,
            boolean usernameChanged,
            boolean emailChanged
    ) {

        Context context = new Context();
        context.setVariable("newUsername", newUsername);
        context.setVariable("oldUsername", oldUsername);
        context.setVariable("newEmail", newEmail);
        context.setVariable("oldEmail", oldEmail);
        context.setVariable("usernameChanged", usernameChanged);
        context.setVariable("emailChanged", emailChanged);

        sendHtmlEmail(
                to,
                "Your account details have been updated",
                "email/user-details-edited.html",
                context,
                "userDetailsChanged"
        );
    }

    public void sendListingDeletionEmail(String to, Listing listing) {

        Context context = new Context();
        context.setVariable("listing", listing);
        context.setVariable("ownerName",
                listing.getOwner().getUser().getUsername());

        sendHtmlEmail(
                to,
                "Your Listing Has Been Deleted",
                "email/listing-deleted.html",
                context,
                "listingDeleted"
        );
    }

    public void sendAccountDeletionEmail(String recipientEmail, User user) {

        Context context = new Context();
        context.setVariable("username", user.getUsername());

        sendHtmlEmail(
                recipientEmail,
                "Your Account Has Been Deleted",
                "email/user-account-deleted.html",
                context,
                "accountDeleted"
        );
    }

    public boolean sendContactUsEmail(ContactForm contactForm) {

        String to = "realestate2025project@gmail.com";

        /* Prepare Thymeleaf context */
        Context context = new Context();
        context.setVariable("name", contactForm.getName());
        context.setVariable("email", contactForm.getEmail());
        context.setVariable("subject", contactForm.getSubject());
        context.setVariable("message", contactForm.getMessage());

        try {
            log.debug("Attempting to send email [type={}] to [{}]", "contactForm", to);

            /* Create MIME email */
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, true, "UTF-8");

            /* Process HTML template */
            String htmlContent = templateEngine.process(
                    "email/contact-us.html", context);

            helper.setTo(to);
            String sanitizedSubject = contactForm.getSubject() != null ?
                    contactForm.getSubject().replaceAll("[\\r\\n]", "") : "";
            helper.setSubject("Contact Form: " + sanitizedSubject);
            helper.setText(htmlContent, true); // HTML content
            if (contactForm.getEmail() != null && !contactForm.getEmail().trim().isEmpty()) {
                helper.setReplyTo(
                        contactForm.getEmail().replaceAll("[\\r\\n]", "")
                ); // reply directly to sender
            }

            mailSender.send(mimeMessage);

            log.info("Email sent successfully [type={}] to [{}]", "contactForm", to);
            return true;
        } catch (MessagingException | MailException e) {
            log.warn("Failed to send email [type={}] to [{}]: {}",
                    "contactForm", to, e.getMessage(), e);
            return false;
        }
    }

}
