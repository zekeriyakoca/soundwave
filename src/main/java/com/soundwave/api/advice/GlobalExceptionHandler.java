package com.soundwave.api.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        var errors = ex.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        problem.setProperty("errors", errors);
        log.atWarn()
                .addKeyValue("event", "request.validation_failed")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.BAD_REQUEST.value())
                .addKeyValue("errorCount", errors.size())
                .log("Request validation failed");
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.atWarn()
                .setCause(ex)
                .addKeyValue("event", "request.body_unreadable")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.BAD_REQUEST.value())
                .log("Request body could not be parsed");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleBusinessRule(IllegalStateException ex, HttpServletRequest request) {
        log.atWarn()
                .setCause(ex)
                .addKeyValue("event", "request.business_rule_failed")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.CONFLICT.value())
                .addKeyValue("errorMessage", ex.getMessage())
                .log("Business rule rejected the request");
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Operation not allowed in current state");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        log.atWarn()
                .setCause(ex)
                .addKeyValue("event", "request.bad_input")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.BAD_REQUEST.value())
                .addKeyValue("errorMessage", ex.getMessage())
                .log("Request contained invalid input");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid input provided");
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ProblemDetail handleInvalidDataAccessApiUsage(InvalidDataAccessApiUsageException ex, HttpServletRequest request) {
        log.atWarn()
                .setCause(ex)
                .addKeyValue("event", "request.invalid_query")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.BAD_REQUEST.value())
                .addKeyValue("errorMessage", ex.getMessage())
                .log("Invalid query parameter (e.g. unknown sort property)");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid query parameter");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.atWarn()
                .setCause(ex)
                .addKeyValue("event", "request.data_integrity_failed")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.CONFLICT.value())
                .log("Data integrity violation occurred");
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data integrity violation");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.atWarn()
                .setCause(ex)
                .addKeyValue("event", "request.concurrent_update_conflict")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.CONFLICT.value())
                .addKeyValue("entity", ex.getPersistentClassName())
                .log("Concurrent update conflict detected");
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Concurrent update detected");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.atError()
                .setCause(ex)
                .addKeyValue("event", "request.unexpected_error")
                .addKeyValue("path", request.getRequestURI())
                .addKeyValue("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .log("Unexpected error while handling request");
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
}
