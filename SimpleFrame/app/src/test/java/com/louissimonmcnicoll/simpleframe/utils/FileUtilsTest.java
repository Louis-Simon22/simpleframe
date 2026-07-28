package com.louissimonmcnicoll.simpleframe.utils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Regression tests for recursive scanning and removable-drive metadata filtering. */
public class FileUtilsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void scanIncludesSupportedImagesInNestedFolders() throws Exception {
        File root = temporaryFolder.newFolder("photos");
        File nested = new File(root, "trip/day-one");
        assertTrue(nested.mkdirs());
        assertTrue(new File(root, "cover.JPG").createNewFile());
        assertTrue(new File(nested, "photo.jpeg").createNewFile());
        assertTrue(new File(nested, "photo.webp").createNewFile());
        assertTrue(new File(nested, "notes.txt").createNewFile());
        assertTrue(new File(root, "._cover.JPG").createNewFile());

        List<String> paths = FileUtils.getFileList(root.getAbsolutePath(), false);

        assertEquals(3, paths.size());
        assertTrue(paths.get(0).endsWith("cover.JPG"));
        assertTrue(paths.stream().anyMatch(path -> path.endsWith("photo.jpeg")));
        assertTrue(paths.stream().anyMatch(path -> path.endsWith("photo.webp")));
    }

    @Test
    public void missingOrBlankFolderReturnsEmptyList() {
        assertTrue(FileUtils.getFileList("", false).isEmpty());
        assertTrue(FileUtils.getFileList("/folder/that/does/not/exist", false).isEmpty());
    }
}
