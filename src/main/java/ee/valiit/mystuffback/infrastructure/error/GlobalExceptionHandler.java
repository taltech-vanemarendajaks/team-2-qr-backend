package ee.valiit.mystuffback.infrastructure.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ee.valiit.mystuffback.infrastructure.exception.TooManyRequestsException;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        String message = (firstError != null && firstError.getDefaultMessage() != null)
                ? firstError.getDefaultMessage()
                : Error.VALIDATION_ERROR.getMessage();

        ApiError apiError = new ApiError();
        apiError.setMessage(message);
        apiError.setErrorCode(Error.VALIDATION_ERROR.getErrorCode());
        return apiError;
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbidden(ForbiddenException ex) {
        ApiError apiError = new ApiError();
        apiError.setMessage(ex.getMessage());
        apiError.setErrorCode(ex.getErrorCode());
        return apiError;
    }

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
