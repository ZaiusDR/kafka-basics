package demo;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static demo.Constants.*;
import static java.util.Collections.singletonList;

public class ConsumerDemoAssignSeek {

    public static void main(String[] args) {
        new ConsumerDemoAssignSeek().run();
    }

    private ConsumerDemoAssignSeek() {

    }

    private void run() {
        Logger logger = LoggerFactory.getLogger(ConsumerDemoAssignSeek.class.getName());

        // Latch for dealing with multiple threads
        CountDownLatch latch = new CountDownLatch(1);

        // Create Consumer Runnable
        logger.info("Creating the consumer.");
        Runnable consumerRunnable = new ConsumerRunnable(latch);

        // Start thread
        Thread thread = new Thread(consumerRunnable);
        thread.start();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread( () -> {
            logger.info("Caught shutdown hook");
            ((ConsumerRunnable) consumerRunnable).shutDown();
        }

        ));

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.info("Application got interrupted.", e);
        } finally {
            logger.info("Application is closing.");
        }
    }

    public class ConsumerRunnable implements Runnable {

        private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class.getName());

        private CountDownLatch latch;
        private KafkaConsumer<String, String> consumer;

        ConsumerRunnable(CountDownLatch latch) {
            this.latch = latch;

            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET);

            consumer = new KafkaConsumer<>(properties);

            TopicPartition partition = new TopicPartition(TOPIC_NAME, 0);

            consumer.assign(singletonList(partition));

            long offset = 1;

            consumer.seek(partition, offset);

        }

        @Override
        public void run() {
            int numberOfMessagesToRead = 200;
            boolean keepOnReading = true;
            int numberOfMessagesReadSoFar = 0;
            try {
                while (keepOnReading) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record : records) {
                        numberOfMessagesReadSoFar += 1;
                        logger.info("Key: " + record.key() + ", Value: " + record.value());
                        logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                        if (numberOfMessagesReadSoFar >= numberOfMessagesToRead) {
                            keepOnReading = false; // Exit while
                            break; // Exit for
                        }
                    }
                }
                logger.info("Exiting the application");

            } catch (WakeupException e) {
                logger.info("Received shutdown signal!");
            } finally {
                consumer.close();
                latch.countDown();
            }
        }

        void shutDown() {
            consumer.wakeup();
        }
    }
}
