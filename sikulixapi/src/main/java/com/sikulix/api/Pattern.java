/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Visual;

public class Pattern extends Visual {

  private static vType vClazz = vType.PATTERN;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());
  //clazz = vClazz;

  public Pattern() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Pattern(String image) {
    this();
    this.image = new Image(image);
  }

  public Pattern(Visual vis) {
    this();
    this.image = vis.getImage();
  }

  public Pattern similar(double score) {
    this.score = score;
    return this;
  }

  public Pattern targetOffset(int xoff, int yoff) {
    setOffset(xoff, yoff);
    return this;
  }
}
