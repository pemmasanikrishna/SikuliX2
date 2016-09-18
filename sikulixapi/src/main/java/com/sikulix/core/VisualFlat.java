/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.Match;
import com.sikulix.api.Offset;
import com.sikulix.api.Region;

public class VisualFlat {

  int x = 0;
  int y = 0;
  int w = 0;
  int h = 0;

  VisualFlat lastMatch = null;
  double score = 0;

  int[] target = null;

  Visual.vType clazz = Visual.vType.VISUAL;

  public VisualFlat(Visual vis) {
    clazz = vis.getType();
    x = vis.x;
    y = vis.y;
    w = vis.w;
    h = vis.h;
    if (vis.isRegion()) {
      Visual match = vis.getLastMatch();
      if (SX.isNotNull(match)) {
        lastMatch = match.getVisualForJson();
      }
    } else if (vis.isMatch()) {
      score = vis.getScore();
    }
    target = new int[] {vis.getTarget().x, vis.getTarget().x};
  }

  public String getType() {
    return clazz.toString();
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getW() {
    return w;
  }

  public int getH() {
    return h;
  }

  public VisualFlat getLastMatch() {
    return lastMatch;
  }

  public Double getScore() {
    if (Visual.vType.MATCH.equals(clazz)) {
      return score;
    }
    return null;
  }

  public int[] getTarget() {
    return target;
  }
}
