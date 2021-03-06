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

## Setting Java Project


### Kafka dependencies

Kafka client:

https://mvnrepository.com/artifact/org.apache.kafka/kafka-clients


### Creating a topic

```
NewTopic topic = new NewTopic(TOPIC_NAME, TOPIC_PARTITIONS, REPLICATION_FACTOR);

Properties adminProperties = new Properties();
adminProperties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

AdminClient.create(adminProperties).createTopics(singletonList(topic));
```

### Setting-up a producer

```
Properties properties = new Properties();
properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
```

### Sending a message

```
ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>(TOPIC_NAME, "Hello World!");

producer.send(producerRecord);
producer.flush();
producer.close();
```

Take into account that `send` is async. That's why in the code there is the `flush` method call.

Also, to be a good citizen, `close` the producer at the end :) 


### Setting-up a consumer

```
Properties properties = new Properties();
properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET);

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
```


### Polling messages

```
ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

for (ConsumerRecord<String, String> record: records) {
    logger.info("Key: " + record.key() + ", Value: " + record.value());
    logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
}
```

### Assign and Seek

This are different APIs, used mostly to replay data or fetch a specific message.

For Assign:

We won't need a Consumer Group for this, just `assign` the Consumer to a Topic and Partition Number.

```
TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);

consumer.assign(singletonList(partition));
```

This way we assign the consumer to a specific partition in a specific topic.

For Seek:

```
long offset = 1;

consumer.seek(partition, offset);
```

This way we indicate the consumer to look for the message with offset 1 in the specified partition.


## Kafka Bi-Directional Compatibility

Since 2017 Kafka client and brokers are fully compatible with every version. This is that client
can be older than brokers, and vice versa. So! Use the newest versions available always!

## Producer and Consumer Tweaking

### Producer

#### Acks

As commented before.

When using `acks=all` the recommended setting is to set `min.insync.replicas=2`. That is at least 2 brokers
that are ISR must response that they have the data. If there are less than the minimum indicated in the
`min.insync.replicas` parameter, the leader will response with an error about not having enough replicas. The producer
will retry until there are enough replicas.


#### Producer retries

Usually developers must lead with exceptions on callback functions. But there is the `retries` option to handle
that for them. The default value is 0. There is a chance of messages will be sent out of order. So if you
are relying in key-based ordering, that can be an important issue.

To control this, there is the `max.in.flight.requests.per.connection` (default 5). It's possible to setup
it to 1, solving the ordering issue, but take into account that this may impact throughput!!


#### Idempotent Producer (For previous issue)

There is a better solution for the previous issue!!

It can happen that the Producer sends a message to Kafka, and is correctly received, but the Ack never
reach the Producer, so it retries sending the message, which will be stored again. Idempotent Producers take
care of this. They come with `retries=Integer.MAX_VALUE`, `max.in.flight.requests=1` or `5` (depending on how
recent is the Kafka version), `acks=all`. To configure them just use `producerProps.put("enable.idempotence", true);`.

So for configuring a safe ordering and replication producer (bit of loss in performance):

```
properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
```

#### Message Compression

Usually messages are text-based, like JSON and so on. It's important to set compression (default `none`in the producer. This
is for free, it does not require any additional config in the Brokers nor the Consumers :)

The compression is more effective for bigger batches of messages sent. Since the size is not only decrease
while sending from Producer to Kafka, but when sending to replicas as well, you get a nice improvement in performance.


#### Batches

Kafka usually send parallel messages. If the max number of parallel requests is reached, it starts batching
the pending ones to improve performance.

This is configurable using the option `linger.ms` (default 0), which is the number of ms a producer will wait until
sending a batch. Raising this value increase the chances of the messages to be batched. This combined with
compression can produce a better performance. There is also the `batch.size` (default 16Kb but can be safely
increased to 32Kb or 64Kb), if the limit of this size is reached, the batch will be sent right away, 
so there is no problem raising the linger a little bit.


#### High throughput Producer

```
properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20"); // ms
properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024)); // Kb
```

### Consumer

#### Delivery Semantics

* __At most once:__ The consumer commits will be done before processing messages. This means that the consumer
reads a batch of messages and then tries to process them. If an error occur during processing, the consumer
will resume from the last commit point, and will leave some messages without processing. 

* __At least once:__ (This is the default) The consumer commits will be done after processing the messages. Same as above.
If there is any problem, some messages will be consumed twice.

