/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Window extends Element {

  private static eType eClazz = eType.WINDOW;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  protected void initAfter() {
    initName(eClazz);
  }

  public Window() {
    clazz = eClazz;
    init(0, 0, 0, 0);
  }
}
