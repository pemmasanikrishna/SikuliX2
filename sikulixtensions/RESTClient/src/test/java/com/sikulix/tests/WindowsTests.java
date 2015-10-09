package com.sikulix.tests;

import com.sikulix.entities.BaseTest;
import com.sikulix.restcore.entities.Command;
import com.sikulix.restcore.entities.Image;
import org.apache.commons.io.FileUtils;
import org.sikuli.script.Key;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Author: Sergey Kuts
 */
public class WindowsTests extends BaseTest {

    private static final File SERVER_PATH = new File(System.getenv("TMP") + "/server");
    private static final File CLIENT_PATH = new File(System.getenv("TMP") + "/client");
    private static final File BATCH_RUNNER_SCRIPT = getResource(RESOURCE_RUNNER_BATCH);
    private static final File BATCH_SAMPLE_SCRIPT = getResource(RESOURCE_SAMPLE_BATCH);
    private static final File IMAGE_BUTTON_START = getResource(RESOURCE_BUTTON_START_IMAGE);
    private static final File IMAGE_INPUT_FIND_FILES = getResource(RESOURCE_INPUT_FIND_FILES_IMAGE);
    private static final File IMAGE_INPUT_CMD = getResource(RESOURCE_INPUT_CMD_IMAGE);

    @Test(priority = 1)
    public void recreateFolders() throws IOException {
        if (getClient().exists(Arrays.asList(SERVER_PATH.getPath()))) {
            getClient().delete(SERVER_PATH.getPath());
        }

        if (CLIENT_PATH.exists()) {
            FileUtils.forceDelete(CLIENT_PATH);
        }

        assertFalse(getClient().exists(Arrays.asList(SERVER_PATH.getPath())));
        assertFalse(CLIENT_PATH.exists());

        getClient().createFolder(SERVER_PATH.getPath());
        FileUtils.forceMkdir(CLIENT_PATH);

        assertTrue(getClient().exists(Arrays.asList(SERVER_PATH.getPath())));
        assertTrue(CLIENT_PATH.exists());
    }

    @Test(priority = 2)
    public void uploadFileToServer() {
        getClient().uploadFile(Arrays.asList(BATCH_RUNNER_SCRIPT.getPath(), BATCH_SAMPLE_SCRIPT.getPath()), SERVER_PATH.getPath());
        assertTrue(getClient().exists(Arrays.asList(SERVER_PATH.getPath() + "/" + BATCH_RUNNER_SCRIPT.getName(),
                SERVER_PATH.getPath() + "/" + BATCH_SAMPLE_SCRIPT.getName())));
    }

    @Test(priority = 3)
    public void executeScript() {
        getClient().executeCommandLine(new Command(SERVER_PATH.getPath() + "/" + BATCH_RUNNER_SCRIPT.getName(),
                Arrays.asList("arg1", "arg2", "arg3"), WAIT_TIMEOUT));
        assertTrue(getClient().exists(Arrays.asList(SERVER_PATH.getPath() + "/" + EMPTY_FILE)));
    }

    @Test(priority = 4)
    public void downloadFileFromServer() {
        getClient().downloadFile(SERVER_PATH.getPath() + "/" + EMPTY_FILE, CLIENT_PATH.getPath());
        assertTrue(new File(CLIENT_PATH.getPath() + "/" + EMPTY_FILE).exists());
    }

    @Test(priority = 5)
    public void callCommandLineFromStartMenu() {
        getClient().uploadFile(Arrays.asList(IMAGE_BUTTON_START.getPath(), IMAGE_INPUT_CMD.getPath(), IMAGE_INPUT_FIND_FILES.getPath()), SERVER_PATH.getPath());
        getClient().click(new Image(SERVER_PATH.getPath() + "/" + IMAGE_BUTTON_START.getName(), SIMILARITY), WAIT_TIMEOUT);
        getClient().setText(new Image(SERVER_PATH.getPath() + "/" + IMAGE_INPUT_FIND_FILES.getName(), SIMILARITY),
                "cmd" + Key.ENTER, WAIT_TIMEOUT);
        assertTrue(getClient().exists(new Image(SERVER_PATH.getPath() + "/" + IMAGE_INPUT_CMD.getName(), SIMILARITY), WAIT_TIMEOUT));
    }
}
