/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

/**
 * INTERNAL USE
 */
class SikuliException extends Exception {

  protected String _name = "SikuliException";

  public SikuliException(String msg) {
    super(msg);
  }

  @Override
  public String toString() {
    String ret = _name + ": " + getMessage() + "\n";
    for (StackTraceElement elm : getStackTrace()) {
      ret += "  Line " + elm.getLineNumber()
              + ", in file " + elm.getFileName() + "\n";
      return ret;
    }
    ret += "Line ?, in File ?";
    return ret;
  }
}
