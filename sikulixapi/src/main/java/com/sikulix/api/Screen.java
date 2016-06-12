/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.Device;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Visual;

public class Screen extends Device {

  private static SXLog log = SX.getLogger("SX.Screen");

  private int sNum = 0;

  public Screen() {
    init(SX.getMainMonitorID());
  }

  public Screen(int id) {
    sNum = id;
    init(id);
  }

  private void init(int id) {

  }

  public static Region all() {
    return new Region(SX.getAllMonitors());
  }
}
