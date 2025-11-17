package gr.hua.dit.dras.services;

/* imports */
import gr.hua.dit.dras.dto.ContactForm;
import gr.hua.dit.dras.entities.Listing;
import gr.hua.dit.dras.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    public void sendEmailNotification(String to, String name, Listing listing, String emailType) {

        try {
            System.out.println("Sending email to: " + to);

            /* email creation */
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            /* content preparation */
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
                default:
                    throw new IllegalArgumentException("Unsupported email type: " + emailType);
            }

            String htmlContent = templateEngine.process(template, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); //HTML content

            mailSender.send(mimeMessage);

            System.out.println("Email sent successfully to: " + to);

        } catch (MailException | MessagingException e) {
            System.err.println("Failed to send email to: " + to);
            e.printStackTrace();
        }
    }

    public void sendUserDetailsChangedEmail(String to, String newUsername, String newEmail,
                                            String oldUsername, String oldEmail,
                                            boolean usernameChanged, boolean emailChanged) {

        try {
            System.out.println("Sending email to: " + to);

            /* MIME email creation */
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            /* content preparation */
            Context context = new Context();
            context.setVariable("newUsername", newUsername);
            context.setVariable("oldUsername", oldUsername);
            context.setVariable("newEmail", newEmail);
            context.setVariable("oldEmail", oldEmail);
            context.setVariable("usernameChanged", usernameChanged);
            context.setVariable("emailChanged", emailChanged);

            String htmlContent = templateEngine.process("email/user-details-edited.html", context);

            helper.setTo(to);
            helper.setSubject("Your account details have been updated");
            helper.setText(htmlContent, true); //HTML content

            mailSender.send(mimeMessage);

            System.out.println("Email sent to: " + to);
        } catch (MailException | MessagingException e) {
            System.err.println("Failed to send email to: " + to);
            e.printStackTrace();
        }
    }

    public void sendListingDeletionEmail(String to, Listing listing) {

        try {
            System.out.println("Sending email to: " + to);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            /* content preparation */
            Context context = new Context();
            context.setVariable("listing", listing);
            context.setVariable("ownerName", listing.getOwner().getUser().getUsername());

            String htmlContent = templateEngine.process("email/listing-deleted.html", context);

            helper.setTo(to);
            helper.setSubject("Your Listing Has Been Deleted");
            helper.setText(htmlContent, true); //HTML content

            mailSender.send(mimeMessage);

            System.out.println("Email sent to: " + to);

        } catch (MailException | MessagingException e) {
            System.err.println("Failed to send email to: " + to);
            e.printStackTrace();
        }
    }

    public void sendAccountDeletionEmail(String recipientEmail, User user) {

        try {
            System.out.println("Sending email to: " + recipientEmail);
            String subject = "Your Account Has Been Deleted";

            /* content preparation */
            Context context = new Context();
            context.setVariable("username", user.getUsername());
            String body = templateEngine.process("email/user-account-deleted.html", context);

            /* MIME email creation */
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true); //HTML content

            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            System.err.println("Failed to send email to: " + recipientEmail);
            e.printStackTrace();
        }
    }

    public void sendWelcomeEmail(String recipientEmail, User user) {

        try {
            System.out.println("Sending welcome email to: " + recipientEmail);
            String subject = "Welcome to Our Platform!";

            /* content preparation */
            Context context = new Context();
            context.setVariable("username", user.getUsername());

            String body = templateEngine.process("email/new-user-welcome.html", context);

            /* MIME email creation */
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(body, true); //HTML content

            mailSender.send(message);

            System.out.println("Email sent to: " + recipientEmail);
        } catch (MailException | MessagingException e) {
            System.err.println("Failed to send welcome email to: " + recipientEmail);
            e.printStackTrace();
        }
    }

    public void sendContactUsEmail(ContactForm contactForm) {
        try {
            String to = "realestate2025project@gmail.com";
            String subject = "Contact Form: " + contactForm.getSubject();

            /* Prepare Thymeleaf context */
            Context context = new Context();
            context.setVariable("name", contactForm.getName());
            context.setVariable("email", contactForm.getEmail());
            context.setVariable("subject", contactForm.getSubject());
            context.setVariable("message", contactForm.getMessage());

            /* Process HTML template */
            String htmlContent = templateEngine.process("email/contact-us.html", context);

            /* Create and send MIME email */
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); //HTML content

            /* reply directly to sender */
            helper.setReplyTo(contactForm.getEmail());
            System.out.println("Sending contact-us email to: " + to);
            mailSender.send(mimeMessage);

            System.out.println("Contact-us email sent to: " + to);
        } catch (MessagingException | MailException e) {
            System.err.println("Failed to send contact-us email.");
            e.printStackTrace();
        }
    }

}
