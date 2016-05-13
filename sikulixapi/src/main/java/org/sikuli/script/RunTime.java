/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package org.sikuli.script;

import com.sikulix.core.SX;

public class RunTime extends SX {

  private static RunTime runTime = null;

  private RunTime(String className) {
  }

  public static synchronized RunTime getRunTime() {
    if (runTime == null) {
      runTime = new RunTime("RunTime");
    }
    return runTime;
  }
}
