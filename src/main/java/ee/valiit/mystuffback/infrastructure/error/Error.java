package ee.valiit.mystuffback.infrastructure.error;

import lombok.Getter;

@Getter
public enum Error {
    INCORRECT_CREDENTIALS("Incorrect username or password", 111),
    USERNAME_UNAVAILABLE("This username already exists", 222),
    ITEM_NAME_UNAVAILABLE("This item name already exists", 333),
    VALIDATION_ERROR("Invalid request", 400);

    private final String message;
    private final Integer errorCode;

    Error(String message, Integer errorCode) {
        this.message = message;
        this.errorCode = errorCode;
    }
}
