/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package org.sikuli.script;

import com.sikulix.api.Picture;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

public class Pattern {
  private static SXLog log = SX.getLogger("SX.PATTERN");

  Picture image = null;
  float similarity = -1;
  Location offset = new Location(0,0);

  public Pattern(String name) {
    image = new Picture(name);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String name = "";

  @Override
  public String toString() {
    String sOffset = (offset.x != 0 || offset.y != 0) ? "T: " + offset.x + "," + offset.y : "";
    return String.format("Pattern(%s) S: %d%% %s", name, (int) (similarity * 100), sOffset);
  }

  public Pattern similar(float sim) {
    similarity = sim;
    return this;
  }

  /**
   * sets the minimum Similarity to 0.99 which means exact match
   *
   * @return the Pattern object itself
   */
  public Pattern exact() {
    similarity = 0.99f;
    return this;
  }

  /**
   *
   * @return the current minimum similarity
   */
  public float getSimilar() {
    return this.similarity;
  }

  /**
   * set the offset from the match's center to be used with mouse actions
   *
   * @param dx x offset
   * @param dy y offset
   * @return the Pattern object itself
   */
  public Pattern targetOffset(int dx, int dy) {
    offset.x = dx;
    offset.y = dy;
    return this;
  }

  /**
   * set the offset from the match's center to be used with mouse actions
   *
   * @param loc Location
   * @return the Pattern object itself
   */
  public Pattern targetOffset(Location loc) {
    offset.x = loc.x;
    offset.y = loc.y;
    return this;
  }

  /**
   *
   * @return the current offset
   */
  public Location getTargetOffset() {
    return offset;
  }
}

