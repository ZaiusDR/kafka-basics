package twitter;

import com.google.gson.JsonParser;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

import static twitter.KafkaConstants.*;

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
