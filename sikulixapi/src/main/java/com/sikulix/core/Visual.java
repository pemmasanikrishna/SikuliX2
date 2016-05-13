/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import java.awt.Rectangle;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

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
    return isRegion() || isMatch() || isScreen() || isWindow();
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

  //<editor-fold desc="x y w h">
  public int x = 0;
  public int y = 0;
  public int w = -1;
  public int h = -1;

  public int getX() { return x;}
  public int getY() { return y;}
  public int getW() { return w;}
  public int getH() { return h;}
  //</editor-fold>

  //<editor-fold desc="margin">
  private static int stdW = 50;
  private static int stdH = 50;

  public static int[] getMargin() {
    return new int[]{stdW, stdH};
  }

  public static void setMargin(int w, int h) {
    if (w > 0) {
      stdW = w;
    }
    if (h > 0) {
      stdH = h;
    }
  }

  public static void setMargin(int wh) {
    setMargin(wh, wh);
  }
  //</editor-fold>

  //<editor-fold desc="lastMatch">
  private Match lastMatch = null;

  public Match getLastMatch() {
    return lastMatch;
  }

  protected void setLastmatch(Match match) {
    lastMatch = match;
  }
  //</editor-fold>

  //<editor-fold desc="lastMatches">
  private List<Match> lastMatches = new ArrayList<Match>();

  public List<Match> getLastMatches() {
    return lastMatches;
  }

  public void setLastMatches(List<Match> lastMatches) {
    this.lastMatches = lastMatches;
  }
  //</editor-fold>

  //<editor-fold desc="lastCapture">
  private Image lastCapture = null;

  public Image getLastCapture() {
    return lastCapture;
  }

  public void setLastCapture(Image lastCapture) {
    this.lastCapture = lastCapture;
  }
  //</editor-fold>

  //<editor-fold desc="target">
  private Location target = null;

  public Visual setTarget(Visual vis) {
    if (vis.isOffset()) {
      target = (Location) getCenter().translate((Offset) vis);
    } else {
      target = new Location(vis.x, vis.y);
    }
    return this;
  }

  public Visual setTarget(int x, int y) {
    target = new Location(x, y);
    return this;
  }

  public Location getTarget() {
    if (SX.isUnset(target)) {
      target = getCenter();
    }
    return target;
  }

  public Location getMatch() {
    if (lastMatch != null) {
      return lastMatch.getTarget();
    }
    return getTarget();
  }
  //</editor-fold>

  //<editor-fold desc="***** construct, show">
  public void init(int _x, int _y, int _w, int _h) {
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    if (isPoint()) {
      w = _w < 0 ? 0 : _w;
      h = _h < 0 ? 0 : _h;
    } else if (!isOffset()) {
      w = _w < 1 ? 1 : _w;
      h = _h < 1 ? 1 : _h;
    }
  }

  public void init(Rectangle rect) {
    init(rect.x, rect.y, rect.width, rect.height);
  }

  public void init(Point p) {
    init(p.x, p.y, 0, 0);
  }

  public void init(Visual vis) {
    init(vis.x, vis.y, vis.w, vis.h);
  }

  @Override
  public String toString() {
    if (isLocation() || isOffset()) {
      return String.format("[\"%s\", [%d, %d]]", clazz, x, y);
    }
    return String.format("[\"%s\", [%d, %d, %d, %d]%s]", clazz, x, y, w, h, toStringPlus());
  }

  protected String toStringPlus() {
    return "";
  }

  /**
   * check wether the given object is in JSON format as ["ID", ...]
   *
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
  public Rectangle getRectangle() {
    return new Rectangle(x, y, w, h);
  }

  public Visual at(int _x, int _y) {
    if (!SX.isUnset(target)) {
      translate(_x - x, _y - y);
    }
    x = _x;
    y = _y;
    return this;
  }

  public Visual translate(int xoff, int yoff) {
    this.x += xoff;
    this.y += yoff;
    return this;
  }

  public Visual translate(Offset off) {
    return translate(off.x, off.y);
  }

  public Location getCenter() {
    return new Location(x + w/2, y + h/2);
  }
  //</editor-fold>

  //<editor-fold desc="***** combine">
  public Region union(Visual vis) {
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(vis.x, vis.y, vis.w, vis.h);
    return new Region(r1.union(r2));
  }

  public boolean contains(Visual vis) {
    if (!isRectangle()) {
      return false;
    }
    if (!vis.isRectangle() && !vis.isPoint()) {
      return false;
    }
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(vis.x, vis.y, vis.w, vis.h);
    if (vis.isRectangle()) {
      return r1.contains(vis.x, vis.y, vis.w, vis.h);
    }
    if (vis.isPoint()) {
      return r1.contains(vis.x, vis.y);
    }
    return false;
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
