-- KEYS[1] = processed events set
-- KEYS[2] = leaderboard sorted set
-- ARGV[1] = eventId
-- ARGV[2] = playerId
-- ARGV[3] = score delta

-- determines whether the member element is a member of the set key
local exists = redis.call("SISMEMBER", KEYS[1], ARGV[1])

-- 1 means event already processed, skip and return 0
if exists == 1 then
    return 0
end

-- mark event processed
redis.call("SADD", KEYS[1], ARGV[1])

-- increment leaderboard score
redis.call("ZINCRBY", KEYS[2], ARGV[3], ARGV[2])

return 1
