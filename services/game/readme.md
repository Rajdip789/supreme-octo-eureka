# Apache Kafka Docker Setup
## Prerequisites

- Docker
- Docker Compose

Ensure Docker is installed and running on your machine.

---

## Setup

Start Kafka and ZooKeeper containers using Docker Compose:

```bash
$ docker compose up -d
```

```bash
$ docker exec -it broker-1 kafka-topics --bootstrap-server broker-1:29092 --create --topic test-topic --partitions 3 --replication-factor 3
```
> Created topic test-topic.

```bash
$ docker exec -it broker-1 kafka-topics --bootstrap-server broker-1:29092 --describe --topic test-topic
```
> Topic: test-topic       TopicId: 250LhXABSbqbpVQq62Oh-w PartitionCount: 3       ReplicationFactor: 3    Configs: min.insync.replicas=2 \
> Topic: test-topic       Partition: 0    Leader: 6       Replicas: 6,4,5 Isr: 6,4,5 \
> Topic: test-topic       Partition: 1    Leader: 4       Replicas: 4,5,6 Isr: 4,5,6 \
> Topic: test-topic       Partition: 2    Leader: 5       Replicas: 5,6,4 Isr: 5,6,4


```bash
$ docker exec -it broker-1 kafka-console-producer --bootstrap-server broker-1:29092 --topic test-topic
> Hello kafka
> kafka working
> ctrl+D
```

```bash
$ docker exec -it broker-2 kafka-console-consumer --bootstrap-server broker-2:29092 --topic test-topic --from-beginning
```
> Hello kafka
> kafka working