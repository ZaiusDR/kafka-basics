package twitter;

public class KafkaConstants {

    static final String BOOTSTRAP_SERVERS = "localhost:9092";

    static final String TOPIC_NAME = "tweets";
    static final int TOPIC_PARTITIONS = 3;
    static final short REPLICATION_FACTOR = 1;

    static final String GROUP_ID = "twitter-app";
    static final String OFFSET_RESET = "earliest";

    // Streams
    static final String APP_ID = "twitter-app-streams";
    static final String IMPORTANT_TWEETS_TOPIC = "important_tweets";
}
