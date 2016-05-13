/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import java.awt.*;

public abstract class Visual {

  //<editor-fold desc="***** Visual variants">
  public static enum vType {
    REGION, LOCATION, IMAGE, SCREEN, MATCH, WINDOW, PATTERN, OFFSET
  }

  public vType clazz;

  public boolean isRegion() {
    return vType.REGION.equals(clazz);
  }

  public boolean isRectangle() {
    return isRegion();
  }

  public boolean isLocation() {
    return vType.LOCATION.equals(clazz);
  }

  public boolean isPoint() {
    return isLocation();
  }

  public boolean isImage() {
    return vType.IMAGE.equals(clazz);
  }

  public boolean isMatch() {
    return vType.MATCH.equals(clazz);
  }

  public boolean isPattern() {
    return vType.PATTERN.equals(clazz);
  }

  public boolean isScreen() {
    return vType.SCREEN.equals(clazz);
  }

  public boolean isWindow() {
    return vType.WINDOW.equals(clazz);
  }

  public boolean isOffset() {
    return vType.OFFSET.equals(clazz);
  }
  //</editor-fold>

  //<editor-fold desc="***** construct, show">
  public int x = 0;
  public int y = 0;
  public int w = -1;
  public int h = -1;

  private static int stdW = 50;
  private static int stdH = 50;

  public static void setMargin(int wh) {
    setMargin(wh, wh);
  }

  private Match lastMatch = null;
  private Location target = null;

  public void init(int _x, int _y, int _w, int _h) {
    x = _x; y = _y;
    w = _w < 0 ? 0 : _w;
    h = _h < 0 ? 0 : _h;
  }

  public void init(Rectangle rect) {
    x = rect.x; y = rect.y; w = rect.width; h = rect.height;
  }

  public void init(Point p) {
    x = p.x; y = p.y; w = 0; h = 0;
  }

  public void init(Visual vis) {
    x = vis.x; y = vis.y; w = vis.w; h = vis.h;
  }

  @Override
  public String toString() {
    if (isLocation()) {
      return String.format("[\"%s\", [%d, %d]%s]", clazz, x, y, toStringPlus());
    }
    return String.format("[\"%s\", [%d, %d, %d, %d]%s]", clazz, x, y, w, h, toStringPlus());
  }

  protected String toStringPlus() {
    return "";
  }

  /**
   * check wether the given object is in JSON format as ["ID", ...]
   * @param json
   * @return true if object is in JSON format, false otherwise
   */
  public static boolean isJSON(Object json) {
    if (json instanceof String) {
      return ((String) json).startsWith("[\"");
    }
    return false;
  }

  public static Object fromJson(Object json) {
    if (!isJSON(json)) return json;
    Visual vis = null;
    return vis;
  }
  //</editor-fold>

  //<editor-fold desc="***** get, set, change">
  public static void setMargin(int w, int h) {
    if (w  > 0) {
      stdW = w;
    }
    if (h  > 0) {
      stdH = h;
    }
  }

  public static int[] getMargin() {
    return new int[] {stdW, stdH};
  }

  public Rectangle getRectangle() {
    return new Rectangle(x, y, w, h);
  }

  public void at(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Visual translate(int xoff, int yoff) {
    this.x += xoff;
    this.y += yoff;
    return this;
  }

  public Location getTarget() {
    //TODO implement getTarget()
    return target;
  }

  public void setTarget(Object visual) {
    //TODO implement setTarget()
  }

  public void setTarget(int x, int y) {
    target = new Location(x, y);
  }

  public Location getMatch() {
    if (lastMatch != null) {
      return lastMatch.getTarget();
    }
    return getTarget();
  }
  //</editor-fold>

  //<editor-fold desc="***** combine">
  public Region union(Visual vis) {
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(vis.x, vis.y, vis.w, vis.h);
    return new Region(r1.union(r2));
  }

  public boolean contains(Visual vis) {
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(vis.x, vis.y, vis.w, vis.h);
    return r1.contains(r2);
  }
  //</editor-fold>

  //<editor-fold desc="**** wait">
  public Visual wait(double time) {
    //TODO implement wait(double time)
    return this;
  }

  public Match wait(Visual vis) {
    //TODO implement wait(Visual vis)
    return new Match();
  }

  public Match wait(Visual vis, double time) {
    //TODO implement wait(Visual vis, double time)
    return new Match();
  }

  public boolean waitVanish(Visual vis) {
    //TODO implement wait(Visual vis)
    return true;
  }

  public boolean waitVanish(Visual vis, double time) {
    //TODO implement wait(Visual vis, double time)
    return true;
  }
  //</editor-fold>

  //<editor-fold desc="***** write, paste">
  public boolean write(String text) {
    //TODO implement write(String text)
    return true;
  }

  public boolean paste(String text) {
    //TODO implement paste(String text)
    return true;
  }
  //</editor-fold>
}
