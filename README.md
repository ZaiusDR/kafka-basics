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
  
