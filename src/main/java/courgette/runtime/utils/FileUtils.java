package courgette.runtime.utils;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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

    public static void readAndWriteFile(InputStream inputStream, String writePath) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder lines = new StringBuilder();
        reader.lines().forEach(line -> lines.append(line).append("\n"));

        writeFile(writePath, lines.toString());
    }

    public static List<File> getParentFiles(String path) {
        return Arrays.asList(new File(path).getParentFile().listFiles());
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

            StringBuilder fileContents = new StringBuilder();
            bufferedReader.lines().forEach(fileContents::append);
            return fileContents.toString();
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
