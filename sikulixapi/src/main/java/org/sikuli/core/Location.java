/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import java.awt.Point;

public class Location extends Visual {

  public Location() {
    super(clazzType.LOCATION);
    init(0, 0, 0, 0);
  }

  public Location(int x, int y) {
    super(clazzType.LOCATION);
    init(x, y, 0, 0);
  }

  public Location(Point p) {
    super(clazzType.LOCATION);
    init(p);
  }

  public Location at(int x, int y) {
    return (Location) vAt(x, y);
  }

  public Location translate(int xoff, int yoff) {
    return (Location) vTranslate(xoff, yoff);
  }

  public Location union(Visual vis) {
    return (Location) this.vUnion(vis);
  }

  public boolean contains(Visual vis) {
    return this.vContains(vis);
  }

  public Region asRegion() {
    return new Region(x, y);
  }

  public Region asRegion(int w, int h) {
    Region reg = new Region(x, y, w, h);
    w = w / 2;
    h = h / 2;
    return (Region) reg.at(x - w, y - h);
  }
}
