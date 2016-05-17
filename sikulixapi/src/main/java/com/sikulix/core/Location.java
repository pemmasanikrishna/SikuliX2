/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import java.awt.Point;

public class Location extends Visual {

  private static vType vClazz = vType.LOCATION;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());

  public Location() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Location(int x, int y) {
    clazz = vClazz;
    init(x, y, 0, 0);
  }

  public Location(Visual vis) {
    clazz = vClazz;
    init(vis.x, vis.y, 0, 0);
  }

  public Location(Point p) {
    clazz = vClazz;
    init(p);
  }

  /**
   * to allow calculated x and y that might not be integers
   *
   * @param x column
   * @param y row
   *          truncated to the integer part
   */
  public Location(double x, double y) {
    clazz = vClazz;
    init((int) x, (int) y, 0, 0);
  }
}
