/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Visual;

public class Screen extends Visual {

  private static vType vClazz = vType.SCREEN;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());
  //clazz = vClazz;

  private int sNum = 0;

  public Screen() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Screen(int n) {
    clazz = vClazz;
    sNum = n;
    init(0, 0, 0, 0);
  }
}
