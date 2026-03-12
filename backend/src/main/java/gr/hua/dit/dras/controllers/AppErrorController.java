package gr.hua.dit.dras.controllers;

/* imports */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;

@Controller
public class AppErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(AppErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            try {
                int statusCode = Integer.parseInt(status.toString());

                Throwable throwable =
                        (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

                if (statusCode >= 500) {
                    if (throwable != null) {
                        log.error("Server Error [{}]: Root cause:", statusCode, throwable);
                    } else {
                        log.error("Server Error [{}]: No exception trace available.", statusCode);
                    }
                } else if (statusCode >= 400) {
                    /* Log a single line for 403s, 404s, etc. */
                    String errorMessage = (throwable != null && throwable.getMessage() != null)
                            ? throwable.getMessage() : "N/A";

                    log.warn("Client Error [{}]: Attempted access to '{}'. Reason: {}",
                            statusCode, request.getRequestURI(), errorMessage);
                }

                switch (statusCode) {
                    case 404:
                        return "error/error-404";
                    case 403:
                        return "error/error-403";
                    case 500:
                        return "error/error-500";
                    default:
                        return "error/error";
                }
            } catch (NumberFormatException ex) {
                log.error("Invalid HTTP status code in /error dispatch: {}", status, ex);
            }
        }
        return "error/error";
    }

}
