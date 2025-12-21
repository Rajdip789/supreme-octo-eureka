package com.rajdip14.game.controller;

import com.rajdip14.game.exception.InvalidMessageException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.rajdip14.game.service.ScoreService;
import com.rajdip14.game.model.PlayerScoreRequest;

@Slf4j
@Controller
public class ScoreController {

    private final ScoreService scoreService;

    @Autowired
    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @MessageMapping("/players/{playerId}/scores")
    public void processScore(
        @DestinationVariable String playerId,
        @Payload @Valid PlayerScoreRequest scoreRequest
    ) {
        log.info("Processing score for player: {}, RequestBody: {}", playerId, scoreRequest);

        if(!playerId.equals(scoreRequest.playerId())) {
            log.error("Player ID in the path variable does not match with request body");
            throw new InvalidMessageException("Player ID mismatch between path variable and request body");
        }

        scoreService.publishScore(playerId, scoreRequest);
    }
}