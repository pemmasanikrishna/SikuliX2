/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXElement;

import java.awt.*;

public class Match extends Region {

  private static eType eClazz = eType.MATCH;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  private int index = 0;

  public Match() {
    clazz = eClazz;
    init(0, 0, 0, 0);
  }

  public Match(Rectangle rect) {
    clazz = eClazz;
    init(rect);
  }

  public Match(SXElement elem) {
    clazz = eClazz;
    init(elem);
    if (elem.isMatch()) {
      setScore(((Match) elem).getScore());
      setOffset(elem.getOffset());
    }
  }

  public Match(SXElement elem, Double score, Offset off) {
    clazz = eClazz;
    init(elem);
    setScore(score);
    setOffset(off);
  }
}
