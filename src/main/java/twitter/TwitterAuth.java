package twitter;

public class TwitterAuth {

    public final String TWITTER_CONSUMER_KEY = "TWITTER_CONSUMER_KEY";
    public final String TWITTER_CONSUMER_SECRET = "TWITTER_CONSUMER_SECRET";
    public final String TWITTER_TOKEN = "TWITTER_TOKEN";
    public final String TWITTER_TOKEN_SECRET = "TWITTER_TOKEN_SECRET";

    private String consumerKey;
    private String consumerSecret;
    private String token;
    private String tokenSecret;

    public TwitterAuth() {
        consumerKey = System.getenv(TWITTER_CONSUMER_KEY);
        consumerSecret = System.getenv(TWITTER_CONSUMER_SECRET);
        token = System.getenv(TWITTER_TOKEN);
        tokenSecret = System.getenv(TWITTER_TOKEN_SECRET);
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getToken() {
        return token;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }
}
