/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import java.awt.*;

public class Match extends Visual {

  private static vType vClazz = vType.MATCH;
  private static SXLog log = SX.getLogger(vClazz.toString());

  double mScore = 0;

  public Match() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Match(Rectangle rect) {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Match(Visual vis) {
    clazz = vClazz;
    init(vis);
  }

  public Match setScore(double score) {
    mScore = score;
    return this;
  }

  public Double getScore(double score) {
    return score;
  }
}
