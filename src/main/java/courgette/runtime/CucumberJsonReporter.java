package courgette.runtime;

import courgette.runtime.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;

final class CucumberJsonReporter {

    private FileWriter fileWriter;
    private int reportSize;
    private final Map<String, String> errors = new HashMap<>();

    public CucumberJsonReporter(String fileName, int reportSize) {
        try {
            FileUtils.createFile(new File(fileName));
            this.fileWriter = FileUtils.createFileWriter(fileName);
            this.fileWriter.write("[");
            this.reportSize = reportSize;
        } catch (IOException e) {
            errors.put(UUID.randomUUID().toString(), e.getMessage());
            printExceptionStackTrace(e);
        }
    }

    public void readAndWriteReport(String reportFile) {
        try {
            String report = FileUtils.readFile(reportFile, false);
            if (report == null) {
                throw new IOException(String.format("%s does not exist in the file system", reportFile));
            }

            fileWriter.write(report.substring(1, report.length() - 1));

            this.reportSize--;

            if (reportSize > 0) {
                fileWriter.write(",");
            } else {
                fileWriter.write("]");
                fileWriter.close();
            }
        } catch (IOException e) {
            errors.put(UUID.randomUUID().toString(), e.getMessage());
            printExceptionStackTrace(e);
        }
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
