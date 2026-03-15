/**
 * InputValidator.java
 * ─────────────────────────────────────────────────────────────────
 * Validates user input before making API calls.
 * Keeps all validation logic in one reusable place.
 * ─────────────────────────────────────────────────────────────────
 */
public class InputValidator {

    // City names may contain letters (any language), spaces, hyphens, and apostrophes.
    // Examples of valid names: "New York", "Dnepr", "Clermont-Ferrand", "Xi'an"
    private static final String VALID_CITY_PATTERN = "^[\\p{L}\\s\\-']+$";

    /**
     * Validates a city name entered by the user.
     *
     * @param cityName raw text from the search field
     * @return a {@link ValidationResult} carrying success/failure + message
     */
    public static ValidationResult validateCity(String cityName) {

        // ── 1. Null / empty check ──────────────────────────────
        if (cityName == null || cityName.trim().isEmpty()) {
            return ValidationResult.failure("City name cannot be empty.\nPlease enter a city name.");
        }

        String trimmed = cityName.trim();

        // ── 2. Length check ───────────────────────────────────
        if (trimmed.length() < 2) {
            return ValidationResult.failure("City name is too short.\nPlease enter at least 2 characters.");
        }

        if (trimmed.length() > 100) {
            return ValidationResult.failure("City name is too long.\nPlease enter a valid city name.");
        }

        // ── 3. Character check ────────────────────────────────
        if (!trimmed.matches(VALID_CITY_PATTERN)) {
            return ValidationResult.failure(
                "Invalid city name: \"" + trimmed + "\"\n" +
                "City names may only contain letters, spaces, hyphens, and apostrophes."
            );
        }

        // ── 4. All checks passed ──────────────────────────────
        return ValidationResult.success(trimmed);
    }

    // ─────────────────────────────────────────────────────────────
    //  Inner result class – avoids throwing exceptions for normal
    //  validation failures.
    // ─────────────────────────────────────────────────────────────
    public static class ValidationResult {

        private final boolean valid;
        private final String  message;   // error message when !valid
        private final String  value;     // trimmed input when valid

        private ValidationResult(boolean valid, String message, String value) {
            this.valid   = valid;
            this.message = message;
            this.value   = value;
        }

        /** Creates a successful result carrying the cleaned value. */
        public static ValidationResult success(String cleanedValue) {
            return new ValidationResult(true, null, cleanedValue);
        }

        /** Creates a failure result carrying a user-facing error message. */
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public boolean isValid()   { return valid;   }
        public String  getMessage(){ return message; }

        /**
         * Returns the trimmed / cleaned city name.
         * Only call this after checking {@link #isValid()}.
         */
        public String getValue()   { return value;   }
    }
}
