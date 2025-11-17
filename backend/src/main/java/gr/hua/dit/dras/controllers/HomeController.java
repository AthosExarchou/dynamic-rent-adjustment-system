package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.ContactForm;
import gr.hua.dit.dras.services.ListingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import gr.hua.dit.dras.services.EmailService;

@Controller
@RequestMapping("/")
public class HomeController {

    private final EmailService emailService;
    private final ListingService listingService;

    public HomeController(EmailService emailService, ListingService listingService) {
        this.emailService = emailService;
        this.listingService = listingService;
    }

    /* Home page */
    @GetMapping
    public String home(Model model) {
        model.addAttribute("title", "Home");

        /* featured listings */
        model.addAttribute("listings", listingService.getListings());
        return "index";
    }

    /* About Us page */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About Us");
        return "contact/about";
    }

    /* Privacy Policy page */
    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Privacy Policy");
        return "legal/privacy";
    }

    /* Terms of Service page */
    @GetMapping("/TermsOfService")
    public String TermsOfService(Model model) {
        model.addAttribute("title", "Terms of Service");
        return "legal/TermsOfService";
    }

    /* Contact Us page */
    @GetMapping("contact/contactus")
    public String contactUsPage(Model model) {

        model.addAttribute("contactForm", new ContactForm());
        return "contact/contactus";
    }

    @PostMapping("/contact/send")
    public String sendContactMessage(Model model, @ModelAttribute("contactForm") ContactForm contactForm) {
        try {
            emailService.sendContactUsEmail(contactForm);
            model.addAttribute("successMessage", "Your message has been sent successfully!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to send message. Please try again.");
            e.printStackTrace();
        }

        return "contact/contactus";
    }

}
