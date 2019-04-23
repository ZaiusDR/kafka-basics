package twitter;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import io.prometheus.client.exporter.PushGateway;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    // Metrics
    // CollectorRegistry registry = new CollectorRegistry();
    PushGateway pushGateway = new PushGateway("localhost:9091");
    static final Counter messages = Counter.build()
            .name("messages_total").help("Total produced messages.").register();
    static final Summary messageBytes = Summary.build()
            .name("messages_size_bytes").help("Message size in bytes.").register();
    static final Summary messageLatency = Summary.build()
            .name("messages_latency_seconds").help("Message latency in seconds.").register();

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

        // Fine Tuning
        // For safety ordering and replication
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        // High throughput (at the expense of a bit of latency and CPU usage)
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20"); //ms
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));

        this.producer = new KafkaProducer<>(properties);
    }

    public void run() {
        this.twitterAPIStreamClient.getClient().connect();

        try {
            for (int msgRead = 0; msgRead < 10000; msgRead++) {
                messages.inc();
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
                    try {
                        logger.error("Pushing data to PushGateway {}", msg);
                        pushGateway.push(messages, "messages");
                    } catch(IOException e) {
                        logger.error("Error sending data to PushGateway {}", e);
                    }
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
