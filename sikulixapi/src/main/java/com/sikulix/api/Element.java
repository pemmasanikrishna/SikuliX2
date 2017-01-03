/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.IRobot;
import com.sikulix.core.SX;
import com.sikulix.core.SXElement;
import com.sikulix.core.SXLog;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class Element extends SXElement {

  private static eType eClazz = eType.ELEMENT;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  protected Mat content = null;

  public Mat getContent() {
    return content;
  }

  protected URL urlImg = null;

  private double score = -1;

  private Element lastMatch = null;
  private java.util.List<Element> lastMatches = new ArrayList<Element>();
  private int matchIndex = -1;

  //<editor-fold desc="***** construction, info">
  public Element() {
    setClazz();
    init();
  }

  protected void setClazz() {
    clazz = eClazz;
  }

  protected void copy(Element elem) {
  }

  public Element(int x, int y, int w, int h) {
    setClazz();
    init(x, y, w, h);
  }

  public Element(int x, int y, int wh) {
    setClazz();
    init(x, y, wh, wh);
  }

  public Element(int x, int y) {
    setClazz();
    init(x, y, 0, 0);
  }

  public Element(int[] rect) {
    setClazz();
    init(rect);
  }

  public Element(Rectangle rect) {
    setClazz();
    init(rect);
  }

  public Element(Point p) {
    setClazz();
    init(p);
  }

  public Element(Element elem) {
    this();
    x = elem.x;
    y = elem.y;
    w = elem.w;
    h = elem.h;
    copy(elem);
  }

  public Element(Element elem, double score) {
    this(elem);
    setScore(score);
  }

  public Element(Element elem, double score, Element off) {
    this(elem);
    setScore(score);
    setTarget(off);
  }

  public Element(Element elem, Element off) {
    this(elem);
    setTarget(off);
  }

  public Element(int id) {
    Rectangle rect = SX.getMonitor(id);
    init(rect.x, rect.y, rect.width, rect.height);
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
  //</editor-fold>

  //<editor-fold desc="***** capture, highlight">
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

  public void highlight() {
    highlight((int) SX.getOptionNumber("DefaultHighlightTime"));
  }

  public void highlight(int time) {
    //TODO Element.highlight not implemented
    log.error("highlight not implemented");
  }

  public static void fakeHighlight(boolean state) {
    //TODO implement fakeHighlight
  }
  //</editor-fold>

  //<editor-fold desc="***** lastCapture">
  private Image lastCapture = null;

  public Image getLastCapture() {
    return lastCapture;
  }

  public void setLastCapture(Image lastCapture) {
    this.lastCapture = lastCapture;
  }
  //</editor-fold>

  //<editor-fold desc="***** target">
  public void setTarget(Element elem) {
    target = elem.getCenter();
  }

  public void setTarget(int x, int y) {
    target = getCenter().offset(x, y);
  }

  public void setTarget(int[] pos) {
    target = getCenter().offset(new Element(pos));
  }

  public Element getTarget() {
    if (SX.isNull(target)) {
      target = getCenter();
    }
    return target;
  }
  //</editor-fold>

  //<editor-fold desc="***** score">
  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }
  //</editor-fold>

  //<editor-fold desc="***** lastMatch">
  public Element getLastMatch() {
    return lastMatch;
  }

  public void setLastMatch(Element match) {
    lastMatch = match;
  }

  public java.util.List<Element> getLastMatches() {
    return lastMatches;
  }

  public void setLastMatches(java.util.List<Element> lastMatches) {
    this.lastMatches = lastMatches;
  }

  public int getMatchIndex() {
    return matchIndex;
  }

  public void setMatchIndex(int matchIndex) {
    this.matchIndex = matchIndex;
  }
  //</editor-fold>

  //<editor-fold desc="***** content/show">
  final static String PNG = "png";
  final static String dotPNG = "." + PNG;

  protected static Mat makeMat(BufferedImage bImg) {
    Mat aMat = null;
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
      log.error("makeMat: 3BYTE_BGR (%dx%d)",
              bImg.getWidth(), bImg.getHeight());
    } else {
      log.error("makeMat: Type not supported: %d (%dx%d)",
              bImg.getType(), bImg.getWidth(), bImg.getHeight());
    }
    return aMat;
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
      log.error("getBufferedImage: %s error(%s)", this, ex.getMessage());
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
  //</editor-fold>

  //<editor-fold desc="***** find, wait">
  private double waitAfter = 0;

  public double getWaitAfter() {
    return waitAfter;
  }

  public void setWaitAfter(double waitAfter) {
    this.waitAfter = waitAfter;
  }

  public void wait(double time) {
    SX.pause(time);
  }

  public Element find(Image img) {
    return wait(img, 0);
  }

  public Element wait(Image img) {
    //TODO implement wait(Image)
    return null;
  }

  public Element wait(Image img, double time) {
    //TODO implement wait(Image, time)
    return null;
  }

  public boolean waitVanish(Image img) {
    //TODO implement waitVanish(Image)
    return true;
  }

  public boolean waitVanish(Image img, double time) {
    //TODO implement waitVanish(Image, time)
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

  //<editor-fold desc="***** keyboard">
  //TODO implement keyboard
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

  //<editor-fold desc="TODO  be like Selenium">
  public boolean isDisplayed() {
    return true;
  }

  public void sendKeys(CharSequence keys) {
    write(keys.toString());
  }
  //</editor-fold>
}
