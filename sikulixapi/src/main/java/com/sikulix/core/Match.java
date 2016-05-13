/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import java.awt.*;

public class Match extends Visual {

  private static vType vClazz = vType.MATCH;
  private static SXLog log = SX.getLogger(vClazz.toString());

  //<editor-fold desc="score">
  double score = 0;

  public Match setScore(double score) {
    this.score = score;
    return this;
  }

  public Double getScore() {
    return score;
  }
  //</editor-fold>

  public Match() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Match(Rectangle rect) {
    clazz = vClazz;
    init(rect);
  }

  public Match(Visual vis) {
    clazz = vClazz;
    init(vis);
    if (vis.isMatch()) {
      setScore(((Match) vis).getScore());
      setTarget(vis.getTarget());
    }
  }

  protected Match(Region reg, Double score, Offset off) {
    clazz = vClazz;
    init(reg);
    setScore(score);
    setTarget(off);
  }
}
