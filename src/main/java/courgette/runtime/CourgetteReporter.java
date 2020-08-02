package courgette.runtime;

import courgette.integration.reportportal.ReportPortalProperties;
import courgette.runtime.utils.FileUtils;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class CourgetteReporter {
    private final String reportFile;
    private final Map<String, CopyOnWriteArrayList<String>> reports;
    private final CourgetteProperties courgetteProperties;

    public CourgetteReporter(String reportFile, Map<String, CopyOnWriteArrayList<String>> reports, CourgetteProperties courgetteProperties) {
        this.reportFile = reportFile;
        this.courgetteProperties = courgetteProperties;

        reports.values().removeIf(t -> t.contains(null) || t.contains("null") || t.contains("[]") || t.contains(""));
        this.reports = reports;
    }

    public void createReport(boolean mergeTestCaseName) {
        if (reportFile != null && !reports.isEmpty()) {
            final Map<String, CopyOnWriteArrayList<String>> reports = new LinkedHashMap<>();

            this.reports.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEachOrdered(x -> reports.put(x.getKey(), x.getValue()));

            final List<String> reportData = new ArrayList<>();

            reports.values().forEach(report -> {
                try {
                    reportData.add(report.get(0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            final boolean isHtml = reportFile.endsWith(".html");
            final boolean isJson = reportFile.endsWith(".json");
            final boolean isNdJson = reportFile.endsWith(".ndjson");
            final boolean isXml = reportFile.endsWith(".xml");

            final CucumberMessageUpdater messageUpdater = new CucumberMessageUpdater();

            if (isHtml) {
                Optional<String> htmlReport = reportData.stream().filter(report -> report.startsWith("<!DOCTYPE html>")).findFirst();

                if (htmlReport.isPresent()) {
                    String report = htmlReport.get();
                    reportData.removeIf(r -> !r.startsWith("<!DOCTYPE html>"));
                    reportData.forEach(messageUpdater::filterCucumberMessages);
                    report = messageUpdater.updateCucumberMessages(report);
                    FileUtils.writeFile(reportFile, report);
                }
            }

            if (isJson) {
                reportData.removeIf(report -> !report.startsWith("["));
                FileUtils.writeFile(reportFile, formatJsonReport(reportData));
            }

            if (isNdJson) {
                reportData.removeIf(report -> !report.startsWith("{\"meta\":"));
                FileUtils.writeFile(reportFile, formatNdJsonReport(reportData));
            }

            if (isXml) {
                reportData.removeIf(report -> !report.startsWith("<?xml"));
                FileUtils.writeFile(reportFile, formatXmlReport(reportData, mergeTestCaseName));
            }
        }
    }

    private String formatJsonReport(List<String> reports) {
        StringBuilder jsonBuilder = new StringBuilder("[");
        reports.forEach(data -> jsonBuilder.append(data, 1, data.length() - 1).append(","));
        jsonBuilder.deleteCharAt(jsonBuilder.lastIndexOf(","));
        jsonBuilder.append("]");
        return jsonBuilder.toString();
    }

    private String formatNdJsonReport(List<String> reports) {
        StringBuilder ndJsonBuilder = new StringBuilder("");
        reports.forEach(data -> ndJsonBuilder.append(data).append("\n"));
        return ndJsonBuilder.toString();
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
                    time = time + Double.parseDouble(node.getAttribute("time"));

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
}
