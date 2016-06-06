/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.Visual;

public class Pattern extends Image {

  private static vType vClazz = vType.PATTERN;
  private static SXLog log = SX.getLogger("SX." + vClazz.toString());

  private static double exactAs = 0.99f;

  public static void setExactAs(double minimumScore) {
    exactAs = minimumScore;
  }

  public static double getExactAs() {
    return exactAs;
  }

  public Pattern() {
    clazz = vClazz;
    init(0, 0, 0, 0);
  }

  public Pattern(String fpImage) {
    this();
    this.image = new Image(fpImage);
  }

  public Pattern(Visual vis) {
    this();
    this.image = vis.getImage();
  }

  public Pattern similar(double score) {
    setMinimumScore(score);
    return this;
  }

  public double getSimilar() {
    return getMinimumScore();
  }

  /**
   * sets the minimum wanted similarity score to the value which means exact match (default 0.99)
   *
   * @return the Pattern object itself
   */
  public Pattern exact() {
    setMinimumScore(exactAs);
    return this;
  }

  @Deprecated
  public Pattern targetOffset(int xoff, int yoff) {
    setOffset(xoff, yoff);
    return this;
  }

  @Deprecated
  public Pattern targetOffset(Location loc) {
    setOffset(loc.x, loc.y);
    return this;
  }

  @Deprecated
  public Location getTargetOffset() {
    return new Location(getOffset().x, getOffset().y);
  }
}