* __Exactly once:__ This is done from Kafka to Kafka only.

Most applications should use __At least once__ and ensure that transformations/processing is idempotent.

To do it, there are usually two approaches:
 
- Identify the messages with a "Kafka Generic ID", such as:
```
String id = record.topic() + "_" + record.partition() + "_" + record.offset();
```

- Use an Id which comes inside the message (I.e: In this app, the id of the tweet `id_str`):

```
{
  ...
  ...
  "_score": 0,
  "_source": {
    "created_at": "Thu Apr 25 05:42:14 +0000 2019",
    "id": 1121288284985004000,
    "id_str": "1121288284985004032",
    "text": "Ethereum Price (ETH) Remains In Strong Downtrend Versus Bitcoin (BTC)",
    ...
    ...
```

Remember that this depends on the type of processing done.

#### Commit Strategies

The main ones are:

* (easy) `enable.auto.commit=true`: (Default) It's synchronous processing of batches.
```
while(true) {
    List<Records> batch = consumer.poll(Duration.ofMillis(100));
    doSomethingSynchronous(batch);
}
```

With auto-commit, commits will be done after `auto.commit.interval.ms=5000` (5s) by default, after
calling `.poll()`. So if you don't do it synchronously, you end up with __At most once__!! (Which is not recommended). 

* (medium) `enable.auto.commit=false`: Manual commit of offsets.
```
while(true) {
    batch += consumer.poll(Duration.ofMillis(100));
    if isReady(batch) {
        doSomethingSynchronus(batch);
        consumer.commitSync();
    }
}
```

You can control when you commit offsets and what's the condition for committing them.


#### Bulk Processing

That's it, just create a batch of messages and bulk insert them for instance in Elasticsearch, then
commit the offsets accordingly.


#### Consumer Offset Reset Behavior

* `auto.offset.reset=latest`: will read from the end of the log
* `auto.offset.reset=earliest`: will read from the start of the log
* `auto.offset.reset=none`: will throw exception if no offset is found

Also, the offset retention is 7 days. This can be controlled by `offset.retention.minutes`.

It's possible to reset the offset so consumers __replay__ the entire log. There is no problem when
consumer is idempotent.


#### Consumer Liveliness

Consumers in a group, talk to a Consumers Group Coordinator (One of the brokers). There is a heartbeat 
(`heartbeat.interval.ms` default 3 seconds) system to detect consumers 
down (`session.timeout.ms` default 10 seconds). Is good for Kafka that consumers process data fast 
and poll often.

There is an additional setting `max.poll.interval.ms` (default 5 minutes), which is the time between polls
in order to declare the consumer dead.

If something goes wrong, Kafka re-balances the Consumers!

## Kafka Connect

It's a Kafka Service capable of producing and consuming from/to a lot of usual data sources/storage. It
can be configured in clusters. It does not require any code!! :)

The information can be modified during the process by apps. That's it that Kafka takes care of producing
messages from a typical source (say twitter), then is possible to do something with that info, and
return it to Kafka again with Kafka Streams. Then Kafka will take care of storing it in a typical data storage.


## Kafka Stream

Easy data processing and transformation library. A sample could be:

```
public class TwitterStreamsDemoApp {

    public static void main(String[] args) {
        Properties properties = new Properties();

        properties.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);
        properties.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        properties.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());

        // Create Topology
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        // Input Topic
        KStream<String, String> inputTopic = streamsBuilder.stream(TOPIC_NAME);
        KStream<String, String> filteredStream = inputTopic.filter(
                (k, jsonTweet) -> extractUserFollowers(jsonTweet) > 10000
        );
        filteredStream.to(IMPORTANT_TWEETS_TOPIC);

        // Create topology
        KafkaStreams  kafkaStreams = new KafkaStreams(streamsBuilder.build(), properties);

        // Start our streams application
        kafkaStreams.start();

    }

    private static Integer extractUserFollowers(String tweet) {
        JsonParser jsonParser = new JsonParser();
        return jsonParser.parse(tweet)
                .getAsJsonObject()
                .get("user")
                .getAsJsonObject()
                .get("followers_count")
                .getAsInt();
    }
}
```


## Schema Registry

To avoid the application to break when source information changes, this is where Schema Registry comes into
play.


## Choosing Partition Count & Replication

Rules of thumb:

Partitions per topic:

```
< 6 brokers -> 2 x Number of brokers
> 12 brokers -> 1 x Number of brokers
```

Replication factor of 3 (sometimes 4).

