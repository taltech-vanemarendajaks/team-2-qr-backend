package ee.valiit.mystuffback.infrastructure.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ee.valiit.mystuffback.infrastructure.exception.TooManyRequestsException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(Error.VALIDATION_ERROR.getMessage());

        ApiError apiError = new ApiError();
        apiError.setMessage(message);
        apiError.setErrorCode(Error.VALIDATION_ERROR.getErrorCode());
        return apiError;
    }
    @ExceptionHandler(TooManyRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiError handleTooManyRequests(TooManyRequestsException ignored) {
        ApiError apiError = new ApiError();
        apiError.setMessage(Error.RATE_LIMITED.getMessage());
        apiError.setErrorCode(Error.RATE_LIMITED.getErrorCode());
        return apiError;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ApiError apiError = new ApiError();
        apiError.setMessage("Internal server error");
        apiError.setErrorCode(500);
        return apiError;
    }

}
