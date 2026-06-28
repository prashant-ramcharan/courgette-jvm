package courgette.runtime.utils;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileUtilsTest {
    @Test
    public void readsAndWritesStringFiles() throws Exception {
        File file = File.createTempFile("courgette", ".txt");

        FileUtils.writeFile(file.getPath(), "one\ntwo");

        assertEquals("one\ntwo", FileUtils.readFile(file.getPath(), false));
    }

    @Test
    public void readsAndWritesCollectionFiles() throws Exception {
        File file = File.createTempFile("courgette", ".txt");

        FileUtils.writeFile(file.getPath(), Arrays.asList("one", "two"));

        assertEquals("one\ntwo", FileUtils.readFile(file.getPath(), false).trim());
    }

    @Test
    public void readsInputStreams() {
        ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));

        assertEquals("hello\n", FileUtils.readFile(input));
    }

    @Test
    public void createsDirectoriesAndFiles() throws Exception {
        File dir = Files.createTempDirectory("courgette").resolve("nested").toFile();
        File file = new File(dir, "file.txt");

        assertTrue(FileUtils.createDirectory(dir));
        assertTrue(FileUtils.createFile(file));
        assertTrue(FileUtils.fileExists(file.getPath()));
    }

    @Test
    public void returnsNullForMissingFiles() {
        assertNull(FileUtils.readFile("/path/that/does/not/exist", false));
        assertNull(FileUtils.getClassPathFile("missing-resource"));
    }

    @Test
    public void extractsFileNamesAndFormatsPaths() {
        assertEquals("example.feature", FileUtils.getFileName(URI.create("file:/tmp/example.feature")));
        assertTrue(FileUtils.formatFilePath("target").endsWith(File.separator));
    }

    @Test
    public void zipsFiles() throws Exception {
        File file = File.createTempFile("courgette", ".txt");
        FileUtils.writeFile(file.getPath(), "content");

        File zip = FileUtils.zipFile(file.getPath(), true);

        assertNotNull(zip);
        assertTrue(zip.exists());
        try (ZipFile zipFile = new ZipFile(zip)) {
            assertNotNull(zipFile.getEntry(file.getName()));
        }
    }
}
