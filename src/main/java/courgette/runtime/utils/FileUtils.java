package courgette.runtime.utils;

import courgette.runtime.CourgetteException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FileUtils {

    public static String readFile(String file, Boolean deleteOnExit) {
        return fileReader.apply(file, deleteOnExit);
    }

    public static void writeFile(String file, Collection<?> contents) {
        fileCollectionWriter.accept(file, contents);
    }

    public static void writeFile(String file, String contents) {
        fileStringWriter.accept(file, contents);
    }

    public static List<File> getParentFiles(String path) {
        return Arrays.asList(new File(path).getParentFile().listFiles());
    }

    public static File getTempFile() {
        try {
            return Files.createTempFile(UUID.randomUUID().toString(), ".tmp").toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File getClassPathFile(String path) {
        URL classPathResource = Thread.currentThread().getContextClassLoader().getResource(path);

        if (classPathResource != null) {
            return new File(classPathResource.getFile());
        }
        return null;
    }

    public static File zipFile(String filePath, boolean removeFileExtension) {
        try {
            File file = new File(filePath);
            String zipFilename = filePath + ".zip";

            if (removeFileExtension) {
                zipFilename = filePath.substring(0, filePath.lastIndexOf(".")) + ".zip";
            }

            FileOutputStream fileOutputStream = new FileOutputStream(zipFilename);
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));

            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            zipOutputStream.write(bytes, 0, bytes.length);
            zipOutputStream.closeEntry();
            zipOutputStream.close();
            return new File(zipFilename);

        } catch (FileNotFoundException ex) {
            throw new CourgetteException(String.format("The file %s does not exist", filePath));
        } catch (IOException ex) {
            throw new CourgetteException("Unable to zip file: " + ex);
        }
    }

    public static void deleteDirectorySilently(String dir) {
        try {
            File directory = new File(dir);
            if (directory.exists()) {
                Arrays.asList(directory.listFiles()).forEach(f -> {
                    if (f.isDirectory()) {
                        deleteDirectorySilently(f.getPath());
                    } else {
                        f.delete();
                    }
                });
                directory.delete();
            }
        } catch (Exception ignored) {
            // no action is needed
        }
    }

    private static BiFunction<String, Boolean, String> fileReader = (file, deleteOnExit) -> {
        if (file == null) {
            return null;
        }

        File rerunFile = new File(file);

        if (deleteOnExit) {
            rerunFile.deleteOnExit();
        }

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(rerunFile);
            bufferedReader = new BufferedReader(fileReader);
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        } catch (Exception ignored) {
            return null;
        } finally {
            try {
                if (fileReader != null)
                    fileReader.close();

                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (Exception ignored) {
            }
        }
    };

    private static BiConsumer<String, String> fileStringWriter = (file, source) -> {
        if (file == null) {
            return;
        }

        File newFile = new File(file);
        try {
            FileWriter fileWriter = new FileWriter(newFile, Boolean.FALSE);
            fileWriter.write(source);
            fileWriter.close();
        } catch (Exception ignored) {
        }
    };

    private static BiConsumer<String, Collection<?>> fileCollectionWriter = (file, source) -> {
        if (file == null) {
            return;
        }

        File newFile = new File(file);
        try {
            FileWriter fileWriter = new FileWriter(newFile, Boolean.FALSE);

            final StringBuilder contents = new StringBuilder();
            source.forEach(data -> contents.append(data).append("\n"));
            fileWriter.write(contents.toString());

            fileWriter.close();
        } catch (Exception ignored) {
        }
    };
}