A broker should not hold more than 2000 to 4000 partitions (across all brokers).


Look at best practices on the internet xD


## Kafka Applications In Real-World

The capacity of producing from a source, transform or aggregate information and produce to a new topic is
quite powerful.

It's nice for CQRS model, in which Queries on the information are decoupled from the information sources.

There are super nice Kafka Connect for watching changes in mainstream databases (CDC - Change data captures).

It's recommended that things that happens and are produced to a topic, are stored as events in the topic:

`Good: User 123 enabled threshold at $1000 at 12pm on July 12th 2018`
`Not Good: User 123 threshold $1000`

This makes replaying history much easier.

Kafka is a widely used as pattern for Big Data ingestion. It's capable to produce with high throughput,
while consuming, which has less priority or is not so critical, can be done more slowly. It acts like a huge
buffer.


## Kafka Security

Kafka supports SSL communications. Also authentication and authorization.


## Kafka Topic advanced configuration

### kafka-configs command


When describing a newly created topic:

```
# kafka-topics --zookeeper localhost:2181 --describe --topic configured-topic
Topic:configured-topic	PartitionCount:3	ReplicationFactor:1	Configs:
	Topic: configured-topic	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic: configured-topic	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
	Topic: configured-topic	Partition: 2	Leader: 0	Replicas: 0	Isr: 0
```

There is no `Configs:` available

To add configs, there is the `kafka-configs` command available.

To see the configs:

```
# kafka-configs --zookeeper localhost:2181 --entity-type topics --entity-name configured-topic --describe
Configs for topic 'configured-topic' are
```

To add configurations:

```
# kafka-configs --zookeeper localhost:2181 --entity-type topics --entity-name configured-topic --add-config min.insync.replicas=2 --alter
Completed Updating config for entity: topic 'configured-topic'.

# kafka-configs --zookeeper localhost:2181 --entity-type topics --entity-name configured-topic --describe
Configs for topic 'configured-topic' are min.insync.replicas=2
```

To delete configurations:

```
# kafka-configs --zookeeper localhost:2181 --entity-type topics --entity-name configured-topic --delete-config min.insync.replicas --alter
Completed Updating config for entity: topic 'configured-topic'.
```

### Kafka partition segments and indexes

Partitions are made of segments (which are just files). Each segment contains a group of offsets. There is
only one segment active at a time for a partition to which data is written to.

Segments comes also with two indexes files:

* An offset to position index, which allows kafka to know where to read to find a message.
* A timestamp to offset index, which allows kafka to find a message by its timestamp.

So Kafka knows how to find messages in __constant time__. All files can be found in the folder where
data points in Kafka config file.

Depending on the throughput these parameters can be tweaked:

* `log.segment.bytes` - (Default 1Gb) If you have 1Gb a week maybe it's nice to reduce its value. But
don't reduce to a very small value (such as 1Mb) or Kafka will need to have too many files opened!!
* `log.segment.ms` - (Default 1 week) How often the log compaction is performed.


### Log Cleanup Policies

The default policy is `log.cleanup.policy=delete` which deletes messages after a week and delete based on log
size, which by default is `-1`, what means infinite.

There is another policy `log.cleanup.policy=compact` which is applied by default for commits topic `__consumer_offsets`
This policy deletes messages based on the keys of messages. Essentially will delete duplicated keys after
the active segment is committed.

Messages deletion is aimed to control disk space and delete obsolete data. Also limit maintenance work
on the Kafka Clusters.

In the case of the partition segments, the smaller/more segments, means that log cleanup will happen more often.
It's necessary to leverage the use of CPU/RAM. The default is recommended (`log.cleaner.backoff.ms` to 15 secs.)


### Log Cleanup Delete

There are two main parameters:

* `log.retention.hours=168` - Default is 1 week (168 hours). Higher values means more disk space.
* `log.retention.bytes=-1` - `-1` means infinite size.


### Log Compaction

Log compaction ensures that your log contains at least the last known value for a specific key within a partition.

It's useful if you only require a __snapshot__ instead of the full history.


## Confluent CLI

This is useful for starting a kafka instance for development. You can easily start a Zookeeper and Kafka,
and some helper services such as `ksql-server`, `kafka-rest`, etc... Once you don't need the instance,
you can simply run `confluent destroy` and it will cleanup everything that was created (default topics, etc...).

## Docker setup bootstrap

https://github.com/simplesteph/kafka-stack-docker-compose

