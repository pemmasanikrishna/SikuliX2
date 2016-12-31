/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXElement;
import com.sikulix.core.Device;

public class Screen extends Element {

  private static eType eClazz = eType.SCREEN;
  private static SXLog log = SX.getLogger("SX.Screen");

  private int sNum = 0;

  public Screen() {
    init(SX.getMonitor());
  }

  public Screen(int id) {
    sNum = id;
    init(SX.getMonitor(id));
  }

  public static Element all() {
    return new Element(SX.getAllMonitors());
  }
}

