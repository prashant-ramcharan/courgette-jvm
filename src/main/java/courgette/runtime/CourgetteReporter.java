package courgette.runtime;

import io.cucumber.messages.Messages;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.FileUtils.writeFile;

class CourgetteReporter {

    private Map<String, CopyOnWriteArrayList<String>> reports;
    private List<Messages.Envelope> messages;
    private CourgetteRuntimeOptions courgetteRuntimeOptions;
    private CourgetteProperties courgetteProperties;
    private boolean canCreateCucumberReports;

    CourgetteReporter(Map<String, CopyOnWriteArrayList<String>> reports,
                      Map<io.cucumber.core.gherkin.Feature, List<List<Messages.Envelope>>> reportMessages,
                      CourgetteRuntimeOptions courgetteRuntimeOptions,
                      CourgetteProperties courgetteProperties) {

        this.reports = reports;
        this.courgetteRuntimeOptions = courgetteRuntimeOptions;
        this.courgetteProperties = courgetteProperties;

        this.messages = createMessages(reportMessages);
        this.canCreateCucumberReports = this.messages != null;

        if (canCreateCucumberReports) {
            createNdJsonReport(this.messages);
        }
    }

    boolean canCreateCucumberReports() {
        return canCreateCucumberReports;
    }

    void createCucumberReport(String reportFile, boolean mergeTestCaseName) {

        if (reportFile != null && !reports.isEmpty()) {

            final List<String> reportData = getReportData();

            final boolean isHtml = reportFile.endsWith(".html");
            final boolean isJson = reportFile.endsWith(".json");
            final boolean isNdJson = reportFile.endsWith(".ndjson");
            final boolean isXml = reportFile.endsWith(".xml");

            if (isHtml) {
                CucumberHtmlReporter.createReport(reportFile, messages);
            }

            if (isJson) {
                reportData.removeIf(report -> !report.startsWith("["));
                CucumberJsonReporter.createReport(reportFile, reportData);
            }

            if (isNdJson) {
                if (!reportFile.equals(courgetteRuntimeOptions.getCourgetteReportNdJson())) {
                    CucumberNdJsonReporter.copyReport(courgetteRuntimeOptions.getCourgetteReportNdJson(), reportFile);
                }
            }

            if (isXml) {
                reportData.removeIf(report -> !report.startsWith("<?xml"));
                CucumberXmlReporter.createReport(reportFile, reportData, mergeTestCaseName, courgetteProperties.isReportPortalPluginEnabled());
            }
        }
    }

    Optional<String> publishCucumberReport() {
        Optional<String> reportUrl = Optional.empty();

        if (courgetteProperties.isCucumberReportPublisherEnabled()) {

            final File ndJsonReport = new File(courgetteRuntimeOptions.getCourgetteReportNdJson());

            if (ndJsonReport.exists()) {

                CucumberReportPublisher reportPublisher = new CucumberReportPublisher(ndJsonReport);
                reportUrl = reportPublisher.publish();

                StringBuilder out = new StringBuilder();

                if (reportUrl.isPresent()) {
                    out.append("\n------------------------------------------------------------------------\n");
                    out.append("Report published at: ").append(Instant.now()).append("\n");
                    out.append("\nCourgette published your Cucumber Report to:\n");
                    out.append(reportUrl.get());
                    out.append("\n------------------------------------------------------------------------\n");
                    System.out.println(out.toString());

                    String reportLinkFilename = courgetteProperties.getCourgetteOptions().reportTargetDir() + File.separator + "cucumber-report-link.txt";
                    writeFile(reportLinkFilename, out.toString());
                }
            }
        }
        return reportUrl;
    }

    private List<String> getReportData() {
        final List<String> reportData = new ArrayList<>();

        final Map<String, CopyOnWriteArrayList<String>> reportMap = new LinkedHashMap<>();

        this.reports.values().removeIf(t -> t.contains(null) || t.contains("null") || t.contains("[]") || t.contains(""));

        this.reports.forEach(reportMap::put);

        reportMap.values().forEach(report -> {
            try {
                reportData.add(report.get(0));
            } catch (Exception e) {
                printExceptionStackTrace(e);
            }
        });
        return reportData;
    }

    private List<Messages.Envelope> createMessages(Map<io.cucumber.core.gherkin.Feature, List<List<Messages.Envelope>>> reportMessages) {
        final CourgetteNdJsonCreator ndJsonCreator = new CourgetteNdJsonCreator(reportMessages);

        return courgetteProperties.isFeatureRunLevel() ?
                ndJsonCreator.createFeatureMessages() : ndJsonCreator.createScenarioMessages();
    }

    private void createNdJsonReport(List<Messages.Envelope> messages) {
        CucumberNdJsonReporter.createReport(courgetteRuntimeOptions.getCourgetteReportNdJson(), messages);
    }
}
