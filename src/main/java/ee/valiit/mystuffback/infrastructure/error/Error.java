package ee.valiit.mystuffback.infrastructure.error;

import lombok.Getter;

@Getter
public enum Error {
    INCORRECT_CREDENTIALS("Incorrect email or password", 111),
    ITEM_NAME_UNAVAILABLE("This item name already exists", 333),
    VALIDATION_ERROR("Invalid request", 400),
    EMAIL_UNAVAILABLE("This email already exists", 223),
    RATE_LIMITED("Too many attempts. Please try again later.", 4291);

    private final String message;
    private final Integer errorCode;

    Error(String message, Integer errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }
}
