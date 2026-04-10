package com.rajdip14.leaderboard_aggregator.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntry {
    private int rank;
    private String playerId;
    private long score;
    private String name;
}