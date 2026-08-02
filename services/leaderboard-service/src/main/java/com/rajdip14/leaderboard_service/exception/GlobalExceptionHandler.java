package com.rajdip14.leaderboard_service.exception;

import com.rajdip14.leaderboard_service.dto.ErrorResponse;
import io.lettuce.core.RedisCommandTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Global exception handler for the leaderboard service.
 * Intercepts exceptions thrown across all controllers and maps
 * them to standardized HTTP error responses.
 *
 * Uses ServerWebExchange instead of HttpServletRequest
 * since this is a WebFlux application running on Netty.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles LeaderboardServiceException thrown when Redis or
     * downstream service operations fail.
     *
     * @param ex       the exception thrown
     * @param exchange the current server web exchange
     * @return Mono wrapping a 500 INTERNAL SERVER ERROR response
     */
    @ExceptionHandler(LeaderboardServiceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleLeaderboardServiceException(
            LeaderboardServiceException ex,
            ServerWebExchange exchange) {

        log.error("LeaderboardServiceException at {}: {}",
                exchange.getRequest().getPath(), ex.getMessage(), ex);

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Leaderboard service error",
                        ex.getMessage(),
                        exchange)));
    }

    /**
     * Handles IllegalArgumentException thrown when invalid
     * path variables or request parameters are provided.
     *
     * @param ex       the exception thrown
     * @param exchange the current server web exchange
     * @return Mono wrapping a 400 BAD REQUEST response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            ServerWebExchange exchange) {

        log.warn("IllegalArgumentException at {}: {}",
                exchange.getRequest().getPath(), ex.getMessage());

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Invalid request",
                        ex.getMessage(),
                        exchange)));
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRedisConnectionFailure(
            RedisConnectionFailureException ex,
            ServerWebExchange exchange) {

        log.error("Redis connection failure at {}: {}",
                exchange.getRequest().getPath(), ex.getMessage());

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildErrorResponse(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Cache unavailable",
                        "Leaderboard service is temporarily unavailable",
                        exchange)));
    }

    @ExceptionHandler(RedisCommandTimeoutException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRedisTimeout(
            RedisCommandTimeoutException ex,
            ServerWebExchange exchange) {

        log.warn("Redis timeout at {}: {}",
                exchange.getRequest().getPath(), ex.getMessage());

        return Mono.just(ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(buildErrorResponse(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Cache timeout",
                        "Request timed out, please try again",
                        exchange)));
    }

    /**
     * Handles all unhandled exceptions as a fallback.
     *
     * @param ex       the exception thrown
     * @param exchange the current server web exchange
     * @return Mono wrapping a 500 INTERNAL SERVER ERROR response
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex,
            ServerWebExchange exchange) {

        log.error("Unhandled exception at {}: {}",
                exchange.getRequest().getPath(), ex.getMessage(), ex);

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal server error",
                        "An unexpected error occurred",
                        exchange)));
    }

    /**
     * Builds a standardized ErrorResponse from the given details.
     *
     * @param status   HTTP status of the error
     * @param error    short error description
     * @param message  detailed error message
     * @param exchange the current server web exchange
     * @return a populated ErrorResponse record
     */
    private ErrorResponse buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            ServerWebExchange exchange) {

        return ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(exchange.getRequest().getPath().toString())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
