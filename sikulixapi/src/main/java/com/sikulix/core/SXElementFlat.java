/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

public class SXElementFlat {

  int x = 0;
  int y = 0;
  int w = 0;
  int h = 0;

  SXElementFlat lastMatch = null;
  double score = 0;

  int[] target = null;

  SXElement.eType clazz = SXElement.eType.ELEMENT;

  public SXElementFlat(SXElement vis) {
    clazz = vis.getType();
    x = vis.x;
    y = vis.y;
    w = vis.w;
    h = vis.h;
    if (vis.isRegion()) {
      SXElement match = vis.getLastMatch();
      if (SX.isNotNull(match)) {
        lastMatch = match.getElementForJson();
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

  public SXElementFlat getLastMatch() {
    return lastMatch;
  }

  public Double getScore() {
    if (SXElement.eType.MATCH.equals(clazz)) {
      return score;
    }
    return null;
  }

  public int[] getTarget() {
    return target;
  }
}
