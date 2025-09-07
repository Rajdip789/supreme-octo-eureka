package com.rajdip14.game.model;

import lombok.Data;

@Data
public class PlayerScore {
    private String playerId;
    private int score;
    private int total_score;
    private long timestamp;
}
