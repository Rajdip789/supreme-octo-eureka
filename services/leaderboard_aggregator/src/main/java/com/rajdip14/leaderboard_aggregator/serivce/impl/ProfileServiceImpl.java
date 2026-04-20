package com.rajdip14.leaderboard_aggregator.serivce.impl;

import com.rajdip14.leaderboard_aggregator.model.PlayerProfile;
import com.rajdip14.leaderboard_aggregator.repository.RedisRepository;
import com.rajdip14.leaderboard_aggregator.serivce.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ObjectMapper objectMapper;
    private final RedisRepository redisRepository;

    @Override
    public Map<String, PlayerProfile> getProfiles(List<String> playerIds) {

        Map<String, PlayerProfile> profileMap = new HashMap<>();

        // 1️⃣ Batch fetch from Redis
        List<String> cachedProfiles = redisRepository.getProfiles(playerIds);

        List<String> missingIds = new ArrayList<>();

        for (int i = 0; i < playerIds.size(); i++) {
            String playerId = playerIds.get(i);
            String profileJson = cachedProfiles != null ? cachedProfiles.get(i) : null;

            if (profileJson != null) {
                try {
                    PlayerProfile profile = objectMapper.readValue(profileJson, PlayerProfile.class);
                    profileMap.put(playerId, profile);
                } catch (Exception e) {
                    log.error("Error parsing profile for {}", playerId, e);
                }
            } else {
                missingIds.add(playerId);
            }
        }

        // 2️⃣ Fetch missing from DB (DUMMY for now)
        if (!missingIds.isEmpty()) {
            log.info("Cache miss for {} users, fetching from DB", missingIds.size());

            List<PlayerProfile> dbProfiles = fetchFromDB(missingIds);
            for (PlayerProfile profile : dbProfiles) {
                profileMap.put(profile.getPlayerId(), profile);
            }

            redisRepository.saveProfiles(dbProfiles);
        }

        return profileMap;
    }

    /**
     * Dummy DB fetch (replace with repository later)
     */
    private List<PlayerProfile> fetchFromDB(List<String> playerIds) {
        return playerIds.stream()
                .map(id -> PlayerProfile.builder()
                        .playerId(id)
                        .name("User_" + id)
                        .build())
                .toList();
    }
}
