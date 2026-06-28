package courgette.runtime;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import courgette.runtime.utils.FileUtils;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CucumberReportPublisherTest {
    private HttpServer server;

    @After
    public void tearDown() {
        System.clearProperty("cucumber.publish.url");
        System.clearProperty("CUCUMBER_PUBLISH_TOKEN");

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void publishesGzippedCucumberMessages() throws Exception {
        CapturedRequest capturedRequest = new CapturedRequest();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/reports", exchange -> createReport(exchange, capturedRequest));
        server.createContext("/reports/upload", exchange -> capturePublishedReport(exchange, capturedRequest));
        server.start();

        String publishUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/reports";
        System.setProperty("cucumber.publish.url", publishUrl);
        System.setProperty("CUCUMBER_PUBLISH_TOKEN", "secret-token");

        File messageFile = File.createTempFile("cucumber", ".ndjson");
        FileUtils.writeFile(messageFile.getPath(), "{\"testRunStarted\":{}}\n");

        Optional<String> reportUrl = new CucumberReportPublisher(messageFile).publish();

        assertTrue(reportUrl.isPresent());
        assertEquals("https://reports.cucumber.io/reports/report-id", reportUrl.get());
        assertEquals("/reports/upload", capturedRequest.publishReportPath);
        assertEquals("Bearer secret-token", capturedRequest.createReportAuthorization);
        assertNull(capturedRequest.publishReportAuthorization);
        assertEquals("gzip", capturedRequest.contentEncoding);
        assertEquals("{\"testRunStarted\":{}}\n", capturedRequest.body);
    }

    private void createReport(HttpExchange exchange, CapturedRequest capturedRequest) throws IOException {
        capturedRequest.createReportAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        String location = "http://127.0.0.1:" + server.getAddress().getPort() + "/reports/upload";
        byte[] response = ("\u001B[32m┌──────────────────────────────────────────────────────────────────────────┐\u001B[0m\n" +
                "\u001B[32m│\u001B[0m \u001B[1m\u001B[32mView your Cucumber Report at:\u001B[0m \u001B[1m\u001B[4m\u001B[36mhttps://reports.cucumber.io/reports/report-id\u001B[0m \u001B[1m\u001B[32m│\u001B[0m\n" +
                "\u001B[32m│\u001B[0m \u001B[1mThis report will self-destruct in 24h.\u001B[0m \u001B[1m\u001B[32m│\u001B[0m\n" +
                "\u001B[32m└──────────────────────────────────────────────────────────────────────────┘\u001B[0m")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(202, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private void capturePublishedReport(HttpExchange exchange, CapturedRequest capturedRequest) throws IOException {
        capturedRequest.publishReportPath = exchange.getRequestURI().getPath();
        capturedRequest.publishReportAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        capturedRequest.contentEncoding = exchange.getRequestHeaders().getFirst("Content-Encoding");
        capturedRequest.body = gunzip(exchange);
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private String gunzip(HttpExchange exchange) throws IOException {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(exchange.getRequestBody());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static class CapturedRequest {
        private String createReportAuthorization;
        private String publishReportPath;
        private String publishReportAuthorization;
        private String contentEncoding;
        private String body;
    }
}
