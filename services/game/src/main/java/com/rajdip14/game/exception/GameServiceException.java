package com.rajdip14.game.exception;

public class GameServiceException extends RuntimeException {
    public GameServiceException(String message, Exception e) {
        super(message);
    }
}