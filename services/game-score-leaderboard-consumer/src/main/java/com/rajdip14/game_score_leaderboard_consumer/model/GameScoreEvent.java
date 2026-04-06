package com.rajdip14.game_score_leaderboard_consumer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GameScoreEvent (
    String playerId,
    UUID eventId,
    Integer score,        // delta
    Long timestamp
) {}