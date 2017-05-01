package courgette.runtime;

import courgette.runtime.utils.FileUtils;

import java.io.File;
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
        this.reports = reports;
    }

    public void createReport() {
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

            final Boolean isHtml = reportFile.contains(".html");

            if (isHtml) {
                createHtmlReportImagesFolder();
                processNewEmbeddedHtmlFiles(reports, reportData);
                removeExistingEmbeddedHtmlFiles();

                reportData.removeIf(report -> !report.contains("$(document)"));
            } else {
                reportData.removeIf(report -> report.contains("$(document)"));
            }

            if (reportFile.endsWith(".json")) {
                FileUtils.writeFile(reportFile, formatJsonReport(reportData));
            } else {
                FileUtils.writeFile(reportFile, reportData);
            }
        }
    }

    private String formatJsonReport(List<String> reports) {
        StringBuilder jsonBuilder = new StringBuilder("[");
        reports.forEach(data -> jsonBuilder.append(data.substring(1, data.length() - 1)).append(","));
        jsonBuilder.deleteCharAt(jsonBuilder.lastIndexOf(","));
        jsonBuilder.append("]");
        return jsonBuilder.toString();
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
