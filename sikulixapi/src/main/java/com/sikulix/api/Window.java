/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Visual;

public class Window extends Region {

  private static Visual.vType vClazz = Visual.vType.WINDOW;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());
  //clazz = vClazz;

  public Window() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }
}
