package twitter;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.Collections.singletonList;

class TwitterAPIStreamClient {

    private BlockingQueue<String> msgQueue;
    private BasicClient client;

    TwitterAPIStreamClient(TwitterAuth twitterAuth) {
        this.configureClient(twitterAuth);
    }

    private void configureClient(TwitterAuth twitterAuth) {
        msgQueue = new LinkedBlockingQueue<>(100000);

        StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();
        endpoint.stallWarnings(false);
        endpoint.trackTerms(singletonList("bitcoin"));

        Authentication auth = new OAuth1(
                twitterAuth.getConsumerKey(),
                twitterAuth.getConsumerSecret(),
                twitterAuth.getToken(),
                twitterAuth.getTokenSecret());

        client = new ClientBuilder()
                .name("twitterClient")
                .hosts(Constants.STREAM_HOST)
                .endpoint(endpoint)
                .authentication(auth)
                .processor(new StringDelimitedProcessor(msgQueue))
                .build();
    }

    BasicClient getClient() {
        return client;
    }

    BlockingQueue<String> getMsgQueue() {
        return msgQueue;
    }

}
