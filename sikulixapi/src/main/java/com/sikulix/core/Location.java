/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import java.awt.Point;

public class Location extends Visual {

  private static vType vClazz = vType.LOCATION;
  private static SXLog log = SX.getLogger(vClazz.toString());

  public Location() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Location(int x, int y) {
    clazz = vClazz;
    init(x, y, 0, 0);
  }

  public Location(Point p) {
    clazz = vClazz;
    init(p);
  }
}
