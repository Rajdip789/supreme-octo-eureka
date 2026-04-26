package com.rajdip14.game_score_leaderboard_consumer.utils;

public class AppConstants {
    public static final String PROCESSED_SORTED_SET = "processed:events";
    public static final String LEADERBOARD_KEY = "leaderboard:game:session";
    public static final String DIRTY_FLAG_KEY = "leaderboard:dirty";
    public static final String LUA_SCRIPT_PATH = "lua/update_score.lua";
}
