package com.rajdip14.game.service;

import com.rajdip14.game.model.PlayerScore;
import com.rajdip14.game.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import static org.springframework.kafka.support.KafkaHeaders.TOPIC;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScoreService {

    private final KafkaTemplate<String, PlayerScore> kafkaTemplate;

    public void publishScore(String playerId, PlayerScore score) {

        log.info("Sending player score event: {}", score.toString());
        score.setPlayerId(playerId);
        score.setTimestamp(System.currentTimeMillis());

        Message<PlayerScore> message = MessageBuilder
                .withPayload(score)
                .setHeader(TOPIC, AppConstants.SCORE_TOPIC)
                .build();

        kafkaTemplate.send(message);

        log.info("Player score event sent to topic 'game-scores'");
    }
}

