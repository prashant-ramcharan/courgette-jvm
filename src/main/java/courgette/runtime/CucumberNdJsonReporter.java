package courgette.runtime;

import courgette.runtime.utils.FileUtils;
import io.cucumber.messages.types.Envelope;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;
import static courgette.runtime.utils.JacksonUtils.CUCUMBER_OBJECT_MAPPER;

final class CucumberNdJsonReporter {
    private final FileWriter fileWriter;
    private int reportSize;
    private final Map<String, String> errors = new HashMap<>();

    public CucumberNdJsonReporter(String fileName, int reportSize) {
        FileUtils.createFile(fileName);
        this.fileWriter = FileUtils.createFileWriter(fileName);
        this.reportSize = reportSize;
    }

    public Map<String, List<List<Envelope>>> readReports(Map.Entry<String, List<CourgetteReportOptions>> reports) {
        Map<String, List<List<Envelope>>> messages = new HashMap<>();

        reports.getValue().stream().map(CourgetteReportOptions::getNdJsonFile).forEach(reportFile -> {
                    try {
                        String report = FileUtils.readFile(reportFile, false);
                        if (report == null) {
                            throw new IOException(String.format("%s does not exist in the file system", reportFile));
                        }

                        List<Envelope> reportMessages = CourgetteNdJsonCreator.createMessages(report);
                        messages.computeIfAbsent(reports.getKey(), r -> new ArrayList<>()).add(reportMessages);
                    } catch (Exception e) {
                        errors.put(UUID.randomUUID().toString(), e.getMessage());
                        printExceptionStackTrace(e);
                    }
                }
        );

        return messages;
    }

    public void writeReport(List<Envelope> messages) {
        try {
            for (Envelope message : messages) {
                fileWriter.write(CUCUMBER_OBJECT_MAPPER.writeValueAsString(message));
                fileWriter.write("\n");
            }

            reportSize--;

            if (reportSize == 0) {
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
