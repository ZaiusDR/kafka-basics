package demo;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.Properties;

import static java.util.Collections.singletonList;

public class ProducerDemo {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    private static final String TOPIC_NAME = "sample_topic";
    private static final int TOPIC_PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1;

    public static void main(String[] args) {
        // Create the topic
        NewTopic topic = new NewTopic(TOPIC_NAME, TOPIC_PARTITIONS, REPLICATION_FACTOR);

        Properties adminProperties = new Properties();
        adminProperties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        AdminClient.create(adminProperties).createTopics(singletonList(topic));


        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        ProducerRecord<String, String> producerRecord =
                new ProducerRecord<>(TOPIC_NAME, "Hello World!");

        producer.send(producerRecord);

        producer.flush();

        // Flush and close
        producer.close();
    }
}
