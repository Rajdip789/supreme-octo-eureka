package com.rajdip14.leaderboard_service.service;

import com.rajdip14.leaderboard_service.dto.PlayerProfile;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface LeaderboardService {

    Mono<PlayerProfile> getPlayer(String playerId);
    Flux<ServerSentEvent<String>> streamLeaderboard(int count);
    Mono<List<PlayerProfile>> getSurroundingPlayers(String playerId, int count);

}