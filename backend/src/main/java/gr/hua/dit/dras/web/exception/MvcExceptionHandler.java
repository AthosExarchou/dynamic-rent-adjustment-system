package gr.hua.dit.dras.web.exception;

/* imports */
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

@ControllerAdvice(annotations = Controller.class)
public class MvcExceptionHandler {

    /**
     * Handles standard business logic violations (e.g. trying to delete a rented listing).
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public String handleBusinessExceptions(RuntimeException ex,
                                           HttpServletRequest request,
                                           RedirectAttributes redirectAttributes
    ) {
        /* Attaches the message so it survives the redirect */
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());

        /* Sends user back to the exact page they came from */
        return redirectToReferer(request);
    }

    /**
     * Handles unauthorized access attempts.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(HttpServletRequest request,
                                              RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "You do not have permission to perform this action.");

        return redirectToReferer(request);
    }

    /**
     * Handles HTTP errors like 404.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(ResponseStatusException ex,
                                                HttpServletRequest request,
                                                RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getReason());

        return redirectToReferer(request);
    }

    /**
     * Generic fallback for unexpected server errors.
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes
    ) {
        /* Logs the actual exception */
        ex.printStackTrace();

        redirectAttributes.addFlashAttribute("errorMessage",
                "An unexpected error occurred. Please try again later.");

        return redirectToReferer(request);
    }

    /**
     * Helper method to safely redirect back to the previous page.
     */
    private String redirectToReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");

        /* If the Referer is missing, fallback to a safe default page */
        if (referer != null && !referer.isBlank()) {
            try {
                URI uri = new URI(referer);

                /* Allows only same-host redirects */
                if (uri.getHost() == null || uri.getHost().equals(request.getServerName())) {
                    String path = uri.getPath();
                    String query = uri.getQuery();

                    return "redirect:" + path + (query != null ? "?" + query : "");
                }
            } catch (URISyntaxException ignored) {}
        }

        return "redirect:/listings";
    }

}
