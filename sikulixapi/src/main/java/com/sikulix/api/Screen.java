/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXElement;
import com.sikulix.core.Device;

public class Screen extends Device {

  private static SXLog log = SX.getLogger("SX.Screen");

  private int sNum = 0;
  private SXElement visScreen = null;

  public Screen() {
    init(SX.getMainMonitorID());
  }

  public Screen(int id) {
    sNum = id;
    init(id);
  }

  private void init(int id) {
    visScreen = new Region(SX.getMonitor(sNum));
  }

  public static Region all() {
    return new Region(SX.getAllMonitors());
  }

  public Region asRegion() {
    return (Region) visScreen;
  }

  public static Region asRegion(int id) {
    return new Screen(id).asRegion();
  }
}
