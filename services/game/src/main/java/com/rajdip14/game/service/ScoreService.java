package com.rajdip14.game.service;

import com.rajdip14.game.exception.GameServiceException;
import com.rajdip14.game.model.PlayerScoreRequest;
import com.rajdip14.game.model.ScoreEvent;
import com.rajdip14.game.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import static org.springframework.kafka.support.KafkaHeaders.TOPIC;
import static org.springframework.kafka.support.KafkaHeaders.KEY;

@Slf4j
@RequiredArgsConstructor
@Service
public class ScoreService {

    private final KafkaTemplate<String, ScoreEvent> kafkaTemplate;

    public void publishScore(String playerId, PlayerScoreRequest scoreRequest) {
        try {

            ScoreEvent scoreEvent = ScoreEvent.builder()
                    .playerId(playerId)
                    .score(scoreRequest.score())
                    .totalScore(scoreRequest.total_score())
                    .timestamp(System.currentTimeMillis())
                    .build();

            log.info("Sending player score event: {}", scoreEvent.toString());

            Message<ScoreEvent> message = MessageBuilder
                    .withPayload(scoreEvent)
                    .setHeader(TOPIC, AppConstants.SCORE_TOPIC)
                    .setHeader(KEY, scoreEvent.getPlayerId())
                    .build();

            kafkaTemplate.send(message);
            log.info("Player score event sent to topic: {}", AppConstants.SCORE_TOPIC);

        } catch (Exception e) {
            log.error("Error while publishing player score event, playerId: {}, Message: {}", scoreRequest.playerId(), e.getMessage());
            throw new GameServiceException("Failed to publish player score event", e);
        }
    }
}
