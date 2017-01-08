/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.By;
import com.sikulix.api.Element;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class SXElement implements Comparable<SXElement>{

  static {
    SX.trace("SXElement: loadNative(SX.NATIVES.OPENCV)");
    SX.loadNative(SX.NATIVES.OPENCV);
  }

  public enum eType {
    SXELEMENT, ELEMENT, IMAGE, TARGET, SCREEN, WINDOW, PATTERN;

    static eType isType(String strType) {
      for (eType t : eType.values()) {
        if (t.toString().equals(strType)) {
          return t;
        }
      }
      return null;
    }
  }

  private static eType eClazz = eType.SXELEMENT;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  protected eType clazz = eClazz;
  public eType getType() {
    return clazz;
  }

  //<editor-fold desc="***** variants">
  public boolean isOnScreen() {
    return isRectangle();
  }

  public boolean isRectangle() {
    return isElement() || isWindow() || isScreen();
  }

  public boolean isPoint() {
    return isElement() && w == 1 && h == 1;
  }

  public boolean isElement() {
    return eType.ELEMENT.equals(clazz);
  }

  public boolean isImage() {
    return eType.IMAGE.equals(clazz);
  }

  public boolean isTarget() {
    return eType.TARGET.equals(clazz) || isImage();
  }

  public boolean isScreen() {
    return eType.SCREEN.equals(clazz);
  }

  public boolean isWindow() {
    return eType.WINDOW.equals(clazz);
  }

  public boolean isSpecial() { return !SX.isNull(containingScreen); }

  Object containingScreen = null;

  /**
   * @return true if the element is useable and/or has valid content
   */
  public boolean isValid() {
    return w > 0 && h > 0;
  };

  //</editor-fold>

  //<editor-fold desc="***** construct, info">
  public Integer x = 0;
  public Integer y = 0;
  public Integer w = -1;
  public Integer h = -1;

//  protected boolean onRemoteScreen = false;

  public int getX() { return x;}
  public int getY() { return y;}
  public int getW() { return w;}
  public int getH() { return h;}

  public long getPixelSize() {
    return w * h;
  }

  protected void init() {
    init(0, 0, 0, 0);
  }

  protected void init(int _x, int _y, int _w, int _h) {
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    if (isPoint()) {
      w = _w < 0 ? 0 : _w;
      h = _h < 0 ? 0 : _h;
    }
  }

  protected void init(int[] rect) {
    int _x = 0;
    int _y = 0;
    int _w = 0;
    int _h = 0;
    switch (rect.length) {
      case 0:
        return;
      case 1:
        _x = rect[0];
        break;
      case 2:
        _x = rect[0];
        _y = rect[1];
        break;
      case 3:
        _x = rect[0];
        _y = rect[1];
        _w = rect[2];
        break;
      default:
        _x = rect[0];
        _y = rect[1];
        _w = rect[2];
        _h = rect[3];
    }
    init(_x, _y, _w, _h);
  }

  protected void init(Rectangle rect) {
    init(rect.x, rect.y, rect.width, rect.height);
  }

  protected void init(Point p) {
    init(p.x, p.y, 0, 0);
  }

  @Override
  public String toString() {
    if (isPoint()) {
      return String.format("[\"%s\", [%d, %d]]", clazz, x, y);
    }
    return String.format("[\"%s\", [%d, %d, %d, %d]%s]", clazz, x, y, w, h, toStringPlus());
  }

  protected String toStringPlus() {
    return "";
  }

  //</editor-fold>

  //<editor-fold desc="TODO margin/padding">
  private static int stdM = 50;
  private static int stdP = 10;

  public static int getMargin() {
    return stdM;
  }

  public static int getPadding() {
    return stdP;
  }
  //</editor-fold>

  //<editor-fold desc="***** JSON">
  public String toJson() {
    return SXJson.makeElement((Element) this).toString();
  }

  private eType isValidJson(JSONObject jobj) {
    eType type = null;
    if (jobj.has("type")) {
      type = eType.isType(jobj.getString("type"));
      if (SX.isNotNull(type)) {
        if (!(jobj.has("x") && jobj.has("y") && jobj.has("w") && jobj.has("h"))) {
          type = null;
        }
      }
    }
    return type;
  }
  //</editor-fold>

  //<editor-fold desc="***** get...">
  public Element getRegion() {
    return new Element(x, y, w, h);
  }

  public Rectangle getRectangle() {
    return new Rectangle(x, y, w, h);
  }

  public Element getCenter() {
    return new Element(x + w/2, y + h/2);
  }

  public Point getPoint() {
    return new Point(getCenter().x, getCenter().y);
  }

  /**
   * creates a point at the given offset, might be negative<br>
   * for a rectangle the reference is the center
   *
   * @param off an offset
   * @return new location
   */
  public Element offset(Element off) {
    return new Element(getCenter().x + off.x, getCenter().y + off.y);
  }

  /**
   * creates a point at the given offset, might be negative<br>
   * for a rectangle the reference is the center
   *
   * @param xoff x offset
   * @param yoff y offset
   * @return new location
   */
  public Element offset(Integer xoff, Integer yoff) {
    return new Element(getCenter().x + xoff, getCenter().y + yoff);
  }

  public Element getTopLeft() {
    return new Element(x, y);
  }

  public Element getTopRight() {
    return new Element(x + w, y);
  }

  public Element getBottomRight() {
    return new Element(x, y + h);
  }

  public Element getBottomLeft() {
    return new Element(x, y + h);
  }

  public Element leftAt() {
    return new Element(x, y + h/2);
  }

  /**
   * creates a point at the given offset to the left<br>
   * negative means the opposite direction<br>
   * for rectangles the reference point is the middle of the left side
   *
   * @param xoff x offset
   * @return new location
   */
  public Element left(Integer xoff) {
    return new Element(x - xoff, leftAt().y);
  }

  public Element rightAt() {
    return new Element(x + w, y + h/2);
  }

  /**
   * creates a point at the given offset to the right<br>
   * negative means the opposite direction<br>
   * for rectangles the reference point is the middle of the right side
   *
   * @param xoff x offset
   * @return new location
   */
  public Element right(Integer xoff) {
    return new Element(rightAt().x + xoff, rightAt().y);
  }

  public Element aboveAt() {
    return new Element(x + w/2, y);
  }

  /**
   * creates a point at the given offset above<br>
   * negative means the opposite direction<br>
   * for rectangles the reference point is the middle of upper side
   *
   * @param yoff y offset
   * @return new location
   */
  public Element above(Integer yoff) {
    return new Element(aboveAt().x, y - yoff);
  }

  public Element belowAt() {
    return new Element(x + w/2, y + h);
  }

  /**
   * creates a point at the given offset below<br>
   * negative means the opposite direction<br>
   * for rectangles the reference point is the middle of the lower side
   *
   * @param yoff y offset
   * @return new location
   */
  public Element below(Integer yoff) {
    return new Element(belowAt().x, belowAt().y + yoff);
  }

// TODO getColor() implement more support and make it useable
  /**
   * Get the color at the given Point (center of element) for details: see java.awt.Robot and ...Color
   *
   * @return The Color of the Point or null if not possible
   */
  public Color getColor() {
    if (isOnScreen()) {
      return getScreenColor();
    }
    return null;
  }

  private static Color getScreenColor() {
    return null;
  }
  //</editor-fold>

  //<editor-fold desc="TODO equals/compare">
  @Override
  public boolean equals(Object oThat) {
    if (this == oThat) {
      return true;
    }
    if (!(oThat instanceof SXElement)) {
      return false;
    }
    SXElement that = (SXElement) oThat;
    return x == that.x && y == that.y;
  }

  @Override
  public int compareTo(SXElement elem) {
    if (equals(elem)) {
      return 0;
    }
    if (elem.x > x) {
      return 1;
    } else if (elem.x == x) {
      if (elem.y > y) {
        return 1;
      }
    }
    return -1;
  }
  //</editor-fold>

  //<editor-fold desc="***** move">
  protected Element target = null;

  public void at(Integer x, Integer y) {
    if (isRectangle()) {
      this.x = x;
      this.y = y;
      if (!SX.isNull(target)) {
        target.translate(x - this.x, y - this.y);
      }
    }
  }

  public void translate(Integer xoff, Integer yoff) {
    if (isRectangle()) {
      this.x += xoff;
      this.y += yoff;
      if (!SX.isNull(target)) {
        target.translate(xoff, yoff);
      }
    }
  }

  public void translate(Element off) {
    translate(off.x, off.y);
  }
  //</editor-fold>

  //<editor-fold desc="***** combine">
  public Element union(SXElement elem) {
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(elem.x, elem.y, elem.w, elem.h);
    return new Element(r1.union(r2));
  }

  public Element intersection(SXElement elem) {
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(elem.x, elem.y, elem.w, elem.h);
    return new Element(r1.intersection(r2));
  }

  public boolean contains(SXElement elem) {
    if (!isRectangle()) {
      return false;
    }
    if (!elem.isRectangle() && !elem.isPoint()) {
      return false;
    }
    Rectangle r1 = new Rectangle(x, y, w, h);
    Rectangle r2 = new Rectangle(elem.x, elem.y, elem.w, elem.h);
    if (elem.isRectangle()) {
      return r1.contains(elem.x, elem.y, elem.w, elem.h);
    }
    if (elem.isPoint()) {
      return r1.contains(elem.x, elem.y);
    }
    return false;
  }
  //</editor-fold>

  //<editor-fold desc="TODO be like Selenium">
  public Element findElement(By by) {
    return new Element();
  }

  public List<Element> findElements(By by) {
    return new ArrayList<Element>();
  }

  public String getAttribute(String key) {
    return "NotAvailable";
  }

  public Element getLocation() {
    return getTopLeft();
  }

  public Element getRect() {
    return (Element) this;
  }

  public Dimension getSize() {
    return new Dimension(w, h);
  }

  //TODO implement OCR
  public String getText() {
    return "NotImplemented";
  }
  //</editor-fold>
}
