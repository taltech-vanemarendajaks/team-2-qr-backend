package ee.valiit.mystuffback.infrastructure;


import ee.valiit.mystuffback.infrastructure.error.ApiError;
import ee.valiit.mystuffback.infrastructure.exception.DataNotFoundException;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.infrastructure.exception.PrimaryKeyNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ApiError> handleForbiddenException(ForbiddenException exception) {
        log.warn("403 Forbidden: {}", exception.getMessage());
        ApiError apiError = new ApiError();
        apiError.setMessage(exception.getMessage());
        apiError.setErrorCode(exception.getErrorCode());
        return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler
    public ResponseEntity<ApiError> handleDataNotFoundException(DataNotFoundException exception) {
        log.warn("404 Not found: {}", exception.getMessage());
        ApiError apiError = new ApiError();
        apiError.setMessage(exception.getMessage());
        apiError.setErrorCode(exception.getErrorCode());
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler
    public ResponseEntity<ApiError> handlePrimaryKeyNotFoundException(PrimaryKeyNotFoundException exception) {
        log.warn("404 Key not found: {}", exception.getMessage());
        ApiError apiError = new ApiError();
        apiError.setMessage(exception.getMessage());
        apiError.setErrorCode(exception.getErrorCode());
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            org.springframework.http.HttpHeaders headers,
            HttpStatusCode status,
            org.springframework.web.context.request.WebRequest request) {

        FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);

        ApiError apiError = new ApiError();
        apiError.setMessage(firstError.getField() + ": " + firstError.getDefaultMessage());
        apiError.setErrorCode(777);

        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }


}
