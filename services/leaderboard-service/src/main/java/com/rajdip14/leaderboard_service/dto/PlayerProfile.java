package com.rajdip14.leaderboard_service.dto;

import lombok.Builder;

@Builder
public record PlayerProfile(
        int rank,
        long score,
        String name,
        String playerId,
        String avatarUrl
) {}