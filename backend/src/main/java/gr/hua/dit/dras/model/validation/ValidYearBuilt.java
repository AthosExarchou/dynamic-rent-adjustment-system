package gr.hua.dit.dras.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that a year-built value falls between 1800 and the current year + 5.
 * Null values are considered valid.
 */
@Documented
@Constraint(validatedBy = YearBuiltValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidYearBuilt {

    String message() default "Year built must be between 1800 and {currentYear + 5}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
