package com.sikulix.restcore.entities;

/**
 * Author: Sergey Kuts
 */
public class Image {

    private String path;
    private float similarity;

    public Image() {
    }

    public Image(final String path, final float similarity) {
        this.path = path;
        this.similarity = similarity;
    }

    public String getPath() {
        return path;
    }

    public float getSimilarity() {
        return similarity;
    }

    public String toString() {
        return "[image path = " + getPath() +
                "; similarity = " + getSimilarity() + "]";
    }
}
