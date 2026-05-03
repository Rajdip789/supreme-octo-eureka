package com.rajdip14.leaderboard_aggregator.utils;

public class AppConstants {

    private AppConstants() {
        /* This utility class should not be instantiated */
    }

    public static final int    LEADERBOARD_SIZE             = 100;
    public static final int    PROFILE_CACHE_TTL_SECONDS    = 600;
    public static final String UNKNOWN_PROFILE              = "Unknown";

    public static final String LEADERBOARD_CHANGE_TOPIC = "leaderboard.snapshot.updated";

    public static final String DIRTY_FLAG_KEY               = "leaderboard:dirty";
    public static final String LEADERBOARD_CACHE_HASH_KEY   = "leaderboard:snapshot:hash";
    public static final String LEADERBOARD_KEY              = "leaderboard:scores";
    public static final String LEADERBOARD_CACHE_KEY        = "leaderboard:snapshot:latest";
    public static final String PROFILE_KEY_PREFIX           = "leaderboard:player:profile:";
}
