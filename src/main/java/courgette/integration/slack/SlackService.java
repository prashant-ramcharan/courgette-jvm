package courgette.integration.slack;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;

import static courgette.runtime.CourgetteException.printError;

public class SlackService {

    private final String webhookUrl;

    public SlackService(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public synchronized void postMessage(String message) {
        HttpClient client = createHttpClient();

        HttpEntity entity = EntityBuilder
                .create()
                .setContentType(ContentType.APPLICATION_JSON)
                .setText(message)
                .build();

        HttpPost post = new HttpPost(webhookUrl);
        post.setEntity(entity);

        try {
            HttpResponse result = client.execute(post);

            if (result != null && result.getStatusLine().getStatusCode() != 200) {
                String body = EntityUtils.toString(result.getEntity(), "UTF-8");
                printError("Courgette Slack Service: error sending message to Slack channel -> " + body);
            }
        } catch (IOException e) {
            printError("Courgette Slack Service: " + e.getMessage());
        }
    }

    private HttpClient createHttpClient() {
        try {
            SSLContext trustedSSLContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
            return HttpClientBuilder.create().setSSLContext(trustedSSLContext).build();
        } catch (Exception e) {
            printError("Courgette Slack Service: error creating a secure http client -> " + e.getMessage());
        }

        return HttpClientBuilder.create().build();
    }
}
