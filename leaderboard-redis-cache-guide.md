# Leaderboard — Redis Cache Guide

A reference for all Redis keys, data structures, and their roles in the leaderboard system.

---

## Cache Overview

| Key                             | Type | TTL | Written By | Read By |
|---------------------------------|---|---|---|---|
| `leaderboard:scores`            | Sorted Set | None | GameScoreConsumer (Lua) | Aggregator, RedisRepository |
| `leaderboard:player:<playerId>` | String (JSON) | 600s | ProfileService | Aggregator, RedisRepository |
| `leaderboard:topk:cache`        | String (JSON) | None | Aggregator | LeaderboardChangeConsumer, Controller |
| `leaderboard:topk:hash`         | String | None | Aggregator | Aggregator |
| `leaderboard:dirty`             | String | None | GameScoreConsumer (Lua) | Aggregator |
| `processed:events`              | Sorted Set | Per-member (24h) | GameScoreConsumer (Lua) | GameScoreConsumer (Lua) |

---

## 1. Leaderboard Scores

```
Key:   leaderboard:scores
Type:  Sorted Set (ZSET)
TTL:   None — permanent source of truth
```

Stores every player's current score. The sorted set automatically keeps players ordered by score, highest first. This is the foundation everything else is built on.

```
MEMBER          SCORE
─────────────────────────────
"player-123"  →  9500.0
"player-456"  →  9200.0
"player-789"  →  8900.0
```

**Written by:** `GameScoreConsumer` via Lua script using `ZINCRBY` — increments the score atomically on every score event consumed from Kafka.

**Read by:**
- Aggregator — `ZREVRANGE` to fetch top K players by rank
- `RedisRepository` — `reverseRank` to find a player's position, `reverseRangeWithScores` to fetch surrounding players

---

## 2. Player Profile Cache

```
Key:   leaderboard:player:<playerId>     ← PROFILE_KEY_PREFIX + playerId
Type:  String (JSON)
TTL:   600 seconds (10 minutes)
```

Stores each player's profile data as a JSON string. The key is built at runtime by concatenating `PROFILE_KEY_PREFIX` with the `playerId`:

```java
public static final String PROFILE_KEY_PREFIX = "leaderboard:player:";

// runtime key construction
PROFILE_KEY_PREFIX + "player-123"  →  "leaderboard:player:player-123"
```

Each key holds one `PlayerProfile` record:

```
leaderboard:player:player-123  →  {"playerId":"player-123","name":"Alice","avatarUrl":"https://..."}
leaderboard:player:player-456  →  {"playerId":"player-456","name":"Bob","avatarUrl":"https://..."}
leaderboard:player:player-789  →  {"playerId":"player-789","name":"Charlie","avatarUrl":"https://..."}
```

**Written by:** `ProfileService` — on a DB cache miss, fetches from PostgreSQL and writes back to Redis.

**Read by:** Aggregator and `RedisRepository` — both use `MGET` to batch fetch all K profiles in a single Redis round trip, avoiding N+1 lookups.

---

## 3. Leaderboard Snapshot Cache

```
Key:   leaderboard:topk:cache
Type:  String (JSON)
TTL:   None — overwritten on every aggregation cycle that detects a change
```

Stores the fully aggregated top K leaderboard as a JSON array. Each entry is a `LeaderboardEntry` combining rank, score, and profile data — ready to serve directly to the browser via SSE with no further processing.

```json
[
  { "rank": 1, "playerId": "player-123", "name": "Alice", "avatarUrl": "https://...", "score": 9500 },
  { "rank": 2, "playerId": "player-456", "name": "Bob",   "avatarUrl": "https://...", "score": 9200 },
  { "rank": 3, "playerId": "player-789", "name": "Charlie","avatarUrl": "https://...", "score": 8900 }
]
```

**Written by:** Aggregator — only after the MD5 delta check confirms the leaderboard has actually changed.

**Read by:**
- `LeaderboardChangeConsumer` — reads on receiving a Kafka signal and broadcasts via SSE to all connected clients
- `LeaderboardController` — reads on new SSE client connect to emit the initial snapshot immediately

---

## 4. Leaderboard Snapshot Hash

```
Key:   leaderboard:topk:hash
Type:  String
TTL:   None
```

Stores the MD5 hash of the last written snapshot JSON. Used by the aggregator to detect whether the leaderboard has actually changed before writing to cache and publishing a Kafka signal.

```
leaderboard:snapshot:hash  →  "a3f5c2d1e8b7c9d4e2f1a6b3c8d5e7f2"
```

**Why MD5 and not direct comparison:**

At scale (top 1000, top 10000 players), comparing full JSON strings on every aggregation cycle is expensive — the string grows linearly with leaderboard size. MD5 always produces a fixed 32-character hex string regardless of leaderboard size, making comparison O(1).

```
Direct comparison:   top 100   →  ~3KB string comparison
                     top 1000  →  ~30KB string comparison
                     top 10000 →  ~300KB string comparison

MD5 comparison:      top 100   →  32 chars  ✅
                     top 1000  →  32 chars  ✅
                     top 10000 →  32 chars  ✅
```

**Written by:** Aggregator — stored alongside the snapshot after a change is confirmed.

**Read by:** Aggregator — compared with the newly computed MD5 on every cycle.

---

## 5. Dirty Flag

```
Key:   leaderboard:dirty
Type:  String
TTL:   None — aggregator is responsible for clearing it
```

A lightweight signal key. Its **existence** means the leaderboard has pending score changes that need aggregation. Its **absence** means nothing has changed.

