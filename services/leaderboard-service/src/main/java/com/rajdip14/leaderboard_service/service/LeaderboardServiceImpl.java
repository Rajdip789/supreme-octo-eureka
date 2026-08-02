package com.rajdip14.leaderboard_service.service;

import com.rajdip14.leaderboard_service.config.LeaderboardSink;
import com.rajdip14.leaderboard_service.dto.PlayerProfile;
import com.rajdip14.leaderboard_service.exception.LeaderboardServiceException;
import com.rajdip14.leaderboard_service.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.rajdip14.leaderboard_service.utils.AppConstants.LEADERBOARD_EVENT;

/**
 * Service responsible for building the SSE stream for leaderboard updates.
 * Combines an immediate snapshot on connect with a live update stream
 * from the reactive sink.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final RedisRepository redisRepository;
    private final LeaderboardSink leaderboardSink;

    public Mono<PlayerProfile> getPlayer(String playerId) {
        return Mono.empty(); // Placeholder for actual implementation
    }

    /**
     * Builds a Flux stream for a connecting SSE client.
     * Immediately emits the current leaderboard snapshot on connect
     * so the client does not see an empty screen while waiting for
     * the next Kafka signal. Subsequent updates are streamed live
     * from the leaderboard sink as they arrive.
     *
     * @param count number of top players the client requested
     * @return Flux of ServerSentEvents combining initial snapshot and live updates
     */
    @Override
    public Flux<ServerSentEvent<String>> streamLeaderboard(int count) {
        return Flux.concat(buildInitialSnapshot(), buildLiveUpdates(count))
                .doOnError(e -> log.error("Error in SSE stream for count: {}", count, e))
                .onErrorResume(LeaderboardServiceException.class, e -> {
                    log.error("Unrecoverable SSE stream error: {}", e.getMessage());
                    return Flux.error(e);
                })
                .onErrorResume(e -> {
                    log.error("Unexpected SSE stream error, terminating gracefully: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    /**
     * Fetches the current leaderboard snapshot from Redis and wraps it
     * in a single-element Flux for immediate emit on client connect.
     * Returns an empty Flux if no snapshot is available or Redis call fails.
     *
     * @return Flux emitting the current snapshot or empty if unavailable
     */
    private Flux<ServerSentEvent<String>> buildInitialSnapshot() {
        return redisRepository.getLeaderboardSnapshot()
                .doOnError(e -> log.error("Failed to fetch initial snapshot from Redis", e))
                .onErrorResume(e -> Mono.empty())
                .filter(snapshot -> !snapshot.isBlank())
                .map(this::buildSseEvent)
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.debug("No initial leaderboard snapshot available in Redis")))
                .flux();
    }

    /**
     * Subscribes to the leaderboard sink and maps each snapshot
     * to a ServerSentEvent for live streaming to the SSE client.
     * Applies latest-only backpressure so slow clients always
     * receive the most recent snapshot on catch-up.
     *
     * @param count number of top players the client requested
     * @return Flux of live ServerSentEvents from the leaderboard sink
     */
    private Flux<ServerSentEvent<String>> buildLiveUpdates(int count) {
        return leaderboardSink.asFlux()
                .onBackpressureLatest()
                .map(this::buildSseEvent)
                .doOnError(e -> log.error("Error in live update stream: {}", e.getMessage(), e))
                .onErrorContinue((e, snapshot) ->
                        log.warn("Skipping failed snapshot, stream continues: {}", e.getMessage()))
                .doOnSubscribe(s -> log.info("SSE client connected, count requested: {}", count))
                .doOnCancel(() -> log.info("SSE client disconnected"));
    }

    /**
     * Builds a ServerSentEvent with the leaderboard event name
     * and the given snapshot as data.
     *
     * @param snapshot the serialized leaderboard JSON
     * @return a ServerSentEvent wrapping the snapshot
     */
    private ServerSentEvent<String> buildSseEvent(String snapshot) {
        return ServerSentEvent.<String>builder()
                .event(LEADERBOARD_EVENT)
                .data(snapshot)
                .build();
    }

    /**
     * Fetches K players surrounding the given player in the leaderboard.
     * Delegates to Redis repository and maps infrastructure errors
     * to LeaderboardServiceException for consistent error handling.
     *
     * @param playerId the unique identifier of the player
     * @param count    number of surrounding players to fetch on each side
     * @return Mono wrapping the list of surrounding players
     */
    @Override
    public Mono<List<PlayerProfile>> getSurroundingPlayers(String playerId, int count) {
        return redisRepository.getSurroundingPlayers(playerId, count)
                .doOnError(e -> log.error("Error fetching surrounding players for playerId: {}, count: {}",
                        playerId, count, e))
                .onErrorMap(e -> !(e instanceof LeaderboardServiceException),
                        e -> new LeaderboardServiceException("Failed to fetch surrounding players", e));
    }
}