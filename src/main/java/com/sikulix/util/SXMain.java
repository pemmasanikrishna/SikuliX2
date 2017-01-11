/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.util;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class SXMain {

  static SXLog log = SX.getLogger("TestCoreBasic");

  public static void main(String[] args) {
    log.trace("main: start: %s", "parameter");
  }
}
