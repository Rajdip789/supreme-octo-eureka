package com.rajdip14.game_score_leaderboard_consumer.kafka;

import com.rajdip14.game_score_leaderboard_consumer.model.GameScoreEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.rajdip14.game_score_leaderboard_consumer.utils.AppConstants.LEADERBOARD_KEY;
import static com.rajdip14.game_score_leaderboard_consumer.utils.AppConstants.PROCESSED_SET;

@Slf4j
@Service
public class GameScoreConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> updateScoreScript;

    public GameScoreConsumer(RedisTemplate<String, String> redisTemplate, DefaultRedisScript<Long> updateScoreScript) {
        this.redisTemplate = redisTemplate;
        this.updateScoreScript = updateScoreScript;
    }

    @KafkaListener(topics = "game-scores")
    public void consumeScore(GameScoreEvent event) {
        try {
            log.info("Received score event: {}", event.toString());

            List<String> keys = List.of(
                    PROCESSED_SET,
                    LEADERBOARD_KEY
            );

            List<String> args = List.of(
                    event.eventId().toString(),
                    event.playerId(),
                    event.score().toString()
            );

            Long result = redisTemplate.execute(updateScoreScript, keys, args.toArray());

//            Long result = redisTemplate.execute(
//                    updateScoreScript,
//                    keys,
//                    event.eventId().toString(),
//                    event.playerId(),
//                    String.valueOf(event.score())
//            );

            if (result != null && result == 1) {
                log.info("Score updated for player: {}", event.playerId());
            } else {
                log.info("Duplicate event ignored: {}", event.eventId());
            }
        } catch (Exception e) {
            log.error("Error processing score event: {}", e.getMessage(), e);
            return;
        }
    }
}
