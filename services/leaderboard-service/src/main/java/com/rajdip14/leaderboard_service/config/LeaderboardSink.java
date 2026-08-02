package com.rajdip14.leaderboard_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Shared reactive sink that fans out leaderboard snapshots to all
 * connected SSE subscribers. Uses multicast with latest-only backpressure
 * to ensure slow clients receive the most recent snapshot on catch-up.
 */
@Slf4j
@Component
public class LeaderboardSink {

    private final Sinks.Many<String> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer(1, false);

    /**
     * Publishes a new leaderboard snapshot to all active subscribers.
     *
     * @param snapshot the serialized leaderboard JSON to broadcast
     */
    public void publish(String snapshot) {
        Sinks.EmitResult result = sink.tryEmitNext(snapshot);
        if (result.isFailure()) {
            log.warn("Failed to emit leaderboard snapshot, result: {}", result);
        }
    }

    /**
     * Returns the Flux that SSE clients subscribe to for live updates.
     *
     * @return Flux of leaderboard snapshot strings
     */
    public Flux<String> asFlux() {
        return sink.asFlux();
    }
}