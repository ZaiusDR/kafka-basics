package twitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter.elasticsearch.ElasticSearchClient;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class TwitterDemoApp {

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(TwitterDemoApp.class.getName());

        // Setup ElasticSearch Index
        ElasticSearchClient elasticSearchClient = new ElasticSearchClient();
        elasticSearchClient.createIndex();

        // Create Topic in Kafka
        TwitterTopic.createTopic();

        // Start Producer to fetch Tweets and inserting them in Kafka
        TwitterAuth twitterAuth = new TwitterAuth();
        TwitterAPIStreamClient twitterAPIStreamClient = new TwitterAPIStreamClient(twitterAuth);

        CountDownLatch producerLatch = new CountDownLatch(1);
        TwitterProducer producer = new TwitterProducer(twitterAPIStreamClient, producerLatch);
        Thread producerThread = new Thread(producer);
        producerThread.start();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(producer::shutDown));

        // Start Consumer to read from Kafka and insert messages in ElasticSearch
        CountDownLatch consumerLatch = new CountDownLatch(1);
        TwitterConsumer consumer = new TwitterConsumer(elasticSearchClient, consumerLatch);
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::shutDown));

        try {
            producerLatch.await();
            consumerLatch.await();
        } catch (InterruptedException e) {
            logger.info("Application got interrupted.", e);
        } finally {
            logger.info("Application is closing.");
        }

    }
}
