package com.rajdip14.leaderboard_aggregator.serivce.impl;

import com.rajdip14.leaderboard_aggregator.model.LeaderboardEntry;
import com.rajdip14.leaderboard_aggregator.model.PlayerProfile;
import com.rajdip14.leaderboard_aggregator.repository.RedisRepository;
import com.rajdip14.leaderboard_aggregator.serivce.AggregatorService;
import com.rajdip14.leaderboard_aggregator.serivce.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

import static com.rajdip14.leaderboard_aggregator.utils.AppConstants.LEADERBOARD_SIZE;
import static com.rajdip14.leaderboard_aggregator.utils.AppConstants.UNKNOWN_PROFILE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorServiceImpl implements AggregatorService {

    private final JsonMapper jsonMapper;
    private final ProfileService profileService;
    private final RedisRepository redisRepository;

    @Scheduled(fixedDelay = 500)
    public void aggregateLeaderboard() {
        try {
            log.info("Starting leaderboard aggregation...");

            // 1️⃣ Fetch top players
            Set<ZSetOperations.TypedTuple<String>> topPlayers = redisRepository.getTopPlayers(LEADERBOARD_SIZE);

            if (topPlayers == null || topPlayers.isEmpty()) {
                log.info("No players found in leaderboard");
                return;
            }

            // 2️⃣ Extract player IDs
            List<String> playerIds = topPlayers.stream()
                    .map(ZSetOperations.TypedTuple::getValue)
                    .toList();

            // 3️⃣ Fetch profiles (cache + fallback)
            Map<String, PlayerProfile> profileMap = profileService.getProfiles(playerIds);

            // 4️⃣ Build response
            List<LeaderboardEntry> result = new ArrayList<>();

            int rank = 1;
            for (var entry : topPlayers) {
                String playerId = entry.getValue();
                Double score = entry.getScore();

                PlayerProfile profile = profileMap.get(playerId);

                result.add(LeaderboardEntry.builder()
                        .rank(rank++)
                        .playerId(playerId)
                        .score(score != null ? score.longValue() : 0)
                        .name(profile != null ? profile.getName() : UNKNOWN_PROFILE)
                        .build());
            }

            // 5️⃣ Store aggregated leaderboard
            String json = jsonMapper.writeValueAsString(result);
            redisRepository.cacheLeaderboardSnapshot(json);

            log.info("Leaderboard cache updated successfully");

        } catch (Exception e) {
            log.error("Error during leaderboard aggregation", e);
        }
    }
}