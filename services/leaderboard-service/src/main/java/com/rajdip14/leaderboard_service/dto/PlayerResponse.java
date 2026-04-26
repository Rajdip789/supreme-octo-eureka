package com.rajdip14.leaderboard_service.dto;


public record PlayerResponse(
        String playerId,
        String name,
        int score
) {
}