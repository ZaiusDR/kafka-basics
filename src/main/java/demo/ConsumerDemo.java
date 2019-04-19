package demo;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import static demo.Constants.*;
import static java.util.Collections.singletonList;

public class ConsumerDemo {

    public static void main(String[] args) {
        new ConsumerDemo().run();
    }

    private ConsumerDemo() {

    }

    private void run() {
        Logger logger = LoggerFactory.getLogger(ConsumerDemo.class.getName());

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
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET);

            consumer = new KafkaConsumer<>(properties);

            consumer.subscribe(singletonList(TOPIC_NAME));
        }

        @Override
        public void run() {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record : records) {
                        logger.info("Key: " + record.key() + ", Value: " + record.value());
                        logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                    }
                }
            } catch (WakeupException e) {
                logger.info("Received shutdown signal!");
            } finally {
                consumer.close();
                latch.countDown();
            }
        }

        public void shutDown() {
            consumer.wakeup();
        }
    }
}
