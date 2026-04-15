package ee.valiit.mystuffback.infrastructure.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final Integer errorCode;

    public BadRequestException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
