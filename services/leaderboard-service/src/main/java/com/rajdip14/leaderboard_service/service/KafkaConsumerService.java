package com.rajdip14.leaderboard_service.service;

import com.rajdip14.leaderboard_service.config.LeaderboardSink;
import com.rajdip14.leaderboard_service.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static com.rajdip14.leaderboard_service.utils.AppConstants.LEADERBOARD_CHANGE_TOPIC;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final RedisRepository redisRepository;
    private final LeaderboardSink leaderboardSink;

    /**
     * Consumes leaderboard update signals from Kafka.
     * Reads the latest snapshot from Redis and publishes to the reactive sink
     * which fans out to all connected SSE clients.
     *
     * @param signal lightweight trigger value, content is irrelevant
     */
    @KafkaListener(topics = LEADERBOARD_CHANGE_TOPIC)
    public void onLeaderboardUpdated(String signal) {
        redisRepository.getLeaderboardSnapshot()
                .onErrorResume(e -> {
                    log.error("Unexpected error fetching snapshot from Redis: {}", e.getMessage(), e);
                    return Mono.empty();
                })
                .filter(snapshot -> !snapshot.isBlank())
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.info("Snapshot not found in Redis, skipping broadcast")))
                .subscribe(snapshot -> {
                    leaderboardSink.publish(snapshot);
                    log.info("Leaderboard snapshot published to sink");
                });
    }
}
