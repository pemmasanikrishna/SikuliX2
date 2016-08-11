/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.scripting;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Runner {

  private static SXLog log = SX.getLogger("SX.Runner");

  public static Object run(String type, Object... args) {
    log.terminate(1, "run: %s not implemented", type);
    return null;
  }

  public static Object runjs(Object... args) {
    return run("js", args);
  }
}
