package com.rajdip14.game.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ScoreEvent {
    private String playerId;
    private UUID eventId;
    private Integer score;
    private Long timestamp;
}