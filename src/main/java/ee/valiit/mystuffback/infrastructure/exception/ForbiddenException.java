package ee.valiit.mystuffback.infrastructure.exception;

import lombok.Getter;

@Getter
public class ForbiddenException extends RuntimeException {
    private final Integer errorCode;

    public ForbiddenException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
