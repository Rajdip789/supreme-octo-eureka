package com.rajdip14.leaderboard_service.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Standardized error response returned to clients on exception.
 */
@Builder
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp
) {}
