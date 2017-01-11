/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;

public class Element extends SXElement {

  private static eType eClazz = eType.ELEMENT;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

  protected URL urlImg = null;

  public Mat getContent() {
    return content;
  }

  public Mat getContent(Element elem) {
    return content.submat(new Rect(elem.x, elem.y, elem.w, elem.h));
  }

  public void setContent(Mat content) {
    this.content = content;
  }

  public boolean hasContent() {
    return SX.isNotNull(content) && !content.empty();
  }

  private Mat content = null;

  protected double resizeFactor;
  public double getResizeFactor() {
    return isValid() ? resizeFactor : 1;
  }

  private Element lastMatch = null;
  private java.util.List<Element> lastMatches = new ArrayList<Element>();
  private int matchIndex = -1;

  public Element getLastSeen() {
    if (SX.isNull(lastSeen)) {
      return new Element();
    }
    return lastSeen;
  }

  public void setLastSeen(Element lastSeen) {
    this.lastSeen = lastSeen;
  }

  private Element lastSeen = null;

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

  protected void initAfter() {
    initName(eClazz);
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

  public Element(Core.MinMaxLocResult mMinMax, Target target, Rect rect) {
    setClazz();
    init((int) mMinMax.maxLoc.x + target.getTarget().x +
            rect.x, (int) mMinMax.maxLoc.y + target.getTarget().y + rect.y,
            target.w, target.h);
    setScore(mMinMax.maxVal);
  }

  public boolean isMatch() {
    return score > -1;
  }

  protected String toStringPlus() {
    if (isMatch()) {
      return " %" + score * 100;
    }
    return "";
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

  //<editor-fold desc="***** content">
  protected boolean plainColor = false;
  protected boolean blackColor = false;
  public boolean isPlainColor() {
    return isValid() && plainColor;
  }

  public boolean isBlack() {
    return isValid() && blackColor;
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

  public void show() {
//    show((int) SX.getOptionNumber("DefaultHighlightTime"));
    show(showTime);
  }

  public void show(int time) {
    show(this, time);
  }

  public void showMatch() {
    show(this, getLastMatch(), showTime);
  }

  public void showMatches() {
    showAll(this, this.getLastMatches(), showTime);
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

  private double score = -1;

  public double getWantedScore() {
    return wantedScore;
  }

  public void setWantedScore(double wantedScore) {
    this.wantedScore = wantedScore;
  }

  private double wantedScore = -1;

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

  private double[] lastScores = new double[] {-1, -1, -1};

  public void setLastScores(double[] scores) {
    for (int i = 0; i<scores.length; i++) {
      lastScores[i] = scores[i];
    }
  }

  public double[] getLastScores() {
    return lastScores;
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
  public static Element at() {
    PointerInfo mp = MouseInfo.getPointerInfo();
    if (mp != null) {
      return new Element(mp.getLocation());
    } else {
      log.error("MouseInfo.getPointerInfo(): null");
      return new Element();
    }
  }

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
