package com.rajdip14.leaderboard_aggregator.repository;

import com.rajdip14.leaderboard_aggregator.model.PlayerProfile;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

public interface RedisRepository {
    Set<ZSetOperations.TypedTuple<String>> getTopPlayers(int limit);
    void cacheLeaderboardSnapshot(String data);
    List<String> getProfiles(List<String> playerIds);
    void saveProfiles(List<PlayerProfile> profiles);
}