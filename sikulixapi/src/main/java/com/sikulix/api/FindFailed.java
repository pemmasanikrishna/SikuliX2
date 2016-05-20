/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

/**
 * implements the SikuliX FindFailed exception class
 * and defines constants and settings for the feature FindFailedResponse
 */
public class FindFailed extends Exception {

  public static enum Response {
    ABORT, PROMPT, RETRY, SKIP, HANDLE
  }

  static Response defaultResponse = Response.ABORT;

  /***
   * @param message to be shown
   */
  public FindFailed(String message) {
  }

  @Override
  public String toString() {
    String ret = "FindFailed: " + getMessage() + "\n";
    for (StackTraceElement elm : getStackTrace()) {
      ret += "  Line " + elm.getLineNumber()
              + ", in file " + elm.getFileName() + "\n";
      return ret;
    }
    ret += "Line ?, in File ?";
    return ret;
  }
}
