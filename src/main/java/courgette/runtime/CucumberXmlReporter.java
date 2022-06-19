package courgette.runtime;

import courgette.integration.reportportal.ReportPortalProperties;
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
import java.util.List;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.FileUtils.writeFile;

final class CucumberXmlReporter {

    static void createReport(String reportFile, List<String> reports,
                             boolean mergeTestCaseName, boolean isReportPortalEnabled) {
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
                    tests = tests + parseTests(node);
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
                                printExceptionStackTrace(te);
                            }
                        }
                    }
                }
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            printExceptionStackTrace(e);
        }

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
