package twitter.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElasticSearchClient {

    private Logger logger = LoggerFactory.getLogger(ElasticsearchClient.class.getName());

    private final String INDEX_NAME = "twitter";

    private RestHighLevelClient client;

    public ElasticSearchClient() {
        client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http"))
        );
    }

    public void createIndex() throws IOException {
        ActionListener<CreateIndexResponse> listener =
                new ActionListener<CreateIndexResponse>() {

                    @Override
                    public void onResponse(CreateIndexResponse createIndexResponse) {
                        logger.info("Successfully created index: {}, response: {}", INDEX_NAME, createIndexResponse.toString());
                    }

                    @Override
                    public void onFailure(Exception e) {
                        logger.error("Error creating index: {}", e.getMessage());
                    }
                };

        GetIndexRequest getIndexRequest = new GetIndexRequest(INDEX_NAME);
        if (!client.indices().exists(getIndexRequest, RequestOptions.DEFAULT)) {
            logger.info("Creating index.");

            CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);

            client.indices().createAsync(request, RequestOptions.DEFAULT, listener);
        } else {
            logger.info("Index already exists.");
        }
    }

    public void indexMessages(HashMap<String, ConsumerRecord<String, String>> messages) {
        ActionListener<BulkResponse> listener =
                new ActionListener<BulkResponse>() {
                    @Override
                    public void onResponse(BulkResponse bulkResponse) {
                        logger.info("Successfully indexed messages.");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        logger.error("Error indexing Message: {}", e.getMessage());
                    }
                };

        BulkRequest bulkRequest = new BulkRequest();

        for (Map.Entry<String, ConsumerRecord<String, String>> stringConsumerRecordEntry : messages.entrySet()) {
            IndexRequest request = new IndexRequest(INDEX_NAME)
                    .id((String) ((Map.Entry) stringConsumerRecordEntry).getKey())
                    .source(((Map.Entry) stringConsumerRecordEntry).getValue(), XContentType.JSON);
            bulkRequest.add(request);
        }

        client.bulkAsync(bulkRequest, RequestOptions.DEFAULT, listener);
    }
}
