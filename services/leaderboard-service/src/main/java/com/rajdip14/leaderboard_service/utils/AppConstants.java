package com.rajdip14.leaderboard_service.utils;

public class AppConstants {

    private AppConstants() {
        /* This utility class should not be instantiated */
    }

    public static final String UNKNOWN_PROFILE              = "Unknown";

    public static final String LEADERBOARD_EVENT            = "leaderboard-update";
    public static final String LEADERBOARD_CHANGE_TOPIC     = "leaderboard.snapshot.updated";

    public static final String LEADERBOARD_KEY              = "leaderboard:scores";
    public static final String PROFILE_KEY_PREFIX           = "leaderboard:player:";
    public static final String LEADERBOARD_CACHE_KEY        = "leaderboard:topk:cache";
}
