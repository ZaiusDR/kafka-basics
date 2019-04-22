package twitter.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
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

    public void indexMessage(String message, String id) {
        ActionListener<IndexResponse> listener =
                new ActionListener<IndexResponse>() {
                    @Override
                    public void onResponse(IndexResponse indexResponse) {
                        logger.info("Successfully created indexed message: {}", indexResponse);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        logger.error("Error indexing Message: {}", e.getMessage());
                    }
                };

        IndexRequest request = new IndexRequest(INDEX_NAME).id(id).source(message, XContentType.JSON);
        client.indexAsync(request, RequestOptions.DEFAULT, listener);
    }
}
