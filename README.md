# Kafka

Some notes taken while reading about Kafka


## Kafka basics

### Topics

Topics are one of the basic concepts on kafka. It basically represent a particular stream of data.
Is similar to a table in a DB. It's possible configure as many topics as you want.

Each topic is identified by a name.


### Partitions

Kafka topics are split into partitions. They are ordered (and numbered from 0 onwards).
Each partition can have a different number of messages on it.


### Offsets

Each message within a partition, gets an incremental ID, called offset (from 0 onwards as well).


### Gotchas about Topics, Partitions and Offsets

* Offset only have a meaning for a specific partition.
* Order is only guaranteed within the partition (not across partitions).
* Data in Kafka is only kept for limited time (default one week).
* Data can't change after it has been written.
* Data is assigned a random partition (unless a key is specified).


### Broker

A kafka cluster is composed of multiple brokers (servers), each one identified by an ID (number).
Each broker contains certain topic partitions.

Once connected to a broker, you have full access to the entire cluster. A 3 brokers cluster
is a good start point.

Kafka distributes the partitions of a topic across the different brokers randomly, but always
in a distributed way.


### Topic Replicator Factor

When creating a topic, it needs a replication factor, usually between 2 and 3 (being 2 a bit
risky and 3 nice).

I.e:

Topic-1 with 2 partitions and replication factor of 2 and 3 brokers:

```
Broker 1: Partition 0 (Leader)
Broker 2: Partition 1 (Leader) and Partition 0 (Repl.)
Broker 3: Partition 1 (Repl.)
```

This way if a broker dies, we still have al the partitions available.


### Leader for partition

* Only one broker can be the "Leader" for a partition.
* Only the leader can receive and server data for a partition.
* Other brokers will just sync the partition with the leader.
* Each partition has only one leader and multiple in-sync-replica (ISR).


### Producers

Producers write data to topics. They will automatically know to which broker and partition
to write to. If a broker fails, producers can recover automatically as well.

Producers will load balance to many brokers writing to different partitions (Following Round Robin).

Producers can choose to receive acknowledgment of data writes:

- acks=0: Won't wait for confirmation (possible data loss).
- acks=1: Will wait for leader confirmation (limited data loss).
- acks=all: Will wait for the leader and all replicas for confirmation (no data loss).


### Message Keys

Producers can choose sending a key with the message (can be whatever). 

If no key indicated, the message is write following Round Robin. If key is indicated, all
messages with that key will be written in the same partition. It's useful when you need
message ordering for a specific field (I.e: Same kind of events).


### Consumers

Consumers read data from a topic and they know which broker to read from. If the broker
fails, consumers know how to recover. The consumers read data in order within each partitions.

If a consumer reads from different partitions, they will read each partition offsets in order,
but, it can read some offsets from one partition, then some others on other partition, and go back
to the previous partition to read some more offsets.


### Consumer Groups

Consumers (a program basically) read data in consumer groups. Each consumer within a group
reads from a exclusive partition. If there are more consumers than partitions, some consumers
will be inactive.

I.e:

Say we have three partitions:

```
- Topic-A, partition0
- Topic-A, partition1
- Topic-A, partition2
```

Then consumer groups will read from them like following. 

```
Consumer-group with two consumers:

- Consumer-group-app1, consumer-1: Topic-A, partition0 and Topic-A, partition1
- Consumer-group-app1, consumer-2: Topic-A, partition2

Consumer-group with three consumers:

- Consumer-group-app2, consumer-1: Topic-A, partition0
- Consumer-group-app2, consumer-2: Topic-A, partition1
- Consumer-group-app2, consumer-3: Topic-A, partition2

Consumer-group with one consumer:

- Consumer-group-app2, consumer-1: Topic-A, partition0, Topic-A, partition1, Topic-A, partition2
```

The consumers in a group are automatically assigned to a partition.

**It's not usual to have more consumers than partitions!**


### Consumer Offsets

Kafka can store the offsets at which a consumer group has been reading. The offsets are committed
in Kafka Topic named `__consumer_offsets`. The offsets commits should be done after processing
data read from Kafka (This is done automatically). This way if a consumer dies, it can start
over from where it left.


### Delivery semantics for consumers

Consumers choose when to commit offsets:

- At most once: Commit as soon as message is received (data loss).
- At least once (preferred): Commit when message is received and processed. This can drive
to message duplication, so it's important that messages processing is **idempotent**.
- Exactly once: This is for communicating different instances of Kafka.

### Kafka Broker discovery

Each broker knows about all the other's topics and partitions in the entire cluster. You only need 
to connect to a single broker and it's possible to figure out where to alternatively connect
in case of failure. All this is managed automatically by Kafka.


