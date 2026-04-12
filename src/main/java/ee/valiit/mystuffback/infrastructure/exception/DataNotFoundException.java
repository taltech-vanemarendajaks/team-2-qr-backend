package ee.valiit.mystuffback.infrastructure.exception;

import lombok.Getter;

@Getter
public class DataNotFoundException extends RuntimeException {
    private final Integer errorCode;

    public DataNotFoundException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
