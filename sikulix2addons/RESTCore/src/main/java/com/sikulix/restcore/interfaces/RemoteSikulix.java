package com.sikulix.restcore.interfaces;

import com.sikulix.restcore.entities.Image;

import java.util.List;

/**
 * Created by Serhii Kuts
 */
public interface RemoteSikulix extends CommandLineExecutor, FileProcessor {

    void click(final Image image, final int timeout);

    void setText(final Image image, final String text, final int timeout);

    boolean exists(final Image image, final int timeout);

    void dragAndDrop(final List<Image> images, final int timeout);

    void close();
}
