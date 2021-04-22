package courgette.integration.reportportal;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import courgette.integration.reportportal.request.FinishRequest;
import courgette.integration.reportportal.request.LaunchRequest;
import courgette.integration.reportportal.request.ScenarioLogRequest;
import courgette.integration.reportportal.request.ScenarioRequest;
import courgette.integration.reportportal.request.TestRequest;
import courgette.integration.reportportal.request.TestSuiteRequest;
import courgette.runtime.CourgetteProperties;
import courgette.runtime.CourgetteRunnerInfo;
import courgette.runtime.report.JsonReportParser;
import courgette.runtime.report.model.Scenario;
import courgette.runtime.report.model.Tag;
import courgette.runtime.utils.FileUtils;
import io.cucumber.core.gherkin.Feature;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static courgette.runtime.CourgetteException.printError;

public class ReportPortalService {

    private final ReportPortalProperties reportPortalProperties;
    private final CourgetteProperties courgetteProperties;
    private final HttpClient httpClient;
    private final List<Feature> features;

    private final ConcurrentHashMap<String, String> testIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> testCounters = new ConcurrentHashMap<>();

    private String launchId;
    private String testSuiteId;

    public ReportPortalService(ReportPortalProperties reportPortalProperties,
                               CourgetteProperties courgetteProperties,
                               List<Feature> features) {

        this.reportPortalProperties = reportPortalProperties;
        this.courgetteProperties = courgetteProperties;
        this.httpClient = createHttpClient();
        this.features = features;
    }

    public void startLaunch() {
        launchId = call(post(apiEndpoint() + "/launch", new LaunchRequest().create(reportPortalProperties)));
    }

    public void startTestSuite() {
        if (launchId != null) {
            testSuiteId = call(post(apiEndpoint() + "/item", new TestSuiteRequest().create(reportPortalProperties.getTestSuite(), launchId)));
        }

        if (testSuiteId != null) {
            createTestIds(testIds, features);
            createTestCounters(testCounters, features);
        }
    }

    public void addTest(CourgetteRunnerInfo runnerInfo) {

        if (testSuiteId != null) {

            final String testName = createFeatureName(runnerInfo.getFeature());

            final File reportFile = runnerInfo.getJsonReportFile();

            if (reportFile == null) {
                printError("Courgette Report Portal Service: unable to add test due to missing report data. Feature URI -> " + runnerInfo.getFeature().getUri());
                return;
            }

            final List<courgette.runtime.report.model.Feature> features = JsonReportParser
                    .create(reportFile, courgetteProperties.getCourgetteOptions().runLevel())
                    .getReportFeatures();

            final String testId = testIds.get(testName);

            List<Scenario> scenarios = features.stream()
                    .flatMap(feature -> feature.getScenarios().stream())
                    .collect(Collectors.toList());

            for (Scenario scenario : scenarios) {
                if (testId != null) {
                    final String scenarioId = startScenario(testId, scenario);

                    if (scenarioId != null) {
                        addScenarioLog(scenarioId, scenario);
                        finishScenario(scenarioId, scenario.passed() ? "passed" : "failed");
                        testCounters.put(testName, testCounters.get(testName) - 1);
                    }
                }
            }

            if (testCounters.get(testName) <= 0) {
                finishTest(testId);
            }
        }
    }

    public void finishTestSuite() {
        if (testSuiteId != null) {
            call(put(apiEndpoint() + "/item/" + testSuiteId, new FinishRequest().create(launchId)));
        }
    }

    public void finishLaunch() {
        if (launchId != null) {
            call(put(apiEndpoint() + "/launch/" + launchId + "/finish", new FinishRequest().create(launchId)));
        }
    }

    private String startTest(String featureName) {
        return call(post(apiEndpoint() + "/item/" + testSuiteId, TestRequest.create(featureName, launchId)));
    }

    private void finishTest(String testId) {
        if (testId != null) {
            call(put(apiEndpoint() + "/item/" + testId, new FinishRequest().create(launchId)));
        }
    }

    private String startScenario(String testId, Scenario scenario) {
        return call(post(apiEndpoint() + "/item/" + testId,
                ScenarioRequest.create(
                        scenario.getName(),
                        scenario.getTags().stream().map(Tag::getName).collect(Collectors.toList()),
                        launchId)));
    }

    private void addScenarioLog(String scenarioId, Scenario scenario) {
        call(post(apiEndpoint() + "/log", new ScenarioLogRequest().create(scenario, scenarioId, launchId)));
    }

    private void finishScenario(String scenarioId, String status) {
        call(put(apiEndpoint() + "/item/" + scenarioId, new FinishRequest().create(status, launchId)));
    }

    private String createFeatureName(Feature feature) {
        return FileUtils.getFileName(feature.getUri()).split("\\.")[0];
    }

    private void createTestIds(ConcurrentHashMap<String, String> testIds, List<Feature> features) {
        features.stream().map(this::createFeatureName).distinct().sorted().forEach(testName -> testIds.put(testName, startTest(testName)));
    }

    private void createTestCounters(ConcurrentHashMap<String, Long> testCounters, List<Feature> features) {
        testCounters.putAll(features.stream().map(this::createFeatureName).collect(Collectors.groupingBy(Function.identity(), Collectors.counting())));
    }

    private String apiEndpoint() {
        return String.format("%s/api/v1/%s", reportPortalProperties.getEndpoint(), reportPortalProperties.getProject());
    }

    private HttpPost post(final String uri, HttpEntity entity) {
        HttpPost post = new HttpPost(uri);
        post.addHeader(authHeader());
        post.setEntity(entity);
        return post;
    }

    private HttpPut put(final String uri, HttpEntity entity) {
        HttpPut put = new HttpPut(uri);
        put.addHeader(authHeader());
        put.setEntity(entity);
        return put;
    }

    private Header authHeader() {
        return new BasicHeader("Authorization", "bearer " + reportPortalProperties.getApiToken());
    }

    private HttpClient createHttpClient() {
        try {
            SSLContext trustedSSLContext = new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build();
            return HttpClientBuilder.create().setSSLContext(trustedSSLContext).build();
        } catch (Exception e) {
            printError("Courgette Report Portal Service: error creating a secure http client: " + e.getMessage());
        }

        return HttpClientBuilder.create().build();
    }

    private synchronized String call(final HttpUriRequest request) {
        String id = null;

        String responseBody;

        try {
            HttpResponse response = httpClient.execute(request);

            responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

            if (isSuccessfulCall.test(response.getStatusLine().getStatusCode())) {

                JsonElement idElement = JsonParser.parseString(responseBody).getAsJsonObject().get("id");

                if (idElement != null) {
                    id = idElement.getAsString();
                }
            } else {
                printError("Courgette Report Portal Service: " + responseBody);
            }
        } catch (IOException e) {
            printError("Courgette Report Portal Service: " + e.getMessage());
        }

        return id;
    }

    private final Predicate<Integer> isSuccessfulCall = (status) -> status == 200 || status == 201;
}
