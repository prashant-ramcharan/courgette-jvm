package courgette.runtime;

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
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CourgetteReporter {
    private final String reportFile;
    private final Map<String, CopyOnWriteArrayList<String>> reports;

    public CourgetteReporter(String reportFile, Map<String, CopyOnWriteArrayList<String>> reports) {
        this.reportFile = reportFile;

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

            final boolean isHtml = reportFile.endsWith("report.js");
            final boolean isJson = reportFile.endsWith(".json");
            final boolean isXml = reportFile.endsWith(".xml");

            if (isHtml) {
                createHtmlReportImagesFolder();
                processNewEmbeddedHtmlFiles(reports, reportData);
                removeExistingEmbeddedHtmlFiles();
                reportData.removeIf(report -> !report.startsWith("$(document)"));
                FileUtils.writeFile(reportFile, reportData);
            }

            if (isJson) {
                reportData.removeIf(report -> !report.startsWith("["));
                FileUtils.writeFile(reportFile, formatJsonReport(reportData));
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

    private String formatXmlReport(List<String> reports, boolean mergeTestCaseName) {
        int failures = 0;
        int skipped = 0;
        int tests = 0;
        double time = 0.0;

        final StringBuilder xmlBuilder = new StringBuilder();

        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        xmlBuilder.append("<testsuite failures=\"id:failures\" name=\"Test Suite\" skipped=\"id:skipped\" tests=\"id:tests\" time=\"id:time\">\n\n");

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
                                Node testName = testcase.getAttributes().getNamedItem("name");
                                String classNameValue = testcase.getAttributes().getNamedItem("classname").getNodeValue();
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

        return xmlBuilder.toString()
                .replace("id:failures", String.valueOf(failures))
                .replace("id:skipped", String.valueOf(skipped))
                .replace("id:tests", String.valueOf(tests))
                .replace("id:time", String.valueOf(time));
    }

    private void processNewEmbeddedHtmlFiles(Map<String, CopyOnWriteArrayList<String>> sortedReports, List<String> reportData) {
        Stream<Map.Entry<String, CopyOnWriteArrayList<String>>> reportsWithEmbeddedFiles = sortedReports.entrySet().stream().filter(r -> r.getKey().contains(".html"));

        final File targetDir = new File(reportFile).getParentFile();

        final String target = targetDir.getPath();
        final String targetImageFolder = "images";

        reportsWithEmbeddedFiles.forEach(report -> {
            final String uuid = UUID.randomUUID().toString().replace("-", "");

            try {
                String reportDetails = report.getValue().get(0);
                final Integer reportIndex = reportData.indexOf(reportDetails);
                String[] embeddedFiles = reportDetails.split("embedded");

                for (int index = 0; (index < embeddedFiles.length - 1); index++) {

                    final List<File> reportFiles = FileUtils
                            .getParentFiles(report.getKey())
                            .stream().filter(name -> name.getName().startsWith("embedded"))
                            .collect(Collectors.toList());

                    final File source = reportFiles.get(index);

                    final String fileExtension = source.getName().substring(source.getName().lastIndexOf(".") + 1);
                    final String embeddedImageFilename = String.format("%s/%s%s.%s", targetImageFolder, uuid, index, fileExtension);

                    final File destination = new File(String.format("%s/%s", target, embeddedImageFilename));

                    Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    reportData.set(reportIndex, reportDetails.replace("embedded", String.format("%s/%s", targetImageFolder, uuid)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void createHtmlReportImagesFolder() {
        File reportTargetChild = new File(reportFile);
        File reportTargetParent = reportTargetChild.getParentFile();

        File imageFolder = new File(reportTargetParent.getPath() + "/images");
        if (!imageFolder.exists()) {
            imageFolder.mkdir();
        }
    }

    private void removeExistingEmbeddedHtmlFiles() {
        File reportTargetChild = new File(reportFile);
        File reportTargetParent = reportTargetChild.getParentFile();

        List<File> embeddedFiles = new ArrayList<>();

        embeddedFiles.addAll(
                Arrays.stream(reportTargetParent.listFiles())
                        .filter(file -> file.getName().startsWith("embedded"))
                        .collect(Collectors.toList()));

        embeddedFiles.forEach(File::delete);
    }
}
