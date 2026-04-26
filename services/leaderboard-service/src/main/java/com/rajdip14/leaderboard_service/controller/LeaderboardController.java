package com.rajdip14.leaderboard_service.controller;

import com.rajdip14.leaderboard_service.dto.PlayerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard/players")
public class LeaderboardController {

    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerResponse> getPlayer(
            @PathVariable("playerId") String playerId
    ) {
        return ResponseEntity.ok(new PlayerResponse(playerId, "Player " + playerId, 1000));
    }

    @GetMapping("/{playerId}/{count}")
    public ResponseEntity<List<PlayerResponse>> getSurroundingKPlayers(
            @PathVariable("playerId") String playerId,
            @PathVariable("count") int count
    ) {
        return ResponseEntity.ok(
                    List.of(new PlayerResponse(playerId, "Player " + playerId, 1000)
                ));
    }
}
