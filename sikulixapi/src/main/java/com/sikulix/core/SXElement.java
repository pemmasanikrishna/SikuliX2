/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.*;
import com.sikulix.api.Image;
import org.json.JSONObject;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class SXElement implements Comparable<SXElement>{

  static {
    SX.trace("Element: loadNative(SX.NATIVES.OPENCV)");
    SX.loadNative(SX.NATIVES.OPENCV);
  }

  public static void initClass() {};

  private static eType eClazz = eType.SXELEMENT;
  private static SXLog eLog = SX.getLogger("SX." + eClazz.toString());

  public eType getType() {
    return clazz;
  }

  //<editor-fold desc="variants">
  public static enum eType {
    SXELEMENT, ELEMENT, REGION, LOCATION, IMAGE, SCREEN, MATCH, WINDOW, PATTERN, OFFSET;

    static eType isType(String strType) {
      for (eType t : eType.values()) {
        if (t.toString().equals(strType)) {
          return t;
        }
      }
      return null;
    }
  }

  public eType clazz;

  public boolean isOnScreen() {
    return isRectangle() || isPoint();
  }

  public boolean isElement() {
    return eType.ELEMENT.equals(clazz);
  }

  public boolean isRectangle() {
    return isElement() || isMatch() || isWindow() || isScreen();
  }

  public boolean isPoint() {
    return isElement() && w == 1 && h == 1;
  }

  public boolean isImage() {
    return eType.IMAGE.equals(clazz);
  }

  public boolean isPattern() {
    return eType.PATTERN.equals(clazz) || isImage();
  }

  public boolean isMatch() {
    return eType.MATCH.equals(clazz);
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
  public boolean isValid() { return true; };

  //</editor-fold>

  //<editor-fold desc="x y w h">
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
  //</editor-fold>

  //<editor-fold desc="margin/padding">
  private static int stdM = 50;
  private static int stdP = 10;

  public static int getMargin() {
    return stdM;
  }

  public static int getPadding() {
    return stdP;
  }
  //</editor-fold>

  //<editor-fold desc="lastMatch">
  private Match lastMatch = null;

  public Match getLastMatch() {
    return lastMatch;
  }

  public void setLastMatch(Match match) {
    lastMatch = match;
  }

  public Element getMatchPoint() {
    if (SX.isNotNull(lastMatch)) {
      return lastMatch.getTarget();
    } else {
      return getTarget();
    }
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

  //<editor-fold desc="waitAfter">
  private double waitAfter = 0;

  public double getWaitAfter() {
    return waitAfter;
  }

  public void setWaitAfter(double waitAfter) {
    this.waitAfter = waitAfter;
  }
  //</editor-fold>

  //<editor-fold desc="target">
  private SXElement target = null;

  public SXElement setTarget(SXElement elem) {
    target = elem.getCenter();
    return this;
  }

  public SXElement setTarget(int x, int y) {
    target = getCenter().offset(x, y);
    return this;
  }

  public SXElement setTarget(int[] pos) {
    target = getCenter().offset(new Element(pos));
    return this;
  }

  public Element getTarget() {
    if (SX.isNull(target)) {
      target = getCenter();
    }
    return (Element) target;
  }
  //</editor-fold>

  //<editor-fold desc="score">
  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  private double score = -1;

  public double getMinimumScore() {
    return minimumScore;
  }

  public void setMinimumScore(double minimumScore) {
    this.minimumScore = minimumScore;
  }

  private double minimumScore = -1;
  //</editor-fold>

  //<editor-fold desc="image">
  public Image getImage() {
    return image;
  }

  public void setImage(Image img) {
    this.image = img;
  }

  protected Image image = null;
  //</editor-fold>

  //<editor-fold desc="content">
  protected Mat content = null;

  final static String PNG = "png";
  final static String dotPNG = "." + PNG;

  protected static Mat makeMat(BufferedImage bImg) {
    Mat aMat = null;
    if (bImg.getType() == BufferedImage.TYPE_INT_RGB) {
      eLog.trace("makeMat: INT_RGB (%dx%d)", bImg.getWidth(), bImg.getHeight());
      int[] data = ((DataBufferInt) bImg.getRaster().getDataBuffer()).getData();
      ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);
      IntBuffer intBuffer = byteBuffer.asIntBuffer();
      intBuffer.put(data);
      aMat = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC4);
      aMat.put(0, 0, byteBuffer.array());
      Mat oMatBGR = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC3);
      Mat oMatA = new Mat(bImg.getHeight(), bImg.getWidth(), CvType.CV_8UC1);
      List<Mat> mixIn = new ArrayList<Mat>(Arrays.asList(new Mat[]{aMat}));
      List<Mat> mixOut = new ArrayList<Mat>(Arrays.asList(new Mat[]{oMatA, oMatBGR}));
      //A 0 - R 1 - G 2 - B 3 -> A 0 - B 1 - G 2 - R 3
      Core.mixChannels(mixIn, mixOut, new MatOfInt(0, 0, 1, 3, 2, 2, 3, 1));
      return oMatBGR;
    } else if (bImg.getType() == BufferedImage.TYPE_3BYTE_BGR) {
      eLog.error("makeMat: 3BYTE_BGR (%dx%d)",
              bImg.getWidth(), bImg.getHeight());
    } else {
      eLog.error("makeMat: Type not supported: %d (%dx%d)",
              bImg.getType(), bImg.getWidth(), bImg.getHeight());
    }
    return aMat;
  }
  //</editor-fold>

  //<editor-fold desc="***** construct, info">
  public String toJson() {
    return SXJson.makeElement(this).toString();
  }

  public <T extends SXElement> T fromJson(String jsonElem) {
    JSONObject jobj = SXJson.makeObject(jsonElem);
    T retval = null;
    eType type = isValidJson(jobj);
    if (SX.isNull(type)) {
      eLog.error("fromJson: not valid: ", jobj);
    } else {
      int x, y, w, h;
      x = jobj.getInt("x");
      y = jobj.getInt("y");
      w = jobj.getInt("w");
      h = jobj.getInt("h");
      if (eType.REGION.equals(type)) {
        retval = (T) new Element(x, y, w, h);
      }
    }
    return retval;
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

  public void init(int _x, int _y, int _w, int _h) {
    x = _x;
    y = _y;
    w = _w;
    h = _h;
    if (isPoint()) {
      w = _w < 0 ? 0 : _w;
      h = _h < 0 ? 0 : _h;
    }
    minimumScore = SX.getOptionNumber("Settings.MinSimilarity", 0.7d);
  }

  public void init(int[] rect) {
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

  public void init(Rectangle rect) {
    init(rect.x, rect.y, rect.width, rect.height);
  }

  public void init(Point p) {
    init(p.x, p.y, 0, 0);
  }

  public void init(SXElement elem) {
    init(elem.x, elem.y, elem.w, elem.h);
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

  //TODO equals and compare
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
  public int compareTo(SXElement vis) {
    if (equals(vis)) {
      return 0;
    }
    if (vis.x > x) {
      return 1;
    } else if (vis.x == x) {
      if (vis.y > y) {
        return 1;
      }
    }
    return -1;
  }
  //</editor-fold>

  //<editor-fold desc="***** get, set, change">
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

  public void at(Integer x, Integer y) {
    this.x = x;
    this.y = y;
    if (!SX.isNull(target)) {
      target.translate(x - this.x, y - this.y);
    }
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

  /**
   * returns -1, if outside of any screen <br>
   *
   * @return the sequence number of the screen, that contains the given point
   */
  public int getContainingScreenNumber() {
    Rectangle r;
    for (int i = 0; i < SX.getNumberOfMonitors(); i++) {
      r = SX.getMonitor(i);
      if (r.contains(this.x, this.y)) {
        return i;
      }
    }
    return -1;
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

  //<editor-fold desc="***** capture/show/highlight">
  public Image capture() {
    Image img = new Image();
    if (isSpecial()) {
      SX.terminate(1, "capture: special not implemented");
    } else {
      Robot robot = SX.getSXROBOT();
      img = new Image(robot.createScreenCapture(getRectangle()));
    }
    return img;
  }

  public void show() {
    show((int) SX.getOptionNumber("DefaultHighlightTime"));
  }

  public void show(int time) {
    JFrame frImg = new JFrame();
    frImg.setAlwaysOnTop(true);
    frImg.setResizable(false);
    frImg.setUndecorated(true);
    frImg.setLocation(x, y);
    frImg.setSize(w, h);
    Container cp = frImg.getContentPane();
    cp.add(new JLabel(new ImageIcon(getImageBytes())), BorderLayout.CENTER);
    frImg.pack();
    frImg.setVisible(true);
    SX.pause(time);
    frImg.dispose();
  }

  public void highlight() {
    highlight((int) SX.getOptionNumber("DefaultHighlightTime"));
  }

  public void highlight(int time) {
  //TODO Element.highlight not implemented
    eLog.error("highlight not implemented");
  }

  public BufferedImage getBufferedImage() {
    return getBufferedImage(dotPNG);
  }

  protected BufferedImage getBufferedImage(String type) {
    BufferedImage bImg = null;
    byte[] bytes = getImageBytes(type);
    InputStream in = new ByteArrayInputStream(bytes);
    try {
      bImg = ImageIO.read(in);
    } catch (IOException ex) {
      eLog.error("getBufferedImage: %s error(%s)", this, ex.getMessage());
    }
    return bImg;
  }

  protected byte[] getImageBytes(String dotType) {
    MatOfByte bytemat = new MatOfByte();
    if (SX.isNull(content)) {
      content = new Mat();
    }
    Highgui.imencode(dotType, content, bytemat);
    return bytemat.toArray();
  }

  public byte[] getImageBytes() {
    return getImageBytes(dotPNG);
  }

  public static void fakeHighlight(boolean state) {
    //TODO implement fakeHighlight
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

  //<editor-fold desc="**** wait">
  public void wait(double time) {
    SX.pause(time);
  }

  public Match wait(SXElement vis) {
    //TODO implement wait(Element vis)
    return new Match();
  }

  public Match wait(SXElement vis, double time) {
    //TODO implement wait(Element vis, double time)
    return new Match();
  }

  public boolean waitVanish(SXElement vis) {
    //TODO implement wait(Element vis)
    return true;
  }

  public boolean waitVanish(SXElement vis, double time) {
    //TODO implement wait(Element vis, double time)
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

  //<editor-fold desc="***** observe">
  public void stopObserver() {
    stopObserver("");
  }

  public void stopObserver(String text) {
    //TODO implement stopObserver()
  }
  //</editor-fold>

  //<editor-fold desc="***** mouse">
  public IRobot getDeviceRobot() {
    //TODO implement special Robots
    return SX.getLocalRobot();
  }
  /**
   * Move the mouse to this element's target
   *
   * @return this
   */
  public SXElement hover() {
    Mouse.get().move(this.getTarget());
    return this;
  }

  /**
   * Move the mouse to this element's target and click left
   *
   * @return this
   */
  public SXElement click() {
    Mouse.get().click(this.getTarget(), "L");
    return this;
  }

  /**
   * Move the mouse to this element's target and double click left
   *
   * @return this
   */
  public SXElement doubleClick() {
    Mouse.get().click(this.getTarget(), "LD");
    return this;
  }

  /**
   * Move the mouse to this element's target and click right
   *
   * @return this
   */
  public SXElement rightClick() {
    Mouse.get().click(this.getTarget(), "R");
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="*****  like Selenium: findElement, get..., is...">
  public Match findElement(By by) {
    return new Match();
  }

  public List<Match> findElements(By by) {
    return new ArrayList<Match>();
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

  //TODO implement isDisplayed
  public boolean isDisplayed() {
    return true;
  }

  public void sendKeys(CharSequence keys) {
    write(keys.toString());
  }
  //</editor-fold>

}
