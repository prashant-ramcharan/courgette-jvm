package courgette.runtime;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;

final class CucumberJsonReporter {

    static void createReport(String fileName, List<String> reports) {

        try {
            final FileWriter fileWriter = new FileWriter(fileName, false);

            fileWriter.write("[");

            Iterator<String> reportIterator = reports.iterator();

            while (reportIterator.hasNext()) {
                String report = reportIterator.next();

                fileWriter.write(report.substring(1, report.length() - 1));

                if (reportIterator.hasNext()) {
                    fileWriter.write(",");
                }
            }

            fileWriter.write("]");

            fileWriter.close();

        } catch (IOException e) {
            printExceptionStackTrace(e);
        }
    }
}
