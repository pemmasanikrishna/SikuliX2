/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.core;

import java.awt.*;

public class Region extends Visual{
  private static String logStamp = "Region";

  public Region() { init(logStamp); }

  public Region(int x, int y, int w, int h) {
    init(logStamp, x, y, w, h);
  }

  public Region(int x, int y, int wh) {
    init(logStamp, x, y, wh, wh);
  }

  public Region(int x, int y) {
    init(logStamp, x - stdW, y - stdH, 2 * stdW , 2 * stdH);
  }

  public Region(Rectangle rect) {
    init(logStamp, rect);
  }

  public Region union(Visual vis) {
    return (Region) this.vUnion(vis);
  }

  public boolean contains(Visual vis) {
    return this.vContains(vis);
  }

  public Rectangle getRectangle() {
    return new Rectangle(x, y, w, h);
  }
}
