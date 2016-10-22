/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Element;

public class Offset extends Element {

  private static eType vClazz = eType.OFFSET;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());

  public Offset() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Offset(int x, int y) {
    this();
    init(x, y, 0, 0);
  }

  public Offset(Element vis) {
    this();
    init(vis.getCenter());
  }

  public Offset(Element visFrom, Element visTo) {
    this();
    init(visTo.getCenter().x - visFrom.getCenter().x, visTo.getCenter().y - visFrom.getCenter().y, 0, 0);
  }
}
