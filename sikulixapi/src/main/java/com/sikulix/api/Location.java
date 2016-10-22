/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Element;

import java.awt.Point;

public class Location extends Element {

  private static eType vClazz = eType.LOCATION;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());

  public Location() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Location(int x, int y) {
    clazz = vClazz;
    init(x, y, 0, 0);
  }

  public Location(Element vis) {
    clazz = vClazz;
    init(vis.getCenter());
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

  /**
   * the offset of given point to this Location
   *
   * @param loc the other Location
   * @return relative offset
   */
  public Location getOffset(Location loc) {
    return new Location(loc.x - x, loc.y - y);
  }

  /**
   * convenience: like awt point
   *
   * @param X new x
   * @param Y new y
   * @return the location itself modified
   */
  public Location move(int X, int Y) {
    at(X, Y);
    return this;
  }
}
