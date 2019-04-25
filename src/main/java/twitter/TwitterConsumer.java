package twitter;

import com.google.gson.JsonParser;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter.elasticsearch.ElasticSearchClient;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static java.util.Collections.singletonList;
import static twitter.KafkaConstants.*;

class TwitterConsumer implements Runnable {

    private Logger logger = LoggerFactory.getLogger(TwitterConsumer.class.getName());

    private ElasticSearchClient elasticSearchClient;
    private KafkaConsumer consumer;

    private CountDownLatch latch;

    TwitterConsumer(ElasticSearchClient elasticSearchClient, CountDownLatch latch) {
        this.setConsumer();
        this.elasticSearchClient = elasticSearchClient;
        this.latch = latch;
    }

    private void setConsumer() {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET);
        this.consumer = new KafkaConsumer<>(properties);
    }

    public void run() {
        consumer.subscribe(singletonList(TOPIC_NAME));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record : records) {
                    String id = extractIdFromTweet(record.value());
                    logger.info("Consuming message with Id {}: {}", id, record.toString());
                    elasticSearchClient.indexMessage(record.value(), id);
                }
            }
        } catch (WakeupException e) {
            logger.info("Received shutdown signal!");
        } finally {
            consumer.close();
            latch.countDown();
        }
    }

    private String extractIdFromTweet(String tweet) {
        JsonParser jsonParser = new JsonParser();
        return jsonParser.parse(tweet)
                .getAsJsonObject()
                .get("id_str")
                .getAsString();
    }

    void shutDown() {
        consumer.wakeup();
    }
}
