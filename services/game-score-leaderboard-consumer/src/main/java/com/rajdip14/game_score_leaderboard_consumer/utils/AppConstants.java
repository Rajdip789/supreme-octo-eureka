package com.rajdip14.game_score_leaderboard_consumer.utils;

public class AppConstants {

    private AppConstants() {
        /* This utility class should not be instantiated */
    }
    public static final String SCORE_TOPIC          = "game.score.events";
    public static final String DIRTY_FLAG_KEY       = "leaderboard:dirty";
    public static final String LEADERBOARD_KEY      = "leaderboard:scores";
    public static final String PROCESSED_SORTED_SET = "processed:events";
    public static final String LUA_SCRIPT_PATH      = "lua/update_score.lua";
}