```
Key exists   →  "1"   →  dirty, aggregator should run
Key missing  →          →  clean, aggregator skips (cheap EXIT in ~0.1ms)
```

**The ordering rule — clear before reading:**

The dirty flag must be cleared **before** reading the sorted set, not after. If cleared after, any score events arriving during aggregation silently lose their signal:

```
CORRECT — clear first:
  clearDirtyFlag()              ← t=0ms
  new score event arrives       ← t=1ms  sets dirty=1 again (safe)
  ZREVRANGE leaderboard:scores  ← t=2ms  includes the new score
  next cycle sees dirty=1       ← picks up the new event ✅

WRONG — clear after:
  ZREVRANGE leaderboard:scores  ← t=0ms
  new score event arrives       ← t=1ms  sets dirty=1
  clearDirtyFlag()              ← t=2ms  clears the flag set at t=1ms
  next cycle sees dirty=0       ← new event silently lost ❌
```

**Written by:** `GameScoreConsumer` Lua script — atomically set alongside `ZINCRBY` in the same script.

**Read by:** Aggregator — `EXISTS` check every 100ms poll cycle.

**Cleared by:** Aggregator — `DEL` before reading the sorted set.

---

## 6. Processed Events (Dedup)

```
Key:   processed:events
Type:  Sorted Set (ZSET)
TTL:   Per-member — members older than 24h are removed by ZREMRANGEBYSCORE
```

Tracks which Kafka score events have already been processed, preventing duplicate score increments caused by Kafka's at-least-once delivery guarantee.

The **score is the epoch timestamp** of when the event was processed, enabling time-based cleanup:

```
MEMBER          SCORE (epoch seconds)
──────────────────────────────────────────
"evt-uuid-1"  →  1714000000
"evt-uuid-2"  →  1714000100
"evt-uuid-3"  →  1714000200
```

**Why Sorted Set over plain Set:**

| Structure | Dedup | Cleanup old events | Memory |
|---|---|---|---|
| Plain Set | ✅ | ❌ no timestamp, grows forever | ✅ single key |
| String keys (NX+EX) | ✅ | ✅ per-key TTL | ❌ N key headers |
| Sorted Set | ✅ | ✅ by score range | ✅ single key |

Sorted Set is the only structure that satisfies all three requirements.

**Lua script flow on every score event:**

```lua
-- check if already processed
local exists = redis.call("ZSCORE", "processed:events", eventId)
if exists then return 0 end                                -- duplicate, skip

-- mark as processed with current timestamp
redis.call("ZADD", "processed:events", currentTimestamp, eventId)

-- remove events older than 24h
redis.call("ZREMRANGEBYSCORE", "processed:events", 0, currentTimestamp - 86400)

-- update score
redis.call("ZINCRBY", "leaderboard:scores", scoreDelta, playerId)

-- signal aggregator
redis.call("SET", "leaderboard:dirty", "1")

return 1
```

**Written by:** `GameScoreConsumer` Lua script — `ZADD` with current timestamp.

**Read by:** `GameScoreConsumer` Lua script — `ZSCORE` to check for duplicates.

**Cleaned by:** `GameScoreConsumer` Lua script — `ZREMRANGEBYSCORE` removes members older than 24h on every write.

---

## Full Data Flow

```
Score event arrives via Kafka
        ↓
GameScoreConsumer (Lua — atomic):
    ZSCORE  processed:events        ← duplicate check
    ZADD    processed:events        ← mark processed
    ZREMRANGEBYSCORE                ← cleanup old events
    ZINCRBY leaderboard:scores      ← update score
    SET     leaderboard:dirty       ← signal aggregator
        ↓
Aggregator (polls every 100ms):
    EXISTS  leaderboard:dirty       ← cheap check, exit if clean
    DEL     leaderboard:dirty       ← clear BEFORE reading
    ZREVRANGE leaderboard:scores    ← fetch top K
    MGET    leaderboard:player:*    ← batch fetch profiles
    GET     leaderboard:topk:hash   ← fetch last hash
    MD5 compare                     ← delta check
    SET     leaderboard:topk:cache  ← store new snapshot
    SET     leaderboard:topk:hash   ← store new hash
    Kafka.send(leaderboard.snapshot.updated)
        ↓
LeaderboardChangeConsumer:
    GET     leaderboard:topk:cache  ← read snapshot
    leaderboardSink.publish()       ← fan out to SSE clients
        ↓
Browser receives SSE update
```

---

## AppConstants Reference

```java
public class AppConstants {
    public static final int    LEADERBOARD_SIZE          = 100;
    public static final int    PROFILE_CACHE_TTL_SECONDS = 600;
    public static final String UNKNOWN_PROFILE           = "Unknown";

    // Redis keys
    public static final String PROCESSED_SORTED_SET      = "processed:events";
    public static final String LEADERBOARD_KEY           = "leaderboard:scores";
    public static final String PROFILE_KEY_PREFIX        = "leaderboard:player:";
    public static final String LEADERBOARD_CACHE_KEY     = "leaderboard:topk:cache";
    public static final String LEADERBOARD_HASH_KEY      = "leaderboard:topk:hash";
    public static final String DIRTY_FLAG_KEY            = "leaderboard:dirty";

    // Kafka topics
    public static final String SCORE_TOPIC               = "game.score.events";
    public static final String LEADERBOARD_CHANGE_TOPIC  = "leaderboard.snapshot.updated";

    // SSE
    public static final String LEADERBOARD_EVENT         = "leaderboard-update";
    public static final String LEADERBOARD_CHANGE_EVENT  = "updated";
    public static final long   MIN_BROADCAST_INTERVAL_MS = 100L;
}
```
