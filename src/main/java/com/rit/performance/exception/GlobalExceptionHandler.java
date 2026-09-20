package com.rit.performance.exception;

import com.rit.performance.dto.ApiMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.rit.performance")
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiMessageResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiMessageResponse> handleAuthentication(AuthenticationException ex) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", ex.getMessage());
    }

    @ExceptionHandler(AccountUnavailableException.class)
    public ResponseEntity<ApiMessageResponse> handleAccountUnavailable(AccountUnavailableException ex) {
        return response(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiMessageResponse> handleNotFound(ResourceNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiMessageResponse> handleDuplicate(DuplicateResourceException ex) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", ex.getMessage());
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiMessageResponse> handleInvalid(InvalidOperationException ex) {
        return response(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiMessageResponse> handleFileStorage(FileStorageException ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_ERROR", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiMessageResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessageResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .distinct().collect(java.util.stream.Collectors.joining("; "));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiMessageResponse> handleMalformedRequest(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request value");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiMessageResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        String message = ex.getAllErrors().stream()
                .map(error -> String.valueOf(error.getDefaultMessage()))
                .distinct().collect(java.util.stream.Collectors.joining("; "));
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiMessageResponse.warning("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessageResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String message = "Unable to complete the request. Please try again.";
        if ("DELETE".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().matches(".*/milestones/[^/]+/positions/[^/]+$")) {
            message = "Unable to remove the position. Please try again.";
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                message);
    }

    private ResponseEntity<ApiMessageResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiMessageResponse.warning(code, message));
    }

    private ResponseEntity<ApiMessageResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiMessageResponse.error(code, message));
    }
}
