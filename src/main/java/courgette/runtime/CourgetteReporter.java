package courgette.runtime;

import courgette.runtime.report.JsonReportParser;
import courgette.runtime.utils.FileUtils;
import io.cucumber.messages.types.Envelope;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static courgette.runtime.utils.FileUtils.writeFile;

class CourgetteReporter {
    private final CourgetteProperties courgetteProperties;
    private final List<CourgetteReportOptions> courgetteReportOptions;
    private final Map<String, Collection<String>> errors = new HashMap<>();
    private JsonReportParser jsonReportParser;

    public CourgetteReporter(List<CourgetteReportOptions> courgetteReportOptions, CourgetteProperties courgetteProperties) {
        this.courgetteProperties = courgetteProperties;
        this.courgetteReportOptions = courgetteReportOptions;
    }

    Optional<String> createCucumberReports(List<String> reportFiles, boolean publishReport) {
        final Optional<String> htmlReportFile = reportFiles.stream().filter(report -> report.endsWith(".html")).findFirst();
        final Optional<String> jsonReportFile = reportFiles.stream().filter(report -> report.endsWith(".json")).findFirst();
        final Optional<String> ndJsonReportFile = reportFiles.stream().filter(report -> report.endsWith(".ndjson")).findFirst();
        final Optional<String> xmlReportFile = reportFiles.stream().filter(report -> report.endsWith(".xml")).findFirst();

        String jsonFile = jsonReportFile.orElseGet(() -> FileUtils.createTempFile("json").getPath());

        jsonReportParser = new JsonReportParser(jsonFile, courgetteProperties.isFeatureRunLevel());

        List<String> jsonReports = courgetteReportOptions.stream()
                .map(CourgetteReportOptions::getJsonFile)
                .collect(Collectors.toList());

        CucumberJsonReporter cucumberJsonReporter = new CucumberJsonReporter(jsonFile, jsonReports.size());
        jsonReports.forEach(cucumberJsonReporter::readAndWriteReport);

        if (cucumberJsonReporter.hasErrors()) {
            errors.put(jsonFile, cucumberJsonReporter.getErrors().values());
        }

        Map<String, List<CourgetteReportOptions>> groupedReports = courgetteReportOptions.stream()
                .collect(Collectors.groupingBy(CourgetteReportOptions::getFeatureId));

        String ndJsonFile = ndJsonReportFile.orElseGet(() -> FileUtils.createTempFile("ndjson").getPath());

        CucumberNdJsonReporter cucumberNdJsonReporter = new CucumberNdJsonReporter(ndJsonFile, groupedReports.size());

        CucumberHtmlReporter cucumberHtmlReporter;
        if (htmlReportFile.isPresent() && courgetteProperties.isCucumberHtmlReportEnabled() && FileUtils.createFile(htmlReportFile.get())) {
            cucumberHtmlReporter = new CucumberHtmlReporter(htmlReportFile.get(), groupedReports.size());
        } else {
            cucumberHtmlReporter = null;
        }

        groupedReports.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach((entry) -> {
                    Map<String, List<List<Envelope>>> messages = cucumberNdJsonReporter.readReports(entry);

                    if (!messages.isEmpty()) {
                        List<Envelope> runLevelMessages = createRunLevelMessages(messages);

                        cucumberNdJsonReporter.writeReport(runLevelMessages);

                        if (cucumberHtmlReporter != null) {
                            cucumberHtmlReporter.writeReport(runLevelMessages);
                        }
                    }
                });

        if (cucumberNdJsonReporter.hasErrors()) {
            errors.put(ndJsonFile, cucumberNdJsonReporter.getErrors().values());
        }

        if (cucumberHtmlReporter != null && cucumberHtmlReporter.hasErrors()) {
            errors.put(htmlReportFile.get(), cucumberHtmlReporter.getErrors().values());
        }

        if (xmlReportFile.isPresent()) {
            List<String> xmlReports = courgetteReportOptions.stream()
                    .map(CourgetteReportOptions::getXmlFile)
                    .map(report -> report.orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            boolean mergeTestCaseName = courgetteProperties.isReportPortalPluginEnabled();

            CucumberXmlReporter cucumberXmlReporter = new CucumberXmlReporter(xmlReportFile.get(), xmlReports.size());
            xmlReports.forEach(report -> cucumberXmlReporter.readAndWriteReport(report, mergeTestCaseName, courgetteProperties.isReportPortalPluginEnabled()));

            if (cucumberXmlReporter.hasErrors()) {
                errors.put(xmlReportFile.get(), cucumberXmlReporter.getErrors().values());
            }
        }

        Optional<String> publishedReport = Optional.empty();

        if (publishReport) {
            publishedReport = publishCucumberReport(ndJsonFile);
        }

        return publishedReport;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void deleteTemporaryReports() {
        courgetteReportOptions.stream().map(CourgetteReportOptions::getJsonFile).forEach(FileUtils::deleteFileSilently);
        courgetteReportOptions.stream().map(CourgetteReportOptions::getNdJsonFile).forEach(FileUtils::deleteFileSilently);
        courgetteReportOptions.stream().map(CourgetteReportOptions::getXmlFile).forEach(report -> report.ifPresent(FileUtils::deleteFileSilently));
        courgetteReportOptions.stream().map(CourgetteReportOptions::getRerunFile).forEach(rerun -> rerun.ifPresent(FileUtils::deleteFileSilently));
    }

    public JsonReportParser jsonReportParser() {
        return jsonReportParser;
    }

    public void createErrorReport() {
        StringBuilder errors = new StringBuilder();
        this.errors.forEach((key, value1) -> {
            errors.append(String.format("Report file: %s\n", key));
            errors.append("Processing errors:\n");
            value1.forEach(value -> {
                errors.append(String.format("\t%s\n", value));
            });
            errors.append("\n");
        });

        String errorReportFile = courgetteProperties.getCourgetteOptions().reportTargetDir() + File.separator + "courgette-report-processing-errors.txt";
        FileUtils.writeFile(errorReportFile, errors.toString());
    }

    private Optional<String> publishCucumberReport(String messagesFile) {
        Optional<String> reportUrl = Optional.empty();

        final File ndJsonReport = new File(messagesFile);

        if (ndJsonReport.exists()) {

            CucumberReportPublisher reportPublisher = new CucumberReportPublisher(ndJsonReport);
            reportUrl = reportPublisher.publish();

            StringBuilder out = new StringBuilder();

            if (reportUrl.isPresent()) {
                out.append("\n─────────────────────────────────────────────────────────────────────────\n");
                out.append("Report published at: ").append(Instant.now()).append("\n");
                out.append("\nCourgette published your Cucumber Report to:\n");
                out.append(reportUrl.get());
                out.append("\n─────────────────────────────────────────────────────────────────────────\n");
                System.out.println(out);

                String reportLinkFilename = courgetteProperties.getCourgetteOptions().reportTargetDir() + File.separator + "cucumber-report-link.txt";
                writeFile(reportLinkFilename, out.toString());
            }
        }
        return reportUrl;
    }

    private List<Envelope> createRunLevelMessages(Map<String, List<List<Envelope>>> messages) {
        final CourgetteNdJsonCreator ndJsonCreator = new CourgetteNdJsonCreator(messages);

        return new ArrayList<>(courgetteProperties.isFeatureRunLevel() ?
                ndJsonCreator.createFeatureMessages() : ndJsonCreator.createScenarioMessages());
    }
}
