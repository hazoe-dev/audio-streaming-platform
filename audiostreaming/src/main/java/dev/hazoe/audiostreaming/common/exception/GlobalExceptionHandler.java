package dev.hazoe.audiostreaming.common.exception;

import dev.hazoe.audiostreaming.common.response.ApiErrorResponse;
import dev.hazoe.audiostreaming.common.response.ValidationErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ================= 400 ================= */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        400,
                        "VALIDATION_FAILED",
                        ex.getMessage(),
                        Instant.now()
                ));
    }

    /* ================= 401 ================= */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        401,
                        "INVALID_CREDENTIALS",
                        ex.getMessage(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        401,
                        "UNAUTHORIZED",
                        ex.getMessage(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiErrorResponse> handleJwt(JwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        401,
                        "INVALID_TOKEN",
                        ex.getMessage(),
                        Instant.now()
                ));
    }

    /* ================= 404 ================= */

    @ExceptionHandler(AudioNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAudioNotFound(AudioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        404,
                        "AUDIO_NOT_FOUND",
                        ex.getMessage(),
                        Instant.now()
                ));
    }

    /* ================= 409 ================= */

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                "EMAIL_ALREADY_EXISTS",
                ex.getMessage(),
                Instant.now()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /* ================= 416 ================= */

    @ExceptionHandler(RangeNotSatisfiableException.class)
    public ResponseEntity<Void> handleRangeNotSatisfiable(
            RangeNotSatisfiableException ex
    ) {
        HttpHeaders headers = new HttpHeaders();

        // RFC 7233 requires this
        headers.set(
                HttpHeaders.CONTENT_RANGE,
                "bytes */" + ex.getFileSize()
        );

        return ResponseEntity
                .status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .headers(headers)
                .build();// ⬅ NO BODY
    }

    /* ================= 500 ================= */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        500,
                        "INTERNAL_ERROR",
                        "Unexpected error",
                        Instant.now()
                ));
    }

    @ExceptionHandler(AudioStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleStorage(AudioStorageException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        500,
                        "AUDIO_STORAGE_ERROR",
                        ex.getMessage(),
                        Instant.now()
                ));
    }

}
