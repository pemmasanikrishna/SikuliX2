/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.By;
import com.sikulix.api.Element;
import org.json.JSONObject;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SXElement implements Comparable<SXElement> {

  public static Mat getNewMat() {
    SX.loadNative(SX.NATIVES.OPENCV);
    return new Mat();
  }

  //<editor-fold desc="housekeeping">
  public enum eType {
    SXELEMENT, ELEMENT, SYMBOL, PICTURE, TARGET, WINDOW,
    REGION, MATCH, SCREEN, PATTERN, LOCATION;

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
  //</editor-fold>

  //<editor-fold desc="***** variants">
  public boolean isOnScreen() {
    return isElement() && !isTarget() || isWindow();
  }

  public boolean isRectangle() {
    return (isElement() && !isPoint()) || isWindow();
  }

  public boolean isPoint() {
    return isElement() && w < 2 && h < 2;
  }

  public boolean isElement() {
    return eType.SYMBOL.equals(clazz) || eType.ELEMENT.equals(clazz) || isPicture() || isTarget() || isWindow();
  }

  public boolean isPicture() {
    return eType.PICTURE.equals(clazz);
  }

  public boolean isSymbol() {
    return eType.SYMBOL.equals(clazz);
  }

  public boolean isTarget() {
    return eType.TARGET.equals(clazz) || isPicture();
  }

  public boolean isWindow() {
    return eType.WINDOW.equals(clazz);
  }

  public boolean isSpecial() {
    return !SX.isNull(containingDevice);
  }

  public Object getContainingDevice() {
    return containingDevice;
  }

  public void setContainingDevice(Object containingDevice) {
    this.containingDevice = containingDevice;
  }

  Object containingDevice = null;

  /**
   * @return true if the element is useable and/or has valid content
   */
  public boolean isValid() {
    return w > 1 && h > 1;
  }

  ;

  //</editor-fold>

  //<editor-fold desc="***** construct, info">
  public Integer x = 0;
  public Integer y = 0;
  public Integer w = -1;
  public Integer h = -1;

  public IDevice getElementDevice() {
    if (SX.isNull(elementDevice)) {
      if (!isSpecial()) {
        elementDevice = SX.getSXLOCALDEVICE();
      } else {
        log.error("not implemented: non-local devices");
        elementDevice = SX.getSXLOCALDEVICE();
      }
    }
    return elementDevice;
  }

  public void setElementDevice(IDevice elementDevice) {
    this.elementDevice = elementDevice;
  }

  protected IDevice elementDevice = null;

  public String getName() {
    return name;
  }

  public Element setName(String name) {
    this.name = name;
    return (Element) this;
  }

  static Pattern patStdName = Pattern.compile("[EPTWMS]_\\d+?_\\d+?_\\d+?x\\d+");

  public boolean hasName() {
    Matcher matchStdName  = patStdName.matcher(name);
    boolean isStdName = matchStdName.find();
    return !isStdName;
  }

  private String name = "";

//  protected boolean onRemoteScreen = false;

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

  public long getPixelSize() {
    return w * h;
  }

  protected void init() {
    //init(0, 0, 0, 0);
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
    initAfter();
  }

  protected void initAfter() {
    if (SX.isNotSet(name)) {
      initName(eClazz);
    }
  }

  protected void initName(eType type) {
    if (SX.isNotSet(name)) {
      setName(String.format("%s_%d_%d_%dx%d", type.toString().substring(0, 1), x, y, w, h));
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
    String name = hasName() ? getName() : clazz.toString();
    if (isPoint()) {
      return String.format("[\"%s\", [%d, %d]]", name, x, y);
    }
    return String.format("[\"%s\", [%d, %d, %d, %d]%s]", name, x, y, w, h, toStringPlus());
  }

  protected String toStringPlus() {
    return "";
  }

  public String logString() {
    return String.format("[%d,%d %dx%d]", x, y, w, h);
  }

  //</editor-fold>

  //<editor-fold desc="waiting times">
  private double waitForThis = -1;

  public double getWaitForThis() {
    if (waitForThis < 0) {
      waitForThis = SX.getOptionNumber("Settings.AutoWaitTimeout");
    }
    return waitForThis;
  }

  public void setWaitForThis(double waitForThis) {
    this.waitForThis = waitForThis;
  }

  private double waitForMatch = -1;

  public double getWaitForMatch() {
    if (waitForMatch < 0) {
      waitForMatch = SX.getOptionNumber("Settings.AutoWaitTimeout");
    }
    return waitForMatch;
  }

  public void setWaitForMatch(double waitForMatch) {
    this.waitForMatch = waitForMatch;
  }

  private double lastWaitForThis = 0;

  public double getLastWaitForThis() {
    return lastWaitForThis;
  }

  public void setLastWaitForThis(double lastWaitForThis) {
    this.lastWaitForThis = lastWaitForThis;
  }

  private double lastWaitForMatch = 0;

  public double getLastWaitForMatch() {
    return lastWaitForMatch;
  }

  public void setLastWaitForMatch(double lastWaitForMatch) {
    this.lastWaitForMatch = lastWaitForMatch;
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

  public void setRectangle(Rectangle rect) {
    x = rect.x;
    y = rect.y;
    w = rect.width;
    h = rect.height;
  }

  public Element getCenter() {
    return new Element(x + w / 2, y + h / 2);
  }

  public void setCentered() {
    Element centered = getCentered(new Element(SX.getSXLOCALDEVICE().getMonitor()));
    x = centered.x;
    y = centered.y;
  }

  public void setCentered(Element base) {
    Element centered = getCentered(base);
    x = centered.x;
    y = centered.y;
  }

  public Element getCentered(int... args) {
    if (args.length == 0) {
      return getCentered(new Element(SX.getSXLOCALDEVICE().getMonitor()), null);
    } else {
      return getCentered(new Element(SX.getSXLOCALDEVICE().getMonitor()), new Element(-args[0]));
    }
  }

  public Element getCentered(Element base) {
    return getCentered(base, null);
  }

  public Element getCentered(Element base, Element margin) {
    int mt = 0;
    int mr = 0;
    int mb = 0;
    int ml = 0;
    if (SX.isNotNull(margin)) {
      mt = margin.x;
      mr = margin.y;
      mb = margin.w;
      ml = margin.h;
    }
    int bcx = base.getCenter().x;
    int bcy = base.getCenter().y;
    int offX = w / 2 + ml;
    int offY = h / 2 + mt;
    int cx = bcx - offX;
    int cy = bcy - offY;
    return new Element(cx, cy);
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
    return new Element(x + w, y + h);
  }

  public Element getBottomLeft() {
    return new Element(x, y + h);
  }

  public Element leftAt() {
    return new Element(x, y + h / 2);
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
    return new Element(x + w, y + h / 2);
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
    return new Element(x + w / 2, y);
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
    return new Element(x + w / 2, y + h);
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
      return -1;
    } else if (elem.x == x && elem.y > y) {
      return -1;
    }
    return 1;
  }
  //</editor-fold>

  private static int growDefault = 2;

  public void grow() {
    grow(growDefault);
  }

  public void grow(int margin) {
    Rectangle r = getRectangle();
    r.grow(margin, margin);
    setRectangle(r);
  }

  public void grow(int hori, int verti) {
    Rectangle r = getRectangle();
    r.grow(hori, verti);
    setRectangle(r);
  }

  //<editor-fold desc="***** move">
  protected Element target = null;

  public void at(Integer x, Integer y) {
    this.x = x;
    this.y = y;
    if (!SX.isNull(target)) {
      target.translate(x - this.x, y - this.y);
    }
  }

  public void at(Element elem) {
    at(elem.x, elem.y);
  }

  public void translate(Integer xoff, Integer yoff) {
    this.x += xoff;
    this.y += yoff;
    if (!SX.isNull(target)) {
      target.translate(xoff, yoff);
    }
  }

  public void translate(Element off) {
    translate(off.x, off.y);
  }

  public void change(Element elem) {
    x = elem.x;
    y = elem.y;
    w = elem.w;
    h = elem.h;
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

  public void intersect(Element elem) {
    Element inter = intersection(elem);
    change(inter);
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

  //<editor-fold desc="utility">
  protected final static String PNG = "png";
  protected final static String dotPNG = "." + PNG;

  protected static Mat makeMat(BufferedImage bImg) {

    Mat aMat = getNewMat() ;
    if (bImg.getType() == BufferedImage.TYPE_INT_RGB) {
      log.trace("makeMat: INT_RGB (%dx%d)", bImg.getWidth(), bImg.getHeight());
      int[] data = ((DataBufferInt) bImg.getRaster().getDataBuffer()).getData();
      ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
      IntBuffer intBuffer = byteBuffer.asIntBuffer();
      intBuffer.put(data);
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC4);
      aMat.put(0, 0, byteBuffer.array());
      Mat oMatBGR = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      Mat oMatA = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC1);
      java.util.List<Mat> mixIn = new ArrayList<Mat>(Arrays.asList(new Mat[]{aMat}));
      java.util.List<Mat> mixOut = new ArrayList<Mat>(Arrays.asList(new Mat[]{oMatA, oMatBGR}));
      //A 0 - R 1 - G 2 - B 3 -> A 0 - B 1 - G 2 - R 3
      Core.mixChannels(mixIn, mixOut, new MatOfInt(0, 0, 1, 3, 2, 2, 3, 1));
      return oMatBGR;
    } else if (bImg.getType() == BufferedImage.TYPE_3BYTE_BGR) {
      log.error("makeMat: 3BYTE_BGR (%dx%d)", bImg.getWidth(), bImg.getHeight());
      byte[] data = ((DataBufferByte) bImg.getRaster().getDataBuffer()).getData();
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      aMat.put(0, 0, data);
      return aMat;
    } else if (bImg.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
      log.trace("makeMat: TYPE_4BYTE_ABGR (%dx%d)", bImg.getWidth(), bImg.getHeight());
      byte[] data = ((DataBufferByte) bImg.getRaster().getDataBuffer()).getData();
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC4);
      aMat.put(0, 0, data);
      Mat oMatBGR = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      Mat oMatA = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC1);
      java.util.List<Mat> mixIn = new ArrayList<Mat>(Arrays.asList(new Mat[]{aMat}));
      java.util.List<Mat> mixOut = new ArrayList<Mat>(Arrays.asList(new Mat[]{oMatA, oMatBGR}));
      //A 0 - R 1 - G 2 - B 3 -> A 0 - B 1 - G 2 - R 3
      Core.mixChannels(mixIn, mixOut, new MatOfInt(0, 0, 1, 1, 2, 2, 3, 3));
      return oMatBGR;
    } else {
      log.error("makeMat: Type not supported: %d (%dx%d)",
              bImg.getType(), bImg.getWidth(), bImg.getHeight());
    }
    return aMat;
  }

  public static BufferedImage getBufferedImage(Mat mat) {
    return getBufferedImage(mat, dotPNG);
  }

  public static BufferedImage getBufferedImage(Mat mat, String type) {
    BufferedImage bImg = null;
    MatOfByte bytemat = new MatOfByte();
    if (SX.isNull(mat)) {
      mat = getNewMat();
    }
    Imgcodecs.imencode(type, mat, bytemat);
    byte[] bytes = bytemat.toArray();
    InputStream in = new ByteArrayInputStream(bytes);
    try {
      bImg = ImageIO.read(in);
    } catch (IOException ex) {
      log.error("getBufferedImage: %s error(%s)", mat, ex.getMessage());
    }
    return bImg;
  }

  //</editor-fold>


}
