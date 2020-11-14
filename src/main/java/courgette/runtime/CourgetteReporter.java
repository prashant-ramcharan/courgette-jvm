package courgette.runtime;

import courgette.integration.reportportal.ReportPortalProperties;
import courgette.runtime.utils.FileUtils;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.messages.Messages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

class CourgetteReporter {

    private String reportFile;
    private Map<String, CopyOnWriteArrayList<String>> reports;
    private List<Messages.Envelope> messages;
    private CourgetteProperties courgetteProperties;

    CourgetteReporter(String reportFile,
                      Map<String, CopyOnWriteArrayList<String>> reports,
                      Map<Feature, CopyOnWriteArrayList<String>> reportMessages,
                      CourgetteProperties courgetteProperties) {

        this.reportFile = reportFile;
        this.reports = reports;
        this.messages = createMessages(courgetteProperties, reportMessages);
        this.courgetteProperties = courgetteProperties;
    }

    CourgetteReporter(Map<Feature, CopyOnWriteArrayList<String>> reportMessages, CourgetteProperties courgetteProperties) {
        this.messages = createMessages(courgetteProperties, reportMessages);
        this.courgetteProperties = courgetteProperties;
    }

    void createReport(boolean mergeTestCaseName) {

        if (reportFile != null && !reports.isEmpty()) {

            final List<String> reportData = getReportData();

            final boolean isHtml = reportFile.endsWith(".html");
            final boolean isJson = reportFile.endsWith(".json");
            final boolean isNdJson = reportFile.endsWith(".ndjson");
            final boolean isXml = reportFile.endsWith(".xml");

            if (isHtml) {
                FileUtils.writeFile(reportFile, new CucumberHtmlReporter().createHtmlReport(messages));
            }

            if (isJson) {
                reportData.removeIf(report -> !report.startsWith("["));
                FileUtils.writeFile(reportFile, formatJsonReport(reportData));
            }

            if (isNdJson) {
                FileUtils.writeFile(reportFile, formatNdJsonReport());
            }

            if (isXml) {
                reportData.removeIf(report -> !report.startsWith("<?xml"));
                FileUtils.writeFile(reportFile, formatXmlReport(reportData, mergeTestCaseName));
            }
        }
    }

    Optional<String> publishCucumberReport() {
        Optional<String> reportUrl = Optional.empty();

        final String report = formatNdJsonReport();

        if (!report.isEmpty()) {

            CucumberReportPublisher reportPublisher = new CucumberReportPublisher(report);
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
                FileUtils.writeFile(reportLinkFilename, out.toString());
            }
        }
        return reportUrl;
    }

    private String formatJsonReport(List<String> reports) {
        StringBuilder jsonBuilder = new StringBuilder("[");
        reports.forEach(data -> jsonBuilder.append(data, 1, data.length() - 1).append(","));
        jsonBuilder.deleteCharAt(jsonBuilder.lastIndexOf(","));
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    private String formatXmlReport(List<String> reports, boolean mergeTestCaseName) {
        int failures = 0;
        int skipped = 0;
        int tests = 0;
        double time = 0.0;
        String testSuite = "Test Suite";

        final StringBuilder xmlBuilder = new StringBuilder();

        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        xmlBuilder.append("<testsuite failures=\"id:failures\" name=\"id:testSuite\" skipped=\"id:skipped\" tests=\"id:tests\" time=\"id:time\">\n\n");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (String report : reports) {
                Document document = builder.parse(new InputSource(new StringReader(report)));

                if (document != null) {
                    Element node = document.getDocumentElement();

                    failures = failures + Integer.parseInt(node.getAttribute("failures"));
                    skipped = skipped + Integer.parseInt(node.getAttribute("skipped"));
                    tests = tests + Integer.parseInt(node.getAttribute("tests"));
                    time = time + parseTime(node.getAttribute("time"));

                    NodeList testCases = document.getElementsByTagName("testcase");

                    if (testCases != null) {
                        for (int i = 0; i < testCases.getLength(); i++) {
                            Node testcase = testCases.item(i);

                            if (mergeTestCaseName) {
                                Node testClassName = testcase.getAttributes().getNamedItem("classname");
                                Node testName = testcase.getAttributes().getNamedItem("name");
                                String classNameValue = testClassName.getNodeValue();
                                String testNameValue = testName.getNodeValue();
                                testName.setNodeValue(classNameValue + ": " + testNameValue);
                            }

                            StringWriter sw = new StringWriter();
                            try {
                                Transformer t = TransformerFactory.newInstance().newTransformer();
                                t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                                t.setOutputProperty(OutputKeys.INDENT, "yes");
                                t.transform(new DOMSource(testcase), new StreamResult(sw));

                                xmlBuilder.append(sw.toString()).append("\n");

                            } catch (TransformerException te) {
                                te.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }

        xmlBuilder.append("</testsuite>");

        if (courgetteProperties.isReportPortalPluginEnabled()) {
            testSuite = ReportPortalProperties.getInstance().getTestSuite();
        }

        return xmlBuilder.toString()
                .replace("id:failures", String.valueOf(failures))
                .replace("id:skipped", String.valueOf(skipped))
                .replace("id:tests", String.valueOf(tests))
                .replace("id:time", String.valueOf(time))
                .replace("id:testSuite", testSuite);
    }

    private String formatNdJsonReport() {
        final CourgetteNdJsonCreator ndJsonCreator = new CourgetteNdJsonCreator();
        return ndJsonCreator.toNdJsonMessageString(messages);
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
                e.printStackTrace();
            }
        });
        return reportData;
    }

    private List<Messages.Envelope> createMessages(CourgetteProperties courgetteProperties,
                                                   Map<Feature, CopyOnWriteArrayList<String>> reportMessages) {

        final CourgetteNdJsonCreator ndJsonCreator = new CourgetteNdJsonCreator(reportMessages);
        return courgetteProperties.isFeatureRunLevel() ? ndJsonCreator.createFeatureMessages() : ndJsonCreator.createScenarioMessages();
    }

    private double parseTime(String time) {
        final NumberFormat numberFormat = NumberFormat.getInstance();
        Number timeValue;
        try {
            timeValue = numberFormat.parse(time);
        } catch (ParseException e) {
            timeValue = Double.parseDouble(time.replaceAll(",", ""));
        }
        return timeValue.doubleValue();
    }
}