### Zookeeper

The Zookeeper manages all brokers, it helps in leader election for partitions. It also
notifies Kafka about any events (new Topic, broker dies, etc...). Zookeeper is needed by
Kafka, it's mandatory.

It's necessary to keep an odd number of Zookeepers. Zookeeper has a leader as well, which
handle Kafka metadata writes. The other instances are called followers and they handle metadata reads.

### Guarantees

* Messages are appended to a topic-partition in the order they are sent.
* Consumers read messages in the order stored in a topic-partition.
* With a replication factor of N, producers and consumers can tolerate up to a N-1 brokers
  being down.
* As long as the number of partitions remains constant for a topic, the same key will
  always go to the same partition (Hummm...).
  

## Kafka Services

To start Zookeeper:

```
zookeper-server-start path_to/zookeeper.properties
```

By default it listens on port 2181.

Zookeeper data dir is pointing to /tmp/zookeeper by default. Change it!

To start Kafka:

```
kafka-server-start path_to/server.properties
```

By default it listens on port 9092.

Kafka logs dir is pointing to /tmp/kafka-logs by default. Change it as well!


## Kafka CLI

### kafka-topics command

#### Create a new topic:

```
kafka-topics --zookeeper 127.0.0.1:2181 --topic sample_topic --create --partitions 3 --replication-factor 1
```

Zookeeper connection must be indicated.

The replication factor of 1 is not cool, but for now I only have one broker :(


#### List topics

```
kafka-topics --zookeeper 127.0.0.1:2181 --list
```


#### Describe topics

```
kafka-topics --zookeeper 127.0.0.1:2181 --topic sample_topic --describe
```

Output:

```
Topic:sample_topic	PartitionCount:3	ReplicationFactor:1	Configs:
	Topic: sample_topic	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic: sample_topic	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
	Topic: sample_topic	Partition: 2	Leader: 0	Replicas: 0	Isr: 0
```


#### Delete topics

```
kafka-topics --zookeeper 127.0.0.1:2181 --topic sample_topic --delete
```

### kafka-console-producer command

#### Produce to a topic

```
kafka-console-producer --broker-list localhost:9092 --topic sample_topic
```

You will be prompted with a `>` sign. Then you can start to produce messages:

```
>This is a cool message
>This is another one
>Best message ever
```

#### Setting ad-hoc properties

```
kafka-console-producer --broker-list localhost:9092 --topic sample_topic --producer-properties acks=all
```

If you produce to a non-existing topic, it will be created with the default values. Usually with 1 partition
and 1 replication factor. That is not good. If you want to change the defaults, this can be done in
`server.properties` file.


### kafka-console-consumer command


#### Consuming messages

```
kafka-console-consumer --bootstrap-server localhost:9092 --topic sample_topic
```

Nothing is shown at first, since the consumer will only read the messages in topic. To see something
it's necessary to produce messages while the consumer is active.

To show all messages:

```
kafka-console-consumer --bootstrap-server localhost:9092 --topic sample_topic --from-beginning
```

#### Consume in a group

Set the `--group` parameter with any string as the group name

```
kafka-console-consumer --bootstrap-server localhost:9092 --topic sample_topic --group my-app
```

You can start more consumers in the same group, they will consume from a fixed partition. If one consumer
goes down, the partitions are reassigned.

Take into account that when you consume as a group, the offset is committed. So even using `from-beginning`
option, previous messages won't be consumed again. Only new ones will be.


### kafka-consumer-groups command

#### List groups

```
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

#### Describe a group

```
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group my-app
```

Output:

```
Consumer group 'my-app' has no active members.

TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
sample_topic    1          3               3               0               -               -               -
sample_topic    0          2               2               0               -               -               -
sample_topic    2          2               2               0               -               -               -
```

Lag 0 means all messages has been consumed in a partition.


#### Resetting offsets for a group

```
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --reset-offsets --to-earliest --execute --group my-app --topic sample_topic
```

Resets offsets for group `my-app` for the topic `sample_topic`

A describe now shows the `LAG` to the total messages on each partition

```
Consumer group 'my-app' has no active members.

TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
sample_topic    1          0               3               3               -               -               -
sample_topic    0          0               2               2               -               -               -
sample_topic    2          0               2               2               -               -               -
```

To reset to, say 3 previous messages use the `--shift-by -3` option.


### Alternatives to CLI

#### Kafka Tool (GUI)

http://www.kafkatool.com/features.html

#### KafkaCat (curated CLI commands)

https://github.com/edenhill/kafkacat