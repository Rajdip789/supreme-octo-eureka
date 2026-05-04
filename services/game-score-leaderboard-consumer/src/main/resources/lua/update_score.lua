-- KEYS[1] = processed events sorted set (timestamp as score)
-- KEYS[2] = leaderboard sorted set
-- KEYS[3] = dirty flag key
-- ARGV[1] = eventId
-- ARGV[2] = playerId
-- ARGV[3] = score delta
-- ARGV[4] = current timestamp (epoch seconds, passed from Java)

-- check if eventId already exists in processed events
local exists = redis.call("ZSCORE", KEYS[1], ARGV[1])

-- non-nil means event already processed, skip and return 0
if exists then
    return 0
end

-- 1. mark event as processed with current timestamp as score
-- 2. remove events older than 12h to prevent memory leak (current timestamp - 43200 seconds)
-- 3. set expiration to 25 hours to ensure old events are cleaned up
-- 4. increment leaderboard score
-- 5. set dirty flag with no TTL (as in case of a crash, the next aggregation will just sess it again and process and cleans up)
redis.call("ZADD", KEYS[1], ARGV[4], ARGV[1])
redis.call("ZREMRANGEBYSCORE", KEYS[1], 0, ARGV[4] - 43200)
redis.call("EXPIRE", KEYS[1], 90000)
redis.call("ZINCRBY", KEYS[2], ARGV[3], ARGV[2])
redis.call("SET", KEYS[3], "1")

return 1
