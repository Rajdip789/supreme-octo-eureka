
# 🎮 Real-Time Leaderboard Consumer Service

This service is responsible for consuming player score events from Kafka and updating a real-time leaderboard stored in Redis.

It is designed for **high throughput, low latency, and correctness** using an event-driven architecture.

---

## 🚀 Tech Stack

- Java 21
- Spring Boot
- Apache Kafka
- Redis
- Lua Scripting (for atomic operations)
- Docker (for Redis)

---

## 🧩 Architecture Overview
WebSocket → Game Service → Kafka (game-scores topic)
↓
Leaderboard Consumer (this service)
↓
Redis

---

## ⚙️ Responsibilities

- Consume score events from Kafka topic `game-scores`
- Perform **idempotent updates** to Redis leaderboard
- Maintain **real-time ranking** using Redis Sorted Sets
- Prevent duplicate score processing using event tracking

---

## 📥 Event Structure

```json
{
  "playerId": "player-123",
  "eventId": "uuid",
  "score": 10,
  "timestamp": 1710000000000
}
```

## 🧪 Sample Event for testing

You can manually produce a test event to Kafka using the following command:

```bash
echo '{"playerId":"test12345","eventId":"123e4578-e85c-12d3-a456-426614174002","score":20,"timestamp":1710000000000}' \
| docker exec -i leaderboard-kafka \
kafka-console-producer --broker-list localhost:9092 --topic game-scores
```


## 🧠 Core Design Concepts

### 1️⃣ Idempotent Processing

Since Kafka guarantees at-least-once delivery, duplicate events may occur.
To handle this:

- Each event contains a unique eventId
- Redis stores processed event IDs in a Set
- Duplicate events are ignored

### 2️⃣ Atomic Updates Using Lua

Redis Lua script ensures:
- Check if event already processed
  - If not:
  Add eventId to processed set
  Increment leaderboard score
- All operations happen atomically in a single execution.

### 3️⃣ Redis Data Structures

| Key | Type        | Purpose                         |
|----|------------|---------------------------------|
| leaderboard:scores   | Sorted Set | Stores player rankings          |
| processed:events | Set        | Tracks processed event IDs      |