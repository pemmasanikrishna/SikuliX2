/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXElement;
import com.sikulix.core.SXLog;

import java.awt.*;

public class Element extends SXElement {

  private static eType eClazz = eType.ELEMENT;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  public Element() {
    clazz = eClazz;
    init(0, 0, 0, 0);
  }

  public Element(int x, int y, int w, int h) {
    clazz = eClazz;
    init(x, y, w, h);
  }

  public Element(int x, int y, int wh) {
    clazz = eClazz;
    init(x, y, wh, wh);
  }

  public Element(int x, int y) {
    clazz = eClazz;
    init(x, y, 0, 0);
  }

  public Element(int[] rect) {
    clazz = eClazz;
    init(rect);
  }

  public Element(int id) {
    Rectangle rect = SX.getMonitor(id);
    init(rect.x, rect.y, rect.width, rect.height);
  }

  public Element(Rectangle rect) {
    clazz = eClazz;
    init(rect);
  }

  public Element(Point p) {
    clazz = eClazz;
    init(p);
  }

  public Element(SXElement elem) {
    clazz = eClazz;
    init(elem);
  }
}
