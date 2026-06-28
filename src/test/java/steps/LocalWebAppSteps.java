package steps;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalWebAppSteps {
    private HttpServer server;
    private String baseUrl;
    private HttpResponse lastResponse;

    @Before
    public void startLocalWebApp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/pages/1", exchange -> writeHtml(exchange, "Page 1", "Local web app page 1"));
        server.createContext("/pages/2", exchange -> writeHtml(exchange, "Page 2", "Local web app page 2"));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void stopLocalWebApp() {
        if (server != null) {
            server.stop(0);
        }
    }

    @When("I open local web app page {int}")
    public void openLocalWebAppPage(Integer page) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + "/pages/" + page).openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        lastResponse = new HttpResponse(connection.getResponseCode(), new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    @Then("local web app page {int} is displayed")
    public void localWebAppPageIsDisplayed(Integer page) {
        assertEquals(200, lastResponse.statusCode);
        assertTrue(lastResponse.body.contains("Local web app page " + page));
    }

    private void writeHtml(HttpExchange exchange, String title, String body) throws IOException {
        byte[] response = ("<!doctype html><html><head><title>" + title + "</title></head><body><h1>" + body + "</h1></body></html>")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    private static class HttpResponse {
        private final int statusCode;
        private final String body;

        private HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
