/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Visual;

import java.awt.*;

public class Offset extends Visual {

  private static vType vClazz = vType.OFFSET;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());

  public Offset() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Offset(int x, int y) {
    this();
    init(x, y, 0, 0);
  }

  public Offset(Object vis) {
    this();
    if (!(vis instanceof Visual)) {
      log.error("Offset: not a Visual: %s", vis);
    } else {
      clazz = vClazz;
      init((Visual) vis);
    }
  }
}
