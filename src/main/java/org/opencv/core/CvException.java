/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package org.opencv.core;

public class CvException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CvException(String msg) {
        super(msg);
    }

    @Override
    public String toString() {
        return "CvException [" + super.toString() + "]";
    }
}
