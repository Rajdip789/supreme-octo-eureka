package com.rajdip14.leaderboard_aggregator.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerProfile {
    private String playerId;
    private String name;
}