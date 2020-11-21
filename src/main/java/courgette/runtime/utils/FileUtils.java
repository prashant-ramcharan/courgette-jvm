package courgette.runtime.utils;

import courgette.runtime.CourgetteException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static courgette.runtime.CourgetteException.printExceptionStackTrace;

public final class FileUtils {

    public static String readFile(String file, Boolean deleteOnExit) {
        try {
            File fileToRead = new File(file);

            if (deleteOnExit) {
                fileToRead.deleteOnExit();
            }

            return Files.readString(Paths.get(file));

        } catch (IOException e) {
            printExceptionStackTrace(e);
        }
        return null;
    }

    public static void writeFile(String file, String contents) {
        try {
            Files.writeString(Paths.get(file), contents);
        } catch (IOException e) {
            printExceptionStackTrace(e);
        }
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
                Arrays.asList(directory.listFiles()).forEach(File::delete);
                directory.delete();
            }
        } catch (Exception ignored) {
            // no action is needed
        }
    }
}