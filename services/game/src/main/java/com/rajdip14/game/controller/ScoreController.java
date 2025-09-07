package com.rajdip14.game.controller;

import com.rajdip14.game.model.PlayerScore;
import com.rajdip14.game.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

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
        PlayerScore score
    ) {
        scoreService.publishScore(playerId, score);
    }
}