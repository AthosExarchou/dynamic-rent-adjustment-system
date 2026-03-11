package gr.hua.dit.dras.controllers;

/* imports */
import gr.hua.dit.dras.dto.ContactForm;
import gr.hua.dit.dras.services.domain.ListingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import gr.hua.dit.dras.services.infrastructure.EmailService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("listings", listingService.getListings()); // featured listings
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
        /* Only adds a new form if one didn't survive a validation error via Flash Attributes */
        if (!model.containsAttribute("contactForm")) {
            model.addAttribute("contactForm", new ContactForm());
        }
        return "contact/contactus";
    }

    @PostMapping("/contact/send")
    public String sendContactMessage(
            @Valid @ModelAttribute("contactForm") ContactForm contactForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.contactForm", bindingResult);
            redirectAttributes.addFlashAttribute("contactForm", contactForm);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please check your inputs.");

            return "redirect:/contact/contactus";
        }

        try {
            emailService.sendContactUsEmail(contactForm);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Your message has been sent successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to send message. Please try again.");
            /* Passes the form back so the user doesn't lose what they typed upon failure */
            redirectAttributes.addFlashAttribute("contactForm", contactForm);
        }

        return "redirect:/contact/contactus";
    }

}
