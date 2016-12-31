/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXElement;
import com.sikulix.core.SXLog;

public class Window extends Element {

  private static eType vClazz = eType.WINDOW;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());
  //clazz = vClazz;

  public Window() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }
}
