/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import java.awt.Rectangle;
import java.awt.Point;

public class Region extends Visual{

  static {
    clazz = clazzType.REGION;
  }

  public Region() {
    super(clazzType.REGION);
    init(0, 0, 0, 0);
  }

  public Region(int x, int y, int w, int h) {
    super(clazzType.REGION);
    init(x, y, w, h);
  }

  public Region(int x, int y, int wh) {
    super(clazzType.REGION);
    init(x, y, wh, wh);
  }

  public Region(int x, int y) {
    super(clazzType.REGION);
    int[] margin = getMargin();
    init(x - margin[0], y - margin[1], 2 * margin[0] , 2 * margin[1]);
  }

  public Region(Rectangle rect) {
    super(clazzType.REGION);
    init(rect);
  }

  public Region(Point p) {
    super(clazzType.REGION);
    init(p);
  }

  public Region(Visual vis) {
    super(clazzType.REGION);
    init(vis);
  }

  public Region at(int x, int y) {
    return (Region) vAt(x, y);
  }

  public Region translate(int xoff, int yoff) {
    return (Region) vTranslate(xoff, yoff);
  }

  public Region union(Visual vis) {
    return (Region) this.vUnion(vis);
  }

  public boolean contains(Visual vis) {
    return this.vContains(vis);
  }
}
