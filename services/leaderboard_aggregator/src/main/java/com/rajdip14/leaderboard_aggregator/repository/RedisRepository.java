package com.rajdip14.leaderboard_aggregator.repository;

import org.springframework.data.redis.core.ZSetOperations;
import java.util.Set;

public interface RedisRepository {
    Set<ZSetOperations.TypedTuple<String>> getTopPlayers(int limit);
    void saveLeaderboardCache(String data);
}