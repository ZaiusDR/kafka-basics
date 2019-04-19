package demo;

class Constants {

    static final String BOOTSTRAP_SERVERS = "localhost:9092";

    static final String TOPIC_NAME = "sample_topic";
    static final int TOPIC_PARTITIONS = 3;
    static final short REPLICATION_FACTOR = 1;

    static final String GROUP_ID = "app1";
    static final String OFFSET_RESET = "earliest";
}
