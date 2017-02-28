/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Window extends Element {

  private static eType eClazz = eType.WINDOW;
  public eType getType() {
    return eClazz;
  }

  private static SXLog log = SX.getLogger("SX." + eClazz.toString());


  public Window() {
  }
}
