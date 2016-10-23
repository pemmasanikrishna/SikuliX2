/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXElement;

import java.awt.Rectangle;
import java.awt.Point;

public class Region extends SXElement {

  private static eType eClazz = eType.REGION;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  public Region() {
    clazz = eClazz;
    init(0, 0, 0, 0);
  }

  public Region(int x, int y, int w, int h) {
    clazz = eClazz;
    init(x, y, w, h);
  }

  public Region(int x, int y, int wh) {
    clazz = eClazz;
    init(x, y, wh, wh);
  }

  public Region(int x, int y) {
    clazz = eClazz;
    int[] margin = getMargin();
    init(x - margin[0], y - margin[1], 2 * margin[0] , 2 * margin[1]);
  }

  public Region(int[] rect) {
    clazz = eClazz;
    init(rect);
  }

  public Region(int id) {
    Rectangle rect = SX.getMonitor(id);
    init(rect.x, rect.y, rect.width, rect.height);
  }

  public Region(Rectangle rect) {
    clazz = eClazz;
    init(rect);
  }

  public Region(Point p) {
    clazz = eClazz;
    init(p);
  }

  public Region(SXElement elem) {
    clazz = eClazz;
    init(elem);
  }
}
