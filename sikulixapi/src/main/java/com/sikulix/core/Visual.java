/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.core;

import com.sikulix.api.*;
import com.sikulix.api.Image;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.sikuli.basics.Settings;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Robot;
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

public abstract class Visual implements Comparable<Visual>{

  static {
    SX.trace("Visual: loadNative(SX.NATIVES.OPENCV)");
    SX.loadNative(SX.NATIVES.OPENCV);
  }

  private static vType vClazz = vType.VISUAL;
  private static SXLog vLog = SX.getLogger("SX." + vClazz.toString());

  //<editor-fold desc="***** Visual variants">
  public static enum vType {
    VISUAL, REGION, LOCATION, IMAGE, SCREEN, MATCH, WINDOW, PATTERN, OFFSET
  }

  public vType clazz;

  public boolean isOnScreen() {
    return isRectangle() || isPoint();
  }

  public boolean isRectangle() {
    return isRegion() || isMatch() || isScreen() || isWindow();
  }

  public boolean isRegion() {
    return vType.REGION.equals(clazz);
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

  public boolean isSpecial() { return !SX.isNull(containingScreen); }

  Object containingScreen = null;

  /**
   * @return true if the Visual is useable and/or has valid content
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

  public long getSize() {
    return w * h;
  }
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

  protected void setLastMatch(Match match) {
    lastMatch = match;
  }

  public Location getMatch() {
    if (lastMatch != null) {
      return lastMatch.getTarget();
    }
    return getTarget();
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
  public enum FindType {
    ONE, ALL, VANISH, ANY, BEST
  }

  private double waitAfter = 0;

  public double getWaitAfter() {
    return waitAfter;
  }

  public void setWaitAfter(double waitAfter) {
    this.waitAfter = waitAfter;
  }

  public Offset getOffset() {
    return offset;
  }

  public void setOffset(Offset offset) {
    this.offset = offset;
  }

  public void setOffset(int xoff, int yoff) {
    this.offset = new Offset(xoff, yoff);
  }

  private Offset offset = null;

  public Visual setTarget(Visual vis) {
    if (vis.isOffset()) {
      target = getCenter().offset((Offset) vis);
    } else {
      target = vis.getCenter();
    }
    return this;
  }

  public Visual setTarget(int x, int y) {
    target = getCenter().offset(x, y);
    return this;
  }

  public Location getTarget() {
    if (SX.isNull(target)) {
      target = getCenter();
    }
    if (!SX.isNull(offset)) {
      return target.offset(offset);
    }
    return (Location) target;
  }

  private Visual target = null;

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
      vLog.trace("makeMat: INT_RGB (%dx%d)", bImg.getWidth(), bImg.getHeight());
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
      vLog.error("makeMat: 3BYTE_BGR (%dx%d)",
              bImg.getWidth(), bImg.getHeight());
    } else {
      vLog.error("makeMat: Type not supported: %d (%dx%d)",
              bImg.getType(), bImg.getWidth(), bImg.getHeight());
    }
    return aMat;
  }
  //</editor-fold>

  //<editor-fold desc="***** construct, info">
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
    minimumScore = SX.getOptionNumber("Settings.MinSimilarity", 0.7d);
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

  //TODO equals and compare
  @Override
  public boolean equals(Object oThat) {
    if (this == oThat) {
      return true;
    }
    if (!(oThat instanceof Visual)) {
      return false;
    }
    Visual that = (Visual) oThat;
    return x == that.x && y == that.y;
  }

  @Override
  public int compareTo(Visual vis) {
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
  public Region getRegion() {
    return new Region(x, y, w, h);
  }

  public Rectangle getRectangle() {
    return new Rectangle(x, y, w, h);
  }

  public Location getCenter() {
    return new Location(x + w/2, y + h/2);
  }

  public Point getPoint() {
    if (isPoint()) {
      return new Point(x, y);
    }
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

  public void translate(Offset off) {
    translate(off.x, off.y);
  }

  /**
   * creates a point at the given offset, might be negative<br>
   * for a rectangle the reference is the center
   *
   * @param off an offset
   * @return new location
   */
  public Location offset(Offset off) {
    if (isPoint()) {
      return new Location(off.x, off.y);
    }
    return getCenter().offset(off);
  }

  /**
   * creates a point at the given offset, might be negative<br>
   * for a rectangle the reference is the center
   *
   * @param xoff x offset
   * @param yoff y offset
   * @return new location
   */
  public Location offset(Integer xoff, Integer yoff) {
    if (isPoint()) {
      return new Location(x + xoff, y + yoff);
    }
    return getCenter().offset(xoff, yoff);
  }

  /**
   * creates a point at the given offset to the left<br>
   * negative means the opposite direction<br>
   * for rectangles the reference point is the middle of the left side
   *
   * @param xoff x offset
   * @return new location
   */
  public Location left(Integer xoff) {
    if (isPoint()) {
      return new Location(x - xoff, y);
    }
    return new Location(getCenter().x - (int) (w/2) - xoff, y);
  }

  /**
   * creates a point at the given offset to the right<br>
   * negative means the opposite direction<br>
   * for rectangles the reference point is the middle of the right side
   *
   * @param xoff x offset
   * @return new location
   */
  public Location right(Integer xoff) {
    if (isPoint()) {
      return new Location(x + xoff, y);
    }
    return new Location(getCenter().x + (int) (w/2) + xoff, y);
  }

  /**
   * creates a point at the given offset above<br>
   * negative means the opposite direction<br>
   * for rectangles the reference point is the middle of upper side
   *
   * @param yoff y offset
   * @return new location
   */
  public Location above(Integer yoff) {
    if (isPoint()) {
      return new Location(x - yoff, y);
    }
    return new Location(getCenter().x - (int) (h/2) - yoff, y);
  }

  /**
   * creates a point at the given offset below<br>
   * negative means the opposite direction<br>
   * for rectangles the reference point is the middle of the lower side
   *
   * @param yoff y offset
   * @return new location
   */
  public Location below(Integer yoff) {
    if (isPoint()) {
      return new Location(x - yoff, y);
    }
    return new Location(getCenter().x - (int) (h/2) - yoff, y);
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
   * Get the color at the given Point (center of visual) for details: see java.awt.Robot and ...Color
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

  //<editor-fold desc="***** capture/show">
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
    show((int) Settings.DefaultHighlightTime);
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
      vLog.error("getBufferedImage: %s error(%s)", this, ex.getMessage());
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

  protected byte[] getImageBytes() {
    return getImageBytes(dotPNG);
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

  public static void fakeHighlight(boolean state) {
    //TODO implement fakeHighlight
  }

  //<editor-fold desc="**** wait">
  public void wait(double time) {
    SX.pause(time);
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
   * Move the mouse to this visual's target
   *
   * @return this
   */
  public Visual hover() {
    Mouse.get().move(this.getTarget());
    return this;
  }

  /**
   * Move the mouse to this visual's target and click left
   *
   * @return this
   */
  public Visual click() {
    Mouse.get().click(this.getTarget(), "L");
    return this;
  }

  /**
   * Move the mouse to this visual's target and double click left
   *
   * @return this
   */
  public Visual doubleClick() {
    Mouse.get().click(this.getTarget(), "LD");
    return this;
  }

  /**
   * Move the mouse to this visual's target and click right
   *
   * @return this
   */
  public Visual rightClick() {
    Mouse.get().click(this.getTarget(), "R");
    return this;
  }
  //</editor-fold>

}
