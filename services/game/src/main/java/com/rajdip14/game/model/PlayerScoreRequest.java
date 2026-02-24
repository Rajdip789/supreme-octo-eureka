package com.rajdip14.game.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlayerScoreRequest(
        @NotBlank(message = "playerId must not be blank")
        String playerId,

        @Min(value = 0, message = "score must be >= 0")
        Integer score,

        String gameId   // optional for future use
) {}