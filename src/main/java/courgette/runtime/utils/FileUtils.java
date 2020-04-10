package courgette.runtime.utils;

import courgette.runtime.CourgetteException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
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

    public static void writeImageFile(String file, String format, String base64Image) {
        try {
            byte[] imageByte = Base64.getDecoder().decode(base64Image);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageByte);
            BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);

            ImageIO.write(bufferedImage, format, new File(file + "." + format));
        } catch (IOException ignored) {
        }
    }

    public static void readAndWriteFile(InputStream inputStream, String writePath) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder lines = new StringBuilder();
        reader.lines().forEach(line -> lines.append(line).append("\n"));

        writeFile(writePath, lines.toString());
    }

    public static List<File> getParentFiles(String path) {
        return Arrays.asList(new File(path).getParentFile().listFiles());
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

    public static Path copyClassPathFilesToTempDirectory() {
        Path tmpDir = createTempDirectory();
        if (tmpDir == null) {
            return null;
        }
        final URL[] classPathUrls = ((URLClassLoader) (Thread.currentThread().getContextClassLoader())).getURLs();
        Arrays.asList(classPathUrls).forEach(classPathUrl -> {
            try {
                Path source = createPath(classPathUrl);
                Path target = createPath(tmpDir.toFile().getAbsoluteFile() + File.separator + source.getFileName());
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Error copying file. Reason: " + e.getLocalizedMessage());
            }
        });
        return tmpDir;
    }

    private static Path createPath(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new CourgetteException("Unable to get file path. Reason: " + e.getLocalizedMessage());
        }
    }

    private static Path createPath(String uri) {
        URI fileUri = new File(uri).toURI();
        return Paths.get(fileUri);
    }

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory(null);
        } catch (IOException e) {
            System.err.println("Unable to create temp directory. Reason: " + e.getMessage());
        }
        return null;
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