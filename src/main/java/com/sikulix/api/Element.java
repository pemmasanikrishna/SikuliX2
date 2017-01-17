/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import javax.swing.*;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public Element getLastTarget() {
    return lastTarget;
  }

  public void setLastTarget(Element lastTarget) {
    this.lastTarget = lastTarget;
  }

  private Element lastTarget = null;
  private Element lastMatch = null;
  private Element lastVanish = null;
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
    x = elem.x;
    y = elem.y;
    w = elem.w;
    h = elem.h;
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
  public int isOn() {
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

  //<editor-fold desc="***** capture, highlight, show">
  public Picture capture() {
    return capture(this);
  }

  public Picture capture(Element elem) {
    content = new Mat();
    Picture img = new Picture();
    if (isSpecial()) {
      SX.terminate(1, "capture: special not implemented");
    } else {
      Robot robot = SX.getSXROBOT();
      img = new Picture(robot.createScreenCapture(elem.getRectangle()));
    }
    if (img.hasContent()) {
      content = img.getContent();
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
    show(this, time, 0);
  }

  public void showMatch() {
    show(this, getLastMatch(), showTime);
  }

  public void showVanish() {
    if (SX.isNotNull(getLastVanish())) {
      show(this, getLastVanish(), showTime);
    }
  }

  public void showMatches() {
    showAll(this, this.getLastMatches(), showTime);
  }

  public void showContent(int timeAfter) {
    showContent(timeAfter, 0, 0);
  }

  public void showContent(int timeAfter, int timeBefore) {
    showContent(timeAfter, timeBefore, 0);
  }

  public void showContent(int timeAfter, int timeBefore, int timeVanish) {
    show(this, -timeAfter, timeBefore, timeVanish);
  }

  public boolean isShowing() {
    return SX.isNotNull(showing);
  }

  public void setShowing(JFrame showing) {
    this.showing = showing;
  }

  public void stopShowing() {
    if (isShowing()) {
      showing.dispose();
      showing = null;
    }
  }

  public Element whereShowing() {
    if (!isShowing()) {
      return new Element();
    }
    Point location = showing.getLocation();
    return new Element(location.x, location.y, showing.getWidth(), showing.getHeight());
  }

  private JFrame showing = null;


  //</editor-fold>

  //<editor-fold desc="***** lastCapture">
  private Picture lastCapture = null;

  public Picture getLastCapture() {
    return lastCapture;
  }

  public void setLastCapture(Picture lastCapture) {
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
  public void resetMatches() {
    lastMatch = null;
    lastMatches = new ArrayList<Element>();
    matchIndex = -1;
    lastScores = new double[] {0, 0, 0};
  }

  public boolean hasMatch() {
    return SX.isNotNull(lastMatch);
  }

  public boolean hasVanish() {
    return SX.isNotNull(lastVanish);
  }

  public boolean hasMatches() {
    return lastMatches.size() > 0;
  }

  public Element getLastMatch() {
    return lastMatch;
  }

  public Element getLastVanish() {
    return lastVanish;
  }

  public java.util.List<Element> getLastMatches() {
    return lastMatches;
  }

  public void setLastMatch(Element match) {
    lastMatch = match;
  }

  public void setLastVanish(Element match) {
    if (SX.isNotNull(match)) {
      lastMatch = null;
      lastVanish = match;
    }
  }

  private double[] lastScores = new double[] {0, 0, 0};

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
    Device.move(this.getTarget());
    return this;
  }

  /**
   * Move the mouse to this element's target and click left
   *
   * @return this
   */
  public SXElement click() {
    Device.click(this.getTarget(), "L");
    return this;
  }

  /**
   * Move the mouse to this element's target and double click left
   *
   * @return this
   */
  public SXElement doubleClick() {
    Device.click(this.getTarget(), "LD");
    return this;
  }

  /**
   * Move the mouse to this element's target and click right
   *
   * @return this
   */
  public SXElement rightClick() {
    Device.click(this.getTarget(), "R");
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

  public double getAutoWaitTimeout() {
    return autoWaitTimeout;
  }

  public void setAutoWaitTimeout(double autoWaitTimeout) {
    this.autoWaitTimeout = autoWaitTimeout;
  }

  double autoWaitTimeout = getWaitForMatch();

  public Event getFindFailedResponse() {
    return eventFindFailed;
  }

  public Event setFindFailedResponse(Event.REACTION response) {
    eventFindFailed = new Event(Event.TYPE.FINDFAILED, response);
    return eventFindFailed;
  }

  public Event setFindFailedHandler(Handler handler) {
    eventFindFailed = new Event(Event.TYPE.FINDFAILED, handler);
    return eventFindFailed;
  }

  public Event getImageMissingResponse() {
    return eventImageMissing;
  }

  public Event setImageMissingResponse(Event.REACTION response) {
    eventImageMissing = new Event(Event.TYPE.IMAGEMISSING, response);
    return eventImageMissing;
  }

  public Event setImageMissingHandler(Handler handler) {
    eventImageMissing = new Event(Event.TYPE.IMAGEMISSING, handler);
    return eventImageMissing;
  }

  private String setOnHandler(Event.TYPE type, Element elem, Handler handler) {
    Event event = new Event(type, elem, handler);
    String name = elem.getName();
    onEvents.put(name, event);
    return name;
  }

  public String onAppear(Element elem, Handler handler) {
    return setOnHandler(Event.TYPE.ONAPPEAR, elem, handler);
  }

  public String onVanish(Element elem, Handler handler) {
    return setOnHandler(Event.TYPE.ONAPPEAR, elem, handler);
  }

  public String onChange(Element elem, Handler handler) {
    return setOnHandler(Event.TYPE.ONAPPEAR, elem, handler);
  }

  private Event eventFindFailed = new Event(Event.TYPE.FINDFAILED, Event.REACTION.ABORT);
  private Event eventImageMissing = new Event(Event.TYPE.IMAGEMISSING, Event.REACTION.ABORT);
  private Map<String, Event> onEvents = new HashMap<>();

  public Element find(Object... args) {
    return Do.find(target, this);
  }

  public Element wait(Object... args) {
    return Do.wait(target, this);
  }

  public boolean waitVanish(Object... args) {
    return Do.waitVanish(target, this);
  }

  public boolean exists(Object... args) {
    return Do.exists(target, this);
  }

  public List<Element> findAll(Object... args) {
    return Do.findAll(target, this);
  }
}
