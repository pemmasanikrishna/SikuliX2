/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Element;

import java.awt.Rectangle;
import java.awt.Point;

public class Region extends Element {

  private static eType vClazz = eType.REGION;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());

  public Region() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Region(int x, int y, int w, int h) {
    clazz = vClazz;
    init(x, y, w, h);
  }

  public Region(int x, int y, int wh) {
    clazz = vClazz;
    init(x, y, wh, wh);
  }

  public Region(int x, int y) {
    clazz = vClazz;
    int[] margin = getMargin();
    init(x - margin[0], y - margin[1], 2 * margin[0] , 2 * margin[1]);
  }

  public Region(int[] rect) {
    clazz = vClazz;
    init(rect);
  }

  public Region(int id) {
    Rectangle rect = SX.getMonitor(id);
    init(rect.x, rect.y, rect.width, rect.height);
  }

  public Region(Rectangle rect) {
    clazz = vClazz;
    init(rect);
  }

  public Region(Point p) {
    clazz = vClazz;
    init(p);
  }

  public Region(Element vis) {
    clazz = vClazz;
    init(vis);
  }
}
