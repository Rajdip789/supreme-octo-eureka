package com.rajdip14.leaderboard_service.exception;

/**
 * Exception thrown when leaderboard service operations fail
 * due to underlying infrastructure issues such as Redis or DB failures.
 */
public class LeaderboardServiceException extends RuntimeException {

    public LeaderboardServiceException(String message) {
        super(message);
    }

    public LeaderboardServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}