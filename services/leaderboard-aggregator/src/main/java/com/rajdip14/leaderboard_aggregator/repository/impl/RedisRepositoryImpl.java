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

    /*
     * Returns top {limit} players with their scores from Redis sorted set.
     */
    @Override
    public Set<ZSetOperations.TypedTuple<String>> getTopPlayers(int limit) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);
    }

    /*
     * Caches the latest leaderboard data as a JSON string in Redis for quick retrieval.
     */
    @Override
    public void cacheLeaderboardSnapshot(String data) {
        redisTemplate.opsForValue().set(CACHE_KEY, data);
    }

    /*
     * 1. Prepare Redis keys from the list of player IDs.
     * 2. Uses multiGet for batch retrieval to minimize Redis calls.
     * 3. Returns a list of profile JSON strings corresponding to the player IDs.
     */
    @Override
    public List<String> getProfiles(List<String> playerIds) {

        List<String> keys = playerIds.stream()
                .map(id -> PROFILE_KEY_PREFIX + id)
                .toList();

        return redisTemplate.opsForValue().multiGet(keys);
    }

    /*
     * 1. Pre-size the Map to avoid internal resizing/re-hashing.
     * 2. Converts each PlayerProfile to a JSON string and prepares a map of Redis keys to values.
     * 3. Uses multiSet for batch saving to minimize Redis calls.
     */
    @Override
    public void saveProfiles(List<PlayerProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return;

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