package gr.hua.dit.dras.controllers;

/* imports */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class AppErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(AppErrorController.class);

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = 500;
        
        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
                Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

                if (statusCode >= 500) {
                    log.error("Server Error [{}]: Root cause:", statusCode, throwable);
                } else if (statusCode >= 400) {
                    String errorMessage = (throwable != null && throwable.getMessage() != null)
                            ? throwable.getMessage() : "N/A";
                    log.warn("Client Error [{}]: Attempted access to '{}'. Reason: {}",
                            statusCode, request.getRequestURI(), errorMessage);
                }
            } catch (NumberFormatException ex) {
                log.error("Invalid HTTP status code in /error dispatch: {}", status, ex);
            }
        }

        String message;
        switch (statusCode) {
            case 404:
                message = "Resource not found";
                break;
            case 403:
                message = "Access denied";
                break;
            case 401:
                message = "Unauthorized";
                break;
            default:
                message = "An unexpected error occurred";
                break;
        }

        return ResponseEntity.status(statusCode).body(Map.of(
            "error", message,
            "status", statusCode
        ));
    }
}
