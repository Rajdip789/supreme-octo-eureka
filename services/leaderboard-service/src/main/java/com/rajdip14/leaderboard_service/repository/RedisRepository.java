package com.rajdip14.leaderboard_service.repository;

import com.rajdip14.leaderboard_service.dto.PlayerProfile;
import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;

public interface RedisRepository {
    Mono<String> getLeaderboardSnapshot();
    Mono<List<PlayerProfile>> getSurroundingPlayers(@NonNull String playerId, int count);
}
