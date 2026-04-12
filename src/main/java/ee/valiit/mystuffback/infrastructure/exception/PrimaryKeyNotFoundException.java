package ee.valiit.mystuffback.infrastructure.exception;

import lombok.Getter;

@Getter
public class PrimaryKeyNotFoundException extends RuntimeException {
    private final Integer errorCode;

    public PrimaryKeyNotFoundException(String fieldName, Integer fieldValue) {
        super("Couldn't find primary key '" + fieldName + "' with value: " + fieldValue);
        this.errorCode = 777;
    }
}
