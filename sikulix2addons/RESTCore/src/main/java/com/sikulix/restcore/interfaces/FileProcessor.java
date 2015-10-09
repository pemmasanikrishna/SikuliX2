package com.sikulix.restcore.interfaces;

import java.util.List;

/**
 * Created by Serhii Kuts
 */
public interface FileProcessor {

    void uploadFile(final List<String> filesPath, final String saveToPath);

    void downloadFile(final String downloadFilePath, final String saveToPath);

    void createFolder(final String path);

    void copyFolder(final String copyFrom, final String copyTo);

    void cleanFolder(final String path);

    void delete(final String path);

    boolean exists(final List<String> paths);
}
