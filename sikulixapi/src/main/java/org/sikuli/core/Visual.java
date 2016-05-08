/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import java.awt.*;

public abstract class Visual extends SX {

  protected Visual(clazzType clazz) {
    setLogger(clazz.toString());
  }

  public int x = 0;
  public int y = 0;
  public int w = -1;
  public int h = -1;

  private static int stdW = 50;
  private static int stdH = 50;

  public static void setMargin(int wh) {
    setMargin(wh, wh);
  }

  public static void setMargin(int w, int h) {
    if (w  > 0) {
      stdW = w;
    }
    if (h  > 0) {
      stdH = h;
    }
  }

  public static int[] getMargin() {
    return new int[] {stdW, stdH};
  }

  public void init(String cls) {
    setLogger(cls);
    x = 0; y = 0; w = 0; h = 0;
  }

  public void init(int _x, int _y, int _w, int _h) {
    x = _x; y = _y;
    w = _w < 0 ? 0 : _w;
    h = _h < 0 ? 0 : _h;
  }

  public void init(Rectangle rect) {
    x = rect.x; y = rect.y; w = rect.width; h = rect.height;
  }

  public void init(Point p) {
    x = p.x; y = p.y; w = 0; h = 0;
  }

  public void init(Visual vis) {
    x = vis.x; y = vis.y; w = vis.w; h = vis.h;
  }

  public Visual vAt(int x, int y) {
    this.x = x;
    this.y = y;
    return this;
  }

  public Visual vTranslate(int xoff, int yoff) {
    this.x += xoff;
    this.y += yoff;
    return this;
  }

  public Rectangle getRectangle() {
    return new Rectangle(x, y, w, h);
  }

  public Visual vUnion(Visual vis) {
    return vis;
  }

  public boolean vContains(Visual vis) {
    return true;
  }
}
