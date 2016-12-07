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
  private Element screenAsElement = null;

  public Screen() {
    init(SX.getMainMonitorID());
  }

  public Screen(int id) {
    sNum = id;
    init(id);
  }

  private void init(int id) {
    screenAsElement = new Element(SX.getMonitor(sNum));
  }

  public static Element all() {
    return new Element(SX.getAllMonitors());
  }

  public Element asElement() {
    return screenAsElement;
  }

  public static Element asElement(int id) {
    return new Screen(id).asElement();
  }
}

