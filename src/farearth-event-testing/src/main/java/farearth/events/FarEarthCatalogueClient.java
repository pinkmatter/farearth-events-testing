/*
 * Copyright Pinkmatter Solutions
 * www.pinkmatter.com
 */
package farearth.events;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.commons.codec.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author eduan
 */
public class FarEarthCatalogueClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FarEarthCatalogueClient.class);
    private final CookieStore cookieStore;
    private final String host;
    private boolean connected;

    public FarEarthCatalogueClient(String host) {
        this.cookieStore = new BasicCookieStore();
        this.host = host;
        this.connected = false;
    }

    /**
     * Authenticate against server. If successful, a 'Set-Cookie' header entry
     * with key 'JSESSIONID' will be returned that needs to be specified with
     * each request thereafter. This method only executes if not already
     * connected previously.
     *
     * @param username Username used against server
     * @param password Password for said user
     * @throws Exception On connection failure (e.g. authentication failed)
     */
    public synchronized void connect(String username, String password) throws Exception {
        if (!connected) {
            try (CloseableHttpClient client = createClient()) {
                String url = String.format("%s/catalogue/login", host);
                HttpPost httpPost = new HttpPost(url);

                List<NameValuePair> keyValPairs = new ArrayList<>();
                keyValPairs.add(new BasicNameValuePair("username", username));
                keyValPairs.add(new BasicNameValuePair("password", password));
                httpPost.setEntity(new UrlEncodedFormEntity(keyValPairs, Charsets.UTF_8));

                try (CloseableHttpResponse uploadResponse = client.execute(httpPost)) {
                    int statusCode = uploadResponse.getStatusLine().getStatusCode();
                    if (!(statusCode == 302 || statusCode == 200)) {
                        throw new RuntimeException("Failed to connect: HTTP " + statusCode);
                    }
                }

                LOG.info("Successfully authenticated against endpoint at {}", url);
            }

            connected = true;
        }
    }

    public void getData(String id, Consumer<CloseableHttpResponse> consumer) throws Exception {
        if (!connected) {
            throw new RuntimeException("Not connected");
        }

        try (CloseableHttpClient client = createClient()) {
            String idUrlEncoded = URLEncoder.encode(id, Charsets.UTF_8.name());
            HttpGet httpGet = new HttpGet(String.format("%s/catalogue/data/%s", host, idUrlEncoded));

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                consumer.accept(response);
            }
        }
    }

    public CloseableHttpClient createClient() {
        return HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
    }

    public static String getFilename(CloseableHttpResponse response, String defaultValue) {
        return Arrays.stream(response.getFirstHeader("Content-Disposition").getElements())
                .map(element -> element.getParameterByName("filename"))
                .filter(Objects::nonNull)
                .map(NameValuePair::getValue)
                .findAny()
                .orElse(defaultValue);
    }

    @Override
    public synchronized void close() throws Exception {
        this.connected = false;
        this.cookieStore.clear();
    }
}
