package com.rajdip14.game.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScoreEvent {
    private String playerId;
    private int score;
    private int totalScore;
    private Long timestamp;
}
