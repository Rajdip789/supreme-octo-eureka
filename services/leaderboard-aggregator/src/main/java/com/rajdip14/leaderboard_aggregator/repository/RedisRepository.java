package com.rajdip14.leaderboard_aggregator.repository;

import com.rajdip14.leaderboard_aggregator.model.PlayerProfile;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.List;
import java.util.Set;

public interface RedisRepository {

    boolean isDirty();
    void clearDirtyFlag();

    String getLastLeaderboardHash();
    void saveLastLeaderboardHash(String newHash);
    void cacheLeaderboardSnapshot(String data);

    Set<ZSetOperations.TypedTuple<String>> getTopPlayers(int limit);

    List<String> getProfiles(List<String> playerIds);
    void saveProfiles(List<PlayerProfile> profiles);

}