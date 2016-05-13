/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

public class Pattern extends Visual {

  private static vType vClazz = vType.PATTERN;
  private static SXLog log = SX.getLogger(vClazz.toString());
  //clazz = vClazz;

  String pImage = "";
  double pScore = 0.7;
  Offset pTargetOffset = null;

  public Pattern() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Pattern(String image) {
    this();
    pImage = image;
  }

  public Pattern similar(double score) {
    pScore = score;
    return this;
  }

  public Pattern targetOffset(int xoff, int yoff) {
    pTargetOffset = new Offset(xoff, yoff);
    return this;
  }
}
