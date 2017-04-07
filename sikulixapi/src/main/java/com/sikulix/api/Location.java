/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXElement;

import java.awt.Point;

public class Location extends SXElement {

  private static eType eClazz = eType.LOCATION;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  public Location() {
    clazz = eClazz;
    init(0, 0, 0, 0);
  }

  public Location(int x, int y) {
    clazz = eClazz;
    init(x, y, 0, 0);
  }

  public Location(SXElement elem) {
    clazz = eClazz;
    init(elem.getCenter());
  }

  public Location(Point p) {
    clazz = eClazz;
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
    clazz = eClazz;
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
