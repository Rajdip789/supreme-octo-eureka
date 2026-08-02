package com.rajdip14.game.utils;

public class AppConstants {

    private AppConstants() {
        /* This utility class should not be instantiated */
    }

    public static final String SCORE_TOPIC = "game.score.events";

    public static final String APP_PATH = "/game";
    public static final String SOCKET_PATH = "/ws-scores";
    public static final String BROKER_USER_PREFIX = "/user";
    public static final String BROKER_QUEUE_PREFIX = "/queue";
}
