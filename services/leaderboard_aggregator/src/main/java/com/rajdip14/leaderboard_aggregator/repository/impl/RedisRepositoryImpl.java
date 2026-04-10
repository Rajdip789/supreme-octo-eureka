package com.rajdip14.leaderboard_aggregator.repository.impl;

import com.rajdip14.leaderboard_aggregator.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisRepositoryImpl implements RedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

     /*
      * Returns top {limit} players with their scores from Redis sorted set.
     */
    @Override
    public Set<ZSetOperations.TypedTuple<String>> getTopPlayers(int limit) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores("leaderboard:global", 0, limit - 1);
    }

    /*
     * Caches the latest leaderboard data as a JSON string in Redis for quick retrieval.
     */
    @Override
    public void saveLeaderboardCache(String data) {
        redisTemplate.opsForValue().set("leaderboard:latest", data);
    }
}