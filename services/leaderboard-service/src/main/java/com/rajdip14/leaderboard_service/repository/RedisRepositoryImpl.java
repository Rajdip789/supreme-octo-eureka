package com.rajdip14.leaderboard_service.repository;

import com.rajdip14.leaderboard_service.dto.PlayerProfile;
import io.lettuce.core.RedisCommandTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rajdip14.leaderboard_service.utils.AppConstants.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final JsonMapper jsonMapper;

    @Override
    public Mono<String> getLeaderboardSnapshot() {
        return withErrorHandling(
                reactiveRedisTemplate.opsForValue()
                        .get(LEADERBOARD_CACHE_KEY),
                "getLeaderboardSnapshot"
        );
    }

    @Override
    public Mono<List<PlayerProfile>> getSurroundingPlayers(@NonNull String playerId, int count) {
        return withErrorHandling(
                fetchPlayerRank(playerId)
                        .flatMap(rank -> fetchSurroundingEntries(rank, count))
                        .flatMap(this::enrichWithProfiles)
                        .switchIfEmpty(Mono.fromRunnable(() ->
                                log.debug("Player not found in leaderboard: {}", playerId))),
                "getSurroundingPlayers[" + playerId + "]"
        );
    }

    /**
     * Fetches the 0-based rank of the given player in the leaderboard sorted set.
     * Returns empty Mono if player is not found.
     *
     * @param playerId the unique identifier of the player
     * @return Mono wrapping the 0-based rank of the player
     */
    private Mono<Long> fetchPlayerRank(String playerId) {
        return reactiveRedisTemplate.opsForZSet()
                .reverseRank(LEADERBOARD_KEY, playerId);
    }

    /**
     * Fetches players surrounding the given rank with their scores.
     * Calculates start and end boundaries and returns a list of RankedTuples.
     *
     * @param rank  0-based rank of the target player
     * @param count number of surrounding players to fetch on each side
     * @return Mono wrapping list of RankedTuples (rank + playerId + score)
     */
    private Mono<List<RankedTuple>> fetchSurroundingEntries(long rank, int count) {
        long start = Math.max(0, rank - count);
        long end = rank + count;

        return reactiveRedisTemplate.opsForZSet()
                .reverseRangeWithScores(LEADERBOARD_KEY, Range.closed(start, end))
                .index()
                .map(indexed -> new RankedTuple(
                        (int) (start + indexed.getT1() + 1),
                        indexed.getT2().getValue(),
                        indexed.getT2().getScore() != null
                                ? indexed.getT2().getScore().longValue() : 0L
                ))
                .collectList();
    }

    /**
     * Extracts player IDs from the list of RankedTuples
     * Using the player IDs fetches their profiles in batch,
     * Falls back to UNKNOWN_PROFILE if a profile is not found in cache.
     *
     * @param entries list of RankedTuples to enrich
     * @return Mono wrapping the fully built list of PlayerProfile
     */
    private Mono<List<PlayerProfile>> enrichWithProfiles(List<RankedTuple> entries) {
        List<String> playerIds = entries.stream()
                .map(RankedTuple::playerId)
                .toList();

        return fetchProfiles(playerIds)
                .map(profileMap -> buildLeaderboardEntries(entries, profileMap));
    }

    /**
     * Batch fetches player profiles from Redis profile cache for all given playerIds.
     * Returns a map of playerId to PlayerProfile for O(1) lookup during entry building.
     * Missing profiles are silently skipped and handled downstream with UNKNOWN_PROFILE.
     *
     * @param playerIds list of playerIds to fetch profiles for
     * @return Mono wrapping map of playerId to PlayerProfile
     */
    private Mono<Map<String, PlayerProfile>> fetchProfiles(List<String> playerIds) {
        List<String> keys = playerIds.stream()
                .map(id -> PROFILE_KEY_PREFIX + id)
                .toList();

        return reactiveRedisTemplate.opsForValue()
                .multiGet(keys)
                .map(values -> buildProfileMap(playerIds, values));
    }

    /**
     * Builds a map of playerId to PlayerProfile from multiGet results.
     * Skips entries where JSON deserialization fails.
     *
     * @param playerIds list of playerIds in same order as values
     * @param values    list of JSON strings from Redis multiGet
     * @return map of playerId to PlayerProfile
     */
    private Map<String, PlayerProfile> buildProfileMap(List<String> playerIds, List<String> values) {
        Map<String, PlayerProfile> profileMap = new HashMap<>();

        for (int i = 0; i < playerIds.size(); i++) {
            String json = values.get(i);
            if (json == null) continue;
            try {
                profileMap.put(playerIds.get(i),
                        jsonMapper.readValue(json, PlayerProfile.class));
            } catch (Exception e) {
                log.warn("Failed to deserialize profile for playerId: {}", playerIds.get(i));
            }
        }

        return profileMap;
    }

    private List<PlayerProfile> buildLeaderboardEntries(
            List<RankedTuple> entries,
            Map<String, PlayerProfile> profileMap) {

        return entries.stream()
                .map(entry -> {
                    PlayerProfile profile = profileMap.get(entry.playerId());
                    return PlayerProfile.builder()
                            .rank(entry.rank())
                            .playerId(entry.playerId())
                            .score(entry.score())
                            .name(profile != null ? profile.name() : UNKNOWN_PROFILE)
                            .avatarUrl(profile != null ? profile.avatarUrl() : null)
                            .build();
                })
                .toList();
    }

    /**
     * Applies standard Redis error handling to any Mono.
     * Logs specific Redis errors with operation context and re-throws
     * for the service layer to map to LeaderboardServiceException,
     * and for the global handler to map to appropriate HTTP responses.
     *
     * @param mono      the Mono to apply error handling to
     * @param operation description of the operation for logging context
     * @return Mono with standard Redis error handling applied
     */
    private <T> Mono<T> withErrorHandling(Mono<T> mono, String operation) {
        return mono
                .doOnError(RedisConnectionFailureException.class,
                        e -> log.error("Redis connection failed during {}: {}", operation, e.getMessage()))
                .doOnError(RedisCommandTimeoutException.class,
                        e -> log.warn("Redis timeout during {}: {}", operation, e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Unexpected error during {}: {}", operation, e.getMessage(), e);
                    return Mono.error(e);
                });
    }

    private record RankedTuple(int rank, String playerId, long score) {}
}