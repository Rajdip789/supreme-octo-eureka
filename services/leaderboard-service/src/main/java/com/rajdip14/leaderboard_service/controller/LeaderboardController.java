package com.rajdip14.leaderboard_service.controller;

import com.rajdip14.leaderboard_service.dto.PlayerProfile;
import com.rajdip14.leaderboard_service.service.LeaderboardService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/leaderboard/players")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * Returns the rank and score of a specific player.
     *
     * @param playerId the unique identifier of the player
     * @return Mono wrapping the player rank and score response
     */
    @GetMapping("/{playerId}")
    public Mono<ResponseEntity<PlayerProfile>> getPlayer(
            @PathVariable String playerId) {
        return leaderboardService.getPlayer(playerId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Returns K players surrounding the given player in the leaderboard.
     * Fetches players ranked above and below the given player by score.
     *
     * @param playerId the unique identifier of the player
     * @param count    number of surrounding players to fetch on each side
     * @return Mono wrapping the list of surrounding players
     */
    @GetMapping("/{playerId}/{count}")
    public Mono<ResponseEntity<List<PlayerProfile>>> getSurroundingKPlayers(
            @PathVariable String playerId,
            @PathVariable int count) {
        return leaderboardService.getSurroundingPlayers(playerId, count)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    /**
     * Opens a persistent SSE connection for real-time leaderboard updates.
     * Immediately emits the current snapshot on connect, then streams
     * subsequent updates as they arrive via the leaderboard sink.
     *
     * @param count number of top players the client is interested in
     * @return Flux of ServerSentEvents streamed to the client
     */
    @GetMapping(value = "/top/{count}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamTopKPlayers(
            @PathVariable @Positive int count) {
        return leaderboardService.streamLeaderboard(count);
    }
}
