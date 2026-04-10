package com.rajdip14.leaderboard_aggregator.serivce.impl;

import com.rajdip14.leaderboard_aggregator.model.LeaderboardEntry;
import com.rajdip14.leaderboard_aggregator.model.PlayerProfile;
import com.rajdip14.leaderboard_aggregator.serivce.AggregatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorServiceImpl implements AggregatorService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String LEADERBOARD_KEY = "leaderboard:global";
    private static final String CACHE_KEY = "leaderboard:latest";

    @Scheduled(fixedRate = 500)
    public void aggregateLeaderboard() {
        try {
            log.info("Starting leaderboard aggregation...");

        } catch (Exception e) {
            log.error("Error during leaderboard aggregation", e);
        }
    }
}
