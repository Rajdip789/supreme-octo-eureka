package com.rajdip14.game.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle @Payload @Valid failures
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());

        assert ex.getBindingResult() != null;
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid"
                ));

        return new ErrorResponse(BAD_REQUEST, "Invalid request", errors);
    }

    // 2. Handles custom validation logic exceptions
    @MessageExceptionHandler(InvalidMessageException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleCustomValidationException(InvalidMessageException ex) {
        return new ErrorResponse(BAD_REQUEST, ex.getMessage(), null);
    }

    // 3. Handles custom business logic exceptions
    @MessageExceptionHandler(GameServiceException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleBusinessException(GameServiceException ex) {
        return new ErrorResponse(INTERNAL_SERVER_ERROR, ex.getMessage(), null);
    }
}