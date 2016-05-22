/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Visual;

public class Screen extends Region {

  private static vType vClazz = vType.SCREEN;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());

  private int sNum = 0;

  public Screen() {
    clazz = vClazz;
    init(SX.getMonitor(SX.getMainMonitorID()));
  }

  public Screen(int n) {
    clazz = vClazz;
    sNum = n;
    init(SX.getMonitor(n));
  }

  public static Region all() {
    return new Region(SX.getAllMonitors());
  }
}
