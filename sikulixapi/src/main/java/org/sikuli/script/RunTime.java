/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */
package org.sikuli.script;

import org.sikuli.core.SX;

public class RunTime extends SX {

  private static RunTime runTime = null;

  private RunTime() {
  }

  /**
   * get the initialized RunTime singleton instance
   *
   * @return
   */
  public static synchronized RunTime get(Object... args) {
    if (runTime == null) {
      return getRunTime();
    }
    return runTime;
  }

  private static synchronized RunTime getRunTime() {
    if (runTime == null) {
      runTime = new RunTime();
    }
    return runTime;
  }
}
