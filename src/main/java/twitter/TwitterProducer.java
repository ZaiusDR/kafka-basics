package twitter;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static twitter.KafkaConstants.TOPIC_NAME;
import static twitter.KafkaConstants.BOOTSTRAP_SERVERS;

class TwitterProducer implements Runnable{

    private Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    private KafkaProducer<String, String> producer;

    private TwitterAPIStreamClient twitterAPIStreamClient;

    private CountDownLatch latch;

    TwitterProducer(TwitterAPIStreamClient twitterAPIStreamClient, CountDownLatch latch) {
        this.setProducer();
        this.twitterAPIStreamClient = twitterAPIStreamClient;
        this.latch = latch;

    }

    private void setProducer() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        this.producer = new KafkaProducer<>(properties);
    }

    public void run() {
        this.twitterAPIStreamClient.getClient().connect();

        try {
            for (int msgRead = 0; msgRead < 10000; msgRead++) {
                if (twitterAPIStreamClient.getClient().isDone()) {
                    System.out.println("Client connection closed unexpectedly: " + twitterAPIStreamClient.getClient().getExitEvent().getMessage());
                    break;
                }

                String msg = twitterAPIStreamClient.getMsgQueue().poll(5, TimeUnit.SECONDS);


                if (msg == null) {
                    System.out.println("Did not receive a message in 5 seconds");
                } else {
                    System.out.println(msg);
                    ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC_NAME, msg);

                    producer.send(producerRecord);
                }
            }
        } catch(InterruptedException e) {
            logger.error("Interrupted exception occurred: {}", e.getMessage());
        } finally {
            producer.close();
            latch.countDown();
        }
    }

    void shutDown() {
        producer.close();
    }
}
