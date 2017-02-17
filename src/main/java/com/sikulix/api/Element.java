/*
 * Copyright (c) 2016 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class Element extends SXElement {

  private static eType eClazz = eType.ELEMENT;
  private static SXLog log = SX.getLogger("SX." + eClazz.toString());

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
    elementDevice = elem.elementDevice;
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

  public Element(Dimension dim) {
    setClazz();
    init(0, 0, (int) dim.getWidth(), (int) dim.getHeight());
  }

  public Element(Element elem) {
    this();
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
    if (id < 0) {
      // hack: special for even margin all sides and for onChange()
      init(-id, -id, -id, -id);
    } else {
      Rectangle rect = getElementDevice().getMonitor(id);
      init(rect.x, rect.y, rect.width, rect.height);
    }
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
    for (int i = 0; i < getElementDevice().getNumberOfMonitors(); i++) {
      r = getElementDevice().getMonitor(i);
      if (r.contains(this.x, this.y)) {
        return i;
      }
    }
    return -1;
  }
  //</editor-fold>

  //<editor-fold desc="***** content">
  protected URL urlImg = null;

  public BufferedImage get() {
    return getBufferedImage(getContent());
  }

  public Mat getContent() {
    return content;
  }

  public Mat getContent(Element elem) {
    return content.submat(new Rect(elem.x, elem.y, elem.w, elem.h));
  }

  public void setContent(Mat content) {
    this.content = content;
  }

  public Element setContent() {
    content = getNewMat();
    return this;
  }

  public boolean hasContent() {
    return SX.isNotNull(content) && !content.empty();
  }

  private Mat content = null;

  public Element load() {
    capture();
    return this;
  }

  public boolean save(String name) {
    return save(name, Picture.getBundlePath());
  }

  public boolean save(String name, String path) {
    URL url = Content.makeURL(new File(path, name).getAbsolutePath());
    if (SX.isNull(url)) {
      return false;
    }
    try {
      url = Content.makeURL(new File(path, name).getCanonicalPath());
      return save(url, name);
    } catch (IOException e) {
    }
    log.error("save: invalid: %s / %s", path, name);
    return false;
  }

  public boolean save(String name, URL urlPath) {
    URL url = Content.makeURL(urlPath, name);
    if (SX.isNotNull(url)) {
      return save(url, name);
    } else {
      log.error("save: invalid: %s / %s", urlPath, name);
    }
    return false;
  }

  public boolean save(URL url, String name) {
    if (!hasContent()) {
      load();
    }
    urlImg = null;
    if (SX.isNotNull(url) && hasContent()) {
      if ("file".equals(url.getProtocol())) {
        log.trace("save: %s", url);
        String imgFileName = SX.getValidImageFilename(url.getPath());
        Mat imgContent = getContent();
        if (Imgcodecs.imwrite(imgFileName, imgContent)) {
          urlImg = url;
          setName(name);
          return true;
        }
      } else {
        //TODO save: http and jar
        log.error("save: not implemented: %s", url);
      }
    }
    return false;
  }

  protected boolean plainColor = false;
  protected boolean blackColor = false;
  protected boolean whiteColor = false;

  public boolean isPlainColor() {
    return isValid() && plainColor;
  }

  public boolean isBlack() {
    return isValid() && blackColor;
  }

  public boolean isWhite() {
    return isValid() && blackColor;
  }

  public double getResizeFactor() {
    return isValid() ? resizeFactor : 1;
  }

  protected double resizeFactor;

  //</editor-fold>

  //<editor-fold desc="***** capture">
  public Picture getAsPicture() {
    if (!hasContent()) {
      return new Picture();
    }
    return new Picture(this);
  }

  public Picture capture() {
    return getElementDevice().capture(this);
  }
  //</editor-fold>

  //<editor-fold desc="***** show, highlight">
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

  private Color lineColor = Story.getLineColor();

  public Color getLineColor() {
    return lineColor;
  }

  public void setLineColor(Color lineColor) {
    this.lineColor = lineColor;
  }

  private int highLightLine = (int) SX.getOptionNumber("highLightLine", 3);

  public int getHighLightLine() {
    return highLightLine;
  }

  public void setHighLightLine(int highLightLine) {
    this.highLightLine = highLightLine;
  }

  private int showTime = (int) SX.getOptionNumber("SXShow.showTime", 3);

  public int getShowTime() {
    return showTime;
  }

  public void setShowTime(int showTime) {
    this.showTime = showTime;
  }

  public Story showStart(int... times) {
    showing = new Story(this, times);
    showing.setWaitForFrame();
    showing.start();
    return showing;
  }

  public void showStop() {
    if (isShowing()) {
      showing.stop();
    }
    showing = null;
  }

  public void show() {
    show(showTime);
  }

  public void show(int time, int... times) {
    if (!hasContent()) {
      load();
    }
    showing = new Story(this, times);
    showing.setBorder();
    showing.show(time);
    showing = null;
  }

  public void show(Element elem) {
    show(elem, showTime);
  }

  public void show(Element elem, int time, int... times) {
    showing = new Story(this, times);
    showing.add(elem);
    showing.show(time);
    showing = null;
  }

  public void showMatch(int... times) {
    if (hasMatch()) {
      showing = new Story(this);
      showing.add(getLastMatch()).show(times.length > 0 ? times[0] : showTime);
      showing = null;
    }
  }

  public void showVanish(int... times) {
    if (SX.isNotNull(getLastVanish())) {
      showing = new Story(this);
      showing.add(getLastVanish()).show(times.length > 0 ? times[0] : showTime);
      showing = null;
    }
  }

  public void showMatches(int... times) {
    if (hasMatches()) {
      showing = new Story(this);
      for (Element match : getLastMatches()) {
        showing.add(match);
      }
      showing.show(times.length > 0 ? times[0] : showTime);
      showing = null;
    }
  }

  public boolean isShowing() {
    return SX.isNotNull(showing);
  }

  private Story showing = null;
  //</editor-fold>

  //<editor-fold desc="***** target">
  public Element getLastTarget() {
    return lastTarget;
  }

  public void setLastTarget(Element lastTarget) {
    this.lastTarget = lastTarget;
  }

  private Element lastTarget = null;

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

  private Element lastMatch = null;
  private Element lastVanish = null;
  private java.util.List<Element> lastMatches = new ArrayList<Element>();
  private int matchIndex = -1;

  public void resetMatches() {
    lastMatch = null;
    lastMatches = new ArrayList<Element>();
    matchIndex = -1;
    lastScores = new double[]{0, 0, 0};
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

  private double[] lastScores = new double[]{0, 0, 0};

  public void setLastScores(double[] scores) {
    for (int i = 0; i < scores.length; i++) {
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
  private Element findForClick(String type, Object... args) {
    Element target;
    if (args.length == 0) {
      target = this;
    } else if (args.length == 1) {
      target = Finder.runFind(type, args[0], this);
    } else {
      target = Finder.runWait(type, args[0], this, args[1]);
    }
    return target;
  }

  /**
   * Move the mouse to this element's target
   *
   * @return this
   */
  public Element hover(Object... args) {
    Element target = findForClick(Finder.HOVER, args);
    Element moveTarget = target.getElementDevice().move(target);
    return moveTarget;
  }

  /**
   * Move the mouse to this element's target and click left
   *
   * @return this
   */
  public Element click(Object... args) {
    Element target = findForClick(Finder.CLICK, args);
    return target.getElementDevice().click(target);
  }

  /**
   * Move the mouse to this element's target and double click left
   *
   * @return this
   */
  public Element doubleClick(Object... args) {
    Element target = findForClick(Finder.DOUBLECLICK, args);
    target.getElementDevice().doubleClick(target);
    return target;
  }

  /**
   * Move the mouse to this element's target and click right
   *
   * @return this
   */
  public Element rightClick(Object... args) {
    Element target = findForClick(Finder.RIGHTCLICK, args);
    target.getElementDevice().rightClick(target);
    return target;
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

  //<editor-fold desc="***** handle FindFailed, ImageMissing">
  public double getAutoWaitTimeout() {
    return autoWaitTimeout;
  }

  public void setAutoWaitTimeout(double autoWaitTimeout) {
    this.autoWaitTimeout = autoWaitTimeout;
  }

  double autoWaitTimeout = getWaitForMatch();

  private Event.RESPONSE findFailedResponse = Event.RESPONSE.ABORT;

  public Event.RESPONSE getFindFailedResponse() {
    return findFailedResponse;
  }

  public void setFindFailedResponse(Event.RESPONSE response) {
    findFailedResponse = response;
  }

  private Handler findFaileHandler = null;

  public void setFindFaileHandler(Handler handler) {
    findFaileHandler = handler;
  }

  public void unsetFindFaileHandler() {
    findFaileHandler = null;
  }

  private Event.RESPONSE imageMissingResponse = Event.RESPONSE.ABORT;

  public Event.RESPONSE getImageMissingResponse() {
    return imageMissingResponse;
  }

  public void setImageMissingResponse(Event.RESPONSE response) {
    imageMissingResponse = response;
  }

  private Handler imageMissingHandler = null;

  public void setImageMissingHandler(Handler handler) {
    imageMissingHandler = handler;
  }

  public void unsetImageMissingHandler() {
    imageMissingHandler = null;
  }
  //</editor-fold>

  //<editor-fold desc="***** observe">
  private Map<Element, Event> events = new HashMap<>();

  public long getObserveCount() {
    return observeCount;
  }

  private long observeCount = 0;

  private synchronized boolean setObserving(Integer state) {
    if (SX.isNotNull(state)) {
      if (state > 0) {
        observeCount++;
      } else if (state < 0) {
        observeCount--;
      } else {
        observeCount = 0;
      }
    }
    return observeCount > 0;
  }

  public boolean incrementObserveCount() {
    return setObserving(1);
  }

  public boolean decrementObserveCount() {
    return setObserving(-1);
  }

  public boolean isObserving() {
    return setObserving(null);
  }

  public void observe() {
    observeCount = 0;
    Events.add(this, events.values());
    Events.startObserving();
  }

  public void observeStop() {
    setObserving(0);
  }

  public void observeReset() {
    observeStop();
    events.clear();
    Events.remove(this);
  }

  private Event putEvent(Event.TYPE type, Object what, Handler handler) {
    if (what instanceof String) {
      Picture pWhat = new Picture((String) what);
      if (!pWhat.isValid()) {
        log.trace("handle image missing: %s", pWhat);
        if (!Picture.handleImageMissing(pWhat)) {
          log.error("Event: %s invalid what: %s", type, what);
          return null;
        }
      }
      what = pWhat;
    } else if (!(what instanceof Element)) {
      log.error("Event: invalid what: %s", what);
      return null;
    }
    Event evt = new Event(type, (Element) what, this, handler);
    if (events.containsKey(what)) {
      evt.setKey(events.get(what).getKey());
    } else {
      evt.setKey(events.size() + 1);
    }
    events.put((Element) what, evt);
    return evt;
  }

  public Event onAppear(Object what, Handler handler) {
    return putEvent(Event.TYPE.ONAPPEAR, what, handler);
  }

  public Event onAppear(Object what) {
    return putEvent(Event.TYPE.ONAPPEAR, what, null);
  }

  public Event onVanish(Object what, Handler handler) {
    return putEvent(Event.TYPE.ONVANISH, what, handler);
  }

  public Event onVanish(Object what) {
    return putEvent(Event.TYPE.ONVANISH, what, null);
  }

  private int minimumSizeDefault = 50;

  public Event onChange() {
    return putEvent(Event.TYPE.ONCHANGE, new Element(-minimumSizeDefault), null);
  }

  public Event onChange(Handler handler) {
    return putEvent(Event.TYPE.ONCHANGE, new Element(-minimumSizeDefault), handler);
  }

  public Event onChange(int minimumSize) {
    return putEvent(Event.TYPE.ONCHANGE, new Element(-minimumSize), null);
  }

  public Event onChange(int minimumSize, Handler handler) {
    return putEvent(Event.TYPE.ONCHANGE, new Element(-minimumSize), handler);
  }

  public void removeEvent(Event evt) {
    events.remove(evt);
  }

  public void removeEvents() {
    events.clear();
  }

  public boolean hasEvents() {
    return Events.hasHappened(this);
  }

  public Event nextEvent() {
    return Events.nextHappened(this);
  }
  //</editor-fold>

  //<editor-fold desc="***** find, ...">
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
  //</editor-fold>

  //<editor-fold desc="is clicked">
  Element click = null;
  long clickedTime = 0;

  public boolean isClicked() {
    return SX.isNotNull(click);
  }

  public Element getClick() {
    return click;
  }

  public Element setClick(Element click) {
    this.click = click;
    click.setClicked(new Date().getTime());
    return this;
  }

  public void resetClick() {
    click = null;
  }

  public void setClicked(long clickedTime) {
    this.clickedTime = clickedTime;
  }

  public long getClicked() {
    return clickedTime;
  }

  public enum Component {
    RECTANGLE, CIRCLE, LINE, IMAGE, TEXT, BUTTON;
  }

  public Symbol setComponent(Component component) {
    this.component = component;
    return (Symbol) this;
  }

  public Component getComponent() {
    return component;
  }

  private Component component = Component.RECTANGLE;
  //</editor-fold>
}
