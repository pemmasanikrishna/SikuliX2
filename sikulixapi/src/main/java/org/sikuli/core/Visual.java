/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import java.awt.*;

public abstract class Visual extends SX {

  public int x = 0;
  public int y = 0;
  public int w = -1;
  public int h = -1;

  protected static int stdW = 50;
  protected static int stdH = 50;

  public Visual init(String cls) {
    setLogger(cls);
    x = 0; y = 0; w = -1; h = -1;
    return this;
  }

  public Visual init(String cls, int _x, int _y, int _w, int _h) {
    setLogger(cls);
    x = _x; y = _y; w = _w; h = _h;
    return this;
  }

  public Visual init(String cls, Rectangle rect) {
    setLogger(cls);
    x = rect.x; y = rect.y; w = rect.width; h = rect.height;
    return this;
  }

  public Visual vUnion(Visual vis) {
    return vis;
  }

  public boolean vContains(Visual vis) {
    return true;
  }
}
