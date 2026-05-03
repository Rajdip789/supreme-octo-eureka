package com.rajdip14.leaderboard_aggregator.repository.impl;

import com.rajdip14.leaderboard_aggregator.model.PlayerProfile;
import com.rajdip14.leaderboard_aggregator.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;

import static com.rajdip14.leaderboard_aggregator.utils.AppConstants.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    private final JsonMapper jsonMapper;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Checks whether the leaderboard dirty flag is set in Redis,
     * indicating that new score events have been processed since the last aggregation.
     *
     * @return true if the dirty flag key exists, false otherwise
     */
    public boolean isDirty() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(DIRTY_FLAG_KEY));
    }

    /**
     * Clears the dirty flag from Redis after aggregation begins.
     * Must be called BEFORE reading the sorted set to avoid missing
     * score events that arrive during the aggregation window.
     */
    public void clearDirtyFlag() {
        redisTemplate.delete(DIRTY_FLAG_KEY);
    }

    /**
     * Retrieves the last stored MD5 hash of the leaderboard snapshot from Redis.
     * @return the MD5 hash string, or null if not set (indicating no previous snapshot)
     */
    @Override
    public String getLastLeaderboardHash() {
        return redisTemplate.opsForValue().get(LEADERBOARD_CACHE_HASH_KEY);
    }

    /**
     * Saves the new MD5 hash of the leaderboard snapshot to Redis for future change detection.
     * @param hash the MD5 hash string to store
     */
    @Override
    public void saveLastLeaderboardHash(String hash) {
        redisTemplate.opsForValue().set(LEADERBOARD_CACHE_HASH_KEY, hash);
    }

    /**
     * Caches the latest leaderboard data as a JSON string in Redis for quick retrieval.
     * @param data the JSON string representing the latest leaderboard snapshot
     */
    @Override
    public void cacheLeaderboardSnapshot(String data) {
        redisTemplate.opsForValue().set(LEADERBOARD_CACHE_KEY, data);
    }

    /**
     * Returns top {limit} players with their scores from Redis sorted set.
     * @param limit the maximum number of top players to retrieve
     * @return a set of TypedTuple containing player IDs and their scores, ordered by score
     */
    @Override
    public Set<ZSetOperations.TypedTuple<String>> getTopPlayers(int limit) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(LEADERBOARD_KEY, 0, (long) limit - 1);
    }

    /**
     * 1. Prepare Redis keys from the list of player IDs.
     * 2. Uses multiGet for batch retrieval to minimize Redis calls.
     * 3. Returns a list of profile JSON strings corresponding to the player IDs.
     *
     *  @param playerIds list of player IDs to fetch profiles for
     *  @return list of profile JSON strings, may contain nulls for missing profiles
     */
    @Override
    public List<String> getProfiles(List<String> playerIds) {

        List<String> keys = playerIds.stream()
                .map(id -> PROFILE_KEY_PREFIX + id)
                .toList();

        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * Saves a list of player profiles to Redis cache using a pipelined batch write.
     *
     * Each profile is serialized to JSON and stored with a TTL of PROFILE_CACHE_TTL_SECONDS
     * under the key PROFILE_KEY_PREFIX + playerId. All writes are batched in a single
     * Redis pipeline to minimize round-trips.
     *
     * @param profiles list of PlayerProfile objects to cache
     * @throws Exception if serialization of any profile fails (caught and logged internally)
     */
    @Override
    public void saveProfiles(List<PlayerProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return;

        //Pre-size the Map to avoid internal resizing/re-hashing. (removed since we are using pipelining and not a Map here)
        //Map<String, String> profileMap = new HashMap<>((int) (profiles.size() / 0.75) + 1);

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {

            for (PlayerProfile profile : profiles) {
                try {
                    String key = PROFILE_KEY_PREFIX + profile.getPlayerId();
                    String value = jsonMapper.writeValueAsString(profile);

                    connection.stringCommands().setEx(
                            key.getBytes(),
                            PROFILE_CACHE_TTL_SECONDS,
                            value.getBytes()
                    );

                } catch (Exception e) {
                    log.error("Error processing profile {}", profile.getPlayerId(), e);
                }
            }
            return null;
        });
    }
}