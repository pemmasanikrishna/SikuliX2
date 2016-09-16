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

  public Offset(Visual vis) {
    this();
    init(vis.getCenter());
  }

  public Offset(Visual visFrom, Visual visTo) {
    this();
    init(visTo.getCenter().x - visFrom.getCenter().x, visTo.getCenter().y - visFrom.getCenter().y, 0, 0);
  }
}
