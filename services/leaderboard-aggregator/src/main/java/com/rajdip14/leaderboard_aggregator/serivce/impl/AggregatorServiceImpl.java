package com.rajdip14.leaderboard_aggregator.serivce.impl;

import com.rajdip14.leaderboard_aggregator.model.LeaderboardEntry;
import com.rajdip14.leaderboard_aggregator.model.PlayerProfile;
import com.rajdip14.leaderboard_aggregator.repository.RedisRepository;
import com.rajdip14.leaderboard_aggregator.serivce.AggregatorService;
import com.rajdip14.leaderboard_aggregator.serivce.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

import static com.rajdip14.leaderboard_aggregator.utils.AppConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorServiceImpl implements AggregatorService {

    private final JsonMapper jsonMapper;
    private final ProfileService profileService;
    private final RedisRepository redisRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;


    /**
     * Aggregates the leaderboard data and updates the cache if changes are detected.
     *
     * Execution flow:
     * 1. Checks the dirty flag in Redis — exits immediately if no score changes have occurred.
     * 2. Clears the dirty flag BEFORE reading the sorted set to avoid missing in-flight events.
     * 3. Fetches the top LEADERBOARD_SIZE players from the Redis sorted set.
     * 4. Batch fetches player profiles from cache, falling back to DB on cache miss.
     * 5. Builds ranked LeaderboardEntry list merging scores and profile data.
     * 6. Computes an MD5 hash of the new snapshot and compares with the last pushed hash.
     * 7. Skips cache write if the leaderboard is unchanged (delta detection).
     * 8. Stores the new snapshot and hash in Redis if a change is detected.
     * 9. Publishes a Kafka event to notify other services of the leaderboard update.
     *
     * @throws Exception if serialization or Redis operations fail (caught and logged internally)
     */
    @Scheduled(fixedDelay = 500)
    public void aggregateLeaderboard() {
        try {

            if (!redisRepository.isDirty()) {
                log.debug("No score changes detected, skipping aggregation...");
                return;
            }
            redisRepository.clearDirtyFlag();

            log.info("score changes detected, starting leaderboard aggregation...");

            Set<ZSetOperations.TypedTuple<String>> topPlayers = redisRepository.getTopPlayers(LEADERBOARD_SIZE);
            if (topPlayers == null || topPlayers.isEmpty()) {
                log.info("No players found in leaderboard");
                return;
            }

            List<String> playerIds = topPlayers.stream()
                    .map(ZSetOperations.TypedTuple::getValue)
                    .toList();

            // Fetch profiles (cache + fallback)
            Map<String, PlayerProfile> profileMap = profileService.getProfiles(playerIds);

            // Build response
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

            String newJson = jsonMapper.writeValueAsString(result);
            String newHash = DigestUtils.md5Hex(newJson);
            String lastHash = redisRepository.getLastLeaderboardHash();

            if (newHash.equals(lastHash)) {
                log.debug("Leaderboard unchanged after aggregation, skipping cache update");
                return;
            }

            redisRepository.cacheLeaderboardSnapshot(newJson);
            redisRepository.saveLastLeaderboardHash(newHash);
            log.info("Leaderboard cache updated successfully");

            kafkaTemplate.send(LEADERBOARD_CHANGE_TOPIC, "updated");
            log.info("Leaderboard change event published to topic: {}", LEADERBOARD_CHANGE_TOPIC);

        } catch (Exception e) {
            log.error("Error during leaderboard aggregation", e);
        }
    }
}