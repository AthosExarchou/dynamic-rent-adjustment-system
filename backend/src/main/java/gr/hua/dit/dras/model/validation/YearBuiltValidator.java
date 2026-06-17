package gr.hua.dit.dras.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Year;

/**
 * Validates that a year-built value falls within [1800, currentYear + 5].
 */
public class YearBuiltValidator implements ConstraintValidator<ValidYearBuilt, Integer> {

    private static final int MIN_YEAR = 1800;
    private static final int FUTURE_OFFSET = 5;

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null is valid
        }

        int maxYear = Year.now().getValue() + FUTURE_OFFSET;

        if (value < MIN_YEAR || value > maxYear) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Year built must be between " + MIN_YEAR + " and " + maxYear
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
