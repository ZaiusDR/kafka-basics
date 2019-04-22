package twitter;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Properties;

import static java.util.Collections.singletonList;
import static twitter.KafkaConstants.*;

class TwitterTopic {

    static void createTopic() {
        NewTopic topic = new NewTopic(TOPIC_NAME, TOPIC_PARTITIONS, REPLICATION_FACTOR);

        Properties adminProperties = new Properties();
        adminProperties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);

        if (AdminClient.create(adminProperties).describeTopics(singletonList(TOPIC_NAME)).values().isEmpty()) {
            AdminClient.create(adminProperties).createTopics(singletonList(topic));
        }
    }
}
