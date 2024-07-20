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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.FileUtils.writeFile;

final class CucumberXmlReporter {

    private final String reportFile;
    private final StringBuilder xmlBuilder = new StringBuilder();
    private int reportSize;
    private int failures = 0;
    private int skipped = 0;
    private int tests = 0;
    private double time = 0.0;
    private String testSuite = "Test Suite";
    private final Map<String, String> errors = new HashMap<>();

    public CucumberXmlReporter(String reportFile, int reportSize) {
        this.reportFile = reportFile;
        this.reportSize = reportSize;

        this.xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        this.xmlBuilder.append("<testsuite failures=\"id:failures\" name=\"id:testSuite\" skipped=\"id:skipped\" tests=\"id:tests\" time=\"id:time\">\n\n");
    }

    public void readAndWriteReport(String fileName,
                                   boolean mergeTestCaseName, boolean isReportPortalEnabled) {

        final HashMap<String, Integer> testcaseIteration = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            String report = FileUtils.readFile(fileName, false);
            if (report == null) {
                throw new IOException(String.format("%s does not exist in the file system", fileName));
            }

            Document document = builder.parse(new InputSource(new StringReader(report)));

            if (document != null) {
                Element node = document.getDocumentElement();

                failures = failures + Integer.parseInt(node.getAttribute("failures"));
                skipped = skipped + Integer.parseInt(node.getAttribute("skipped"));
                tests = tests + parseTests(node);
                time = time + parseTime(node.getAttribute("time"));

                NodeList testCases = document.getElementsByTagName("testcase");

                if (testCases != null) {
                    for (int i = 0; i < testCases.getLength(); i++) {
                        Node testcase = testCases.item(i);
                        Node testClassName = testcase.getAttributes().getNamedItem("classname");
                        Node testName = testcase.getAttributes().getNamedItem("name");

                        String key = testClassName.getNodeValue() + "-" + testName.getNodeValue();
                        testcaseIteration.merge(key, 1, Integer::sum);

                        if (testcaseIteration.get(key) > 1) {
                            testName.setNodeValue(testName.getNodeValue() + " " + testcaseIteration.get(key));
                        }

                        if (mergeTestCaseName) {
                            testName.setNodeValue(testClassName.getNodeValue() + ": " + testName.getNodeValue());
                        }

                        StringWriter sw = new StringWriter();
                        try {
                            Transformer t = TransformerFactory.newInstance().newTransformer();
                            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                            t.setOutputProperty(OutputKeys.INDENT, "yes");
                            t.transform(new DOMSource(testcase), new StreamResult(sw));

                            xmlBuilder.append(sw).append("\n");

                        } catch (TransformerException te) {
                            errors.put(UUID.randomUUID().toString(), te.getMessage());
                            printExceptionStackTrace(te);
                        }
                    }
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            errors.put(UUID.randomUUID().toString(), e.getMessage());
            printExceptionStackTrace(e);
        }

        reportSize--;

        if (reportSize == 0) {
            xmlBuilder.append("</testsuite>");

            if (isReportPortalEnabled) {
                testSuite = ReportPortalProperties.getInstance().getTestSuite();
            }

            final String xmlReport = xmlBuilder.toString()
                    .replace("id:failures", String.valueOf(failures))
                    .replace("id:skipped", String.valueOf(skipped))
                    .replace("id:tests", String.valueOf(tests))
                    .replace("id:time", String.valueOf(time))
                    .replace("id:testSuite", testSuite);

            writeFile(reportFile, xmlReport);
        }
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    private static double parseTime(String time) {
        final NumberFormat numberFormat = NumberFormat.getInstance();
        Number timeValue;
        try {
            timeValue = numberFormat.parse(time);
        } catch (ParseException e) {
            timeValue = Double.parseDouble(time.replaceAll(",", ""));
        }
        return timeValue.doubleValue();
    }

    private static int parseTests(Element node) {
        String tests = node.getAttribute("tests");
        return tests.equals("") ? 0 : Integer.parseInt(tests);
    }
}
